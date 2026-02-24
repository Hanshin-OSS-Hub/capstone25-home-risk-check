"""
전세사기 위험도 분석 API (비동기 + Redis 캐싱 버전)

변경사항 (v2):
- /predict → 비동기 처리 (BackgroundTasks)
  - 요청 접수 → 즉시 task_id 반환 (202 Accepted)
  - 백그라운드에서 OCR + 예측 수행
- /predict/{task_id} → 작업 상태/결과 조회 (폴링)
- Redis 캐싱: 동일 주소+보증금+파일 조합 결과 재사용
- /predict/cache → 캐시 무효화 API
"""
from fastapi import FastAPI, UploadFile, File, Form, HTTPException, BackgroundTasks
from fastapi.responses import JSONResponse
from typing import List, Optional
import shutil
import os
import uuid
import asyncio
import logging
from datetime import datetime
from functools import partial
from fastapi.middleware.cors import CORSMiddleware

from app.services.ocr.ledger_parser import extract_building_ledger
from app.services.ocr.registry_parser import extract_real_estate_data
from app.services.predict_service import predict_risk_with_ocr
from app.router import stats
from app.core import (
    get_settings,
    check_db_connection,
    is_db_available,
    reset_db_availability
)
from app.core.exceptions import DatabaseConnectionError, ServiceUnavailableError
from app.services.document_validator import validate_document_match

# Redis 모듈
from app.core.redis_config import (
    get_redis,
    close_redis,
    generate_cache_key,
    generate_file_hash,
    get_cached_result,
    set_cached_result,
    invalidate_cache,
    set_task_status,
    get_task_status,
    health_check_redis,
    TaskStatus,
)

logger = logging.getLogger(__name__)

app = FastAPI(
    title="Fraud Detector AI",
    version="2.0",
    description="전세사기 위험도 분석 API (비동기 + Redis 캐싱)"
)

settings = get_settings()
origins = ["*"] if settings.APP_ENV == "local" else [
    "https://your-frontend-domain.com",
    "https://app.your-domain.com"
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "DELETE"],
    allow_headers=["*"],
)

# 파일 업로드 제약사항
MAX_FILE_SIZE_MB = 20
ALLOWED_IMAGE_TYPES = {"image/png", "image/jpeg", "image/jpg"}
ALLOWED_PDF_TYPES = {"application/pdf"}


# =============================================================================
# 앱 Lifecycle (Redis 연결/해제)
# =============================================================================
@app.on_event("startup")
async def startup_event():
    """앱 시작 시 Redis 연결"""
    redis_client = await get_redis()
    if redis_client:
        logger.info("🚀 Redis 연결 완료")
    else:
        logger.warning("⚠️ Redis 없이 시작 (캐싱 비활성)")


@app.on_event("shutdown")
async def shutdown_event():
    """앱 종료 시 Redis 연결 해제"""
    await close_redis()
    logger.info("Redis 연결 종료")


# =============================================================================
# 유틸리티 함수
# =============================================================================
def validate_file_size(file: UploadFile, max_size_mb: int = MAX_FILE_SIZE_MB):
    """파일 크기 검증"""
    file.file.seek(0, 2)
    file_size = file.file.tell()
    file.file.seek(0)

    if file_size > max_size_mb * 1024 * 1024:
        raise HTTPException(
            status_code=413,
            detail=f"파일 크기는 {max_size_mb}MB 이하여야 합니다 (현재: {file_size / 1024 / 1024:.1f}MB)"
        )


def validate_file_type(file: UploadFile, allowed_types: set):
    """파일 타입 검증"""
    if file.content_type not in allowed_types:
        raise HTTPException(
            status_code=415,
            detail=f"지원하지 않는 파일 형식입니다. 허용 형식: {', '.join(allowed_types)}"
        )


def create_error_response(
        status_code: int,
        message: str,
        errors: list,
        suggestions: list = None
) -> JSONResponse:
    """에러 응답 생성 헬퍼 함수"""
    content = {
        "meta": {
            "code": status_code,
            "message": message,
            "timestamp": datetime.now().isoformat()
        },
        "errors": errors
    }
    if suggestions:
        content["suggestions"] = suggestions

    return JSONResponse(status_code=status_code, content=content)


# =============================================================================
# 헬스체크
# =============================================================================
@app.get("/", summary="서비스 상태 확인")
async def health_check():
    redis_status = await health_check_redis()
    return {
        "status": "Healthy",
        "service": "Fraud Detector AI",
        "version": "2.0",
        "redis": redis_status,
    }


# 라우터 등록
app.include_router(stats.router)


# =============================================================================
# 메인 예측 API (비동기 전환)
# =============================================================================
@app.post("/predict",
          status_code=202,
          summary="전세사기 위험도 분석 요청 (비동기)",
          description="""
          건축물대장(이미지)과 등기부등본(PDF)을 업로드하여 위험도 분석을 요청합니다.

          **동작 방식:**
          1. 요청 접수 → 즉시 `task_id` 반환 (202 Accepted)
          2. 동일 요청의 캐시가 있으면 즉시 결과 반환 (200 OK)
          3. `/predict/{task_id}` 로 진행 상태/결과 조회 (폴링)

          **파일 제약사항:**
          - 건축물대장: PNG, JPG, JPEG (최대 10MB, 최대 5개)
          - 등기부등본: PDF (최대 20MB, 최대 3개)
          """)
async def predict_risk_endpoint(
        background_tasks: BackgroundTasks,
        deposit: int = Form(..., description="보증금 (만원)", ge=0, le=1000000),
        address: str = Form(..., description="주소 (시세 조회용)", min_length=5, max_length=200),
        ledger_files: List[UploadFile] = File(default=None, description="건축물대장 이미지 (PNG/JPG)"),
        registry_files: List[UploadFile] = File(default=None, description="등기부등본 파일 (PDF)"),
        skip_cache: bool = Form(default=False, description="캐시 무시 여부 (True면 캐시 건너뜀)"),
):
    # === 1. 입력 검증 ===
    errors = []

    if not ledger_files or len(ledger_files) == 0:
        errors.append({
            "field": "ledger_files",
            "message": "건축물대장 파일은 필수입니다. 최소 1개 이상의 이미지를 업로드해주세요."
        })

    if not registry_files or len(registry_files) == 0:
        errors.append({
            "field": "registry_files",
            "message": "등기부등본 파일은 필수입니다. 최소 1개 이상의 PDF를 업로드해주세요."
        })

    if ledger_files and len(ledger_files) > 5:
        errors.append({
            "field": "ledger_files",
            "message": "건축물대장은 최대 5개까지 업로드 가능합니다"
        })

    if registry_files and len(registry_files) > 3:
        errors.append({
            "field": "registry_files",
            "message": "등기부등본은 최대 3개까지 업로드 가능합니다"
        })

    # 파일 타입/크기 검증
    if ledger_files:
        for idx, file in enumerate(ledger_files):
            try:
                validate_file_type(file, ALLOWED_IMAGE_TYPES)
                validate_file_size(file, max_size_mb=10)
            except HTTPException as e:
                errors.append({
                    "field": f"ledger_files[{idx}]",
                    "message": f"{file.filename}: {e.detail}"
                })

    if registry_files:
        for idx, file in enumerate(registry_files):
            try:
                validate_file_type(file, ALLOWED_PDF_TYPES)
                validate_file_size(file, max_size_mb=20)
            except HTTPException as e:
                errors.append({
                    "field": f"registry_files[{idx}]",
                    "message": f"{file.filename}: {e.detail}"
                })

    if errors:
        return create_error_response(400, "입력 데이터 검증 실패", errors)

    # === 2. 파일 해시 생성 → 캐시 키 ===
    file_hashes = []
    file_contents = {}  # {filename: bytes} - 나중에 임시파일 저장용

    for file in (ledger_files or []):
        content = await file.read()
        file_hashes.append(generate_file_hash(content))
        file_contents[f"ledger_{file.filename}"] = content
        await asyncio.to_thread(file.file.seek, 0)

    for file in (registry_files or []):
        content = await file.read()
        file_hashes.append(generate_file_hash(content))
        file_contents[f"registry_{file.filename}"] = content
        await asyncio.to_thread(file.file.seek, 0)

    cache_key = generate_cache_key(address, deposit, file_hashes)

    # === 3. 캐시 확인 ===
    if not skip_cache:
        cached_result = await get_cached_result(cache_key)
        if cached_result:
            logger.info(f"🎯 캐시 히트 - 즉시 응답: {cache_key}")
            return JSONResponse(status_code=200, content=cached_result)

    # === 4. 비동기 작업 생성 ===
    task_id = str(uuid.uuid4())

    # 작업 상태 초기화
    await set_task_status(task_id, TaskStatus.PENDING, progress=0)

    # 임시 파일 저장
    temp_dir = f"temp_uploads/{task_id}"
    os.makedirs(temp_dir, exist_ok=True)

    ledger_paths = []
    registry_paths = []

    for file in (ledger_files or []):
        file_path = os.path.join(temp_dir, f"ledger_{datetime.now().timestamp()}_{file.filename}")
        content_key = f"ledger_{file.filename}"
        with open(file_path, "wb") as buffer:
            buffer.write(file_contents[content_key])
        ledger_paths.append(file_path)

    for file in (registry_files or []):
        file_path = os.path.join(temp_dir, f"registry_{datetime.now().timestamp()}_{file.filename}")
        content_key = f"registry_{file.filename}"
        with open(file_path, "wb") as buffer:
            buffer.write(file_contents[content_key])
        registry_paths.append(file_path)

    # === 5. 백그라운드 작업 등록 ===
    background_tasks.add_task(
        _run_prediction_task,
        task_id=task_id,
        address=address,
        deposit=deposit,
        ledger_paths=ledger_paths,
        registry_paths=registry_paths,
        cache_key=cache_key,
        temp_dir=temp_dir,
    )

    # === 6. 즉시 응답 (202 Accepted) ===
    return JSONResponse(
        status_code=202,
        content={
            "meta": {
                "code": 202,
                "message": "분석 요청이 접수되었습니다",
                "timestamp": datetime.now().isoformat()
            },
            "data": {
                "task_id": task_id,
                "status": TaskStatus.PENDING,
                "cache_key": cache_key,
                "poll_url": f"/predict/{task_id}",
                "estimated_seconds": 15,
            }
        }
    )


# =============================================================================
# 작업 상태/결과 조회 (폴링)
# =============================================================================
@app.get("/predict/{task_id}",
         summary="분석 작업 상태/결과 조회",
         description="""
         `/predict`에서 반환된 `task_id`로 진행 상태를 조회합니다.

         **상태값:**
         - `PENDING` → 대기 중
         - `PROCESSING` → 분석 진행 중 (progress 0~100)
         - `COMPLETED` → 완료 (result에 분석 결과 포함)
         - `FAILED` → 실패 (error에 사유 포함)
         """)
async def get_prediction_status(task_id: str):
    task_data = await get_task_status(task_id)

    if not task_data:
        return create_error_response(
            404,
            "작업을 찾을 수 없습니다",
            [{"field": "task_id", "message": f"'{task_id}' 작업이 존재하지 않거나 만료되었습니다"}],
            suggestions=["올바른 task_id인지 확인해주세요", "작업 결과는 24시간 후 자동 삭제됩니다"]
        )

    status = task_data.get("status")

    if status == TaskStatus.COMPLETED:
        return JSONResponse(
            status_code=200,
            content={
                "meta": {
                    "code": 200,
                    "message": "분석 완료",
                    "timestamp": datetime.now().isoformat()
                },
                "data": {
                    "task_id": task_id,
                    "status": TaskStatus.COMPLETED,
                    "progress": 100,
                    "result": task_data.get("result"),
                }
            }
        )

    elif status == TaskStatus.FAILED:
        return JSONResponse(
            status_code=200,
            content={
                "meta": {
                    "code": 200,
                    "message": "분석 실패",
                    "timestamp": datetime.now().isoformat()
                },
                "data": {
                    "task_id": task_id,
                    "status": TaskStatus.FAILED,
                    "progress": task_data.get("progress", 0),
                    "error": task_data.get("error", "알 수 없는 오류"),
                }
            }
        )

    else:
        # PENDING 또는 PROCESSING
        return JSONResponse(
            status_code=200,
            content={
                "meta": {
                    "code": 200,
                    "message": "분석 진행 중",
                    "timestamp": datetime.now().isoformat()
                },
                "data": {
                    "task_id": task_id,
                    "status": status,
                    "progress": task_data.get("progress", 0),
                }
            }
        )


# =============================================================================
# 캐시 관리 API
# =============================================================================
@app.delete("/predict/cache/{cache_key}",
            summary="특정 캐시 무효화",
            description="캐시 키를 지정하여 해당 분석 결과 캐시를 삭제합니다.")
async def delete_cache(cache_key: str):
    success = await invalidate_cache(cache_key)
    if success:
        return {"meta": {"code": 200, "message": "캐시가 삭제되었습니다"}}
    return create_error_response(
        404,
        "캐시를 찾을 수 없습니다",
        [{"field": "cache_key", "message": "해당 캐시 키가 존재하지 않습니다"}]
    )


# =============================================================================
# 백그라운드 작업: 실제 예측 수행
# =============================================================================
async def _run_prediction_task(
        task_id: str,
        address: str,
        deposit: int,
        ledger_paths: list,
        registry_paths: list,
        cache_key: str,
        temp_dir: str,
):
    """
    BackgroundTasks에서 실행되는 비동기 예측 파이프라인

    진행 단계:
    1. DB 연결 확인       (10%)
    2. 건축물대장 OCR     (30%)
    3. 등기부등본 OCR     (50%)
    4. 문서 매칭 검증     (60%)
    5. 위험도 예측        (90%)
    6. 결과 저장 + 캐싱   (100%)
    """
    try:
        # --- Step 1: DB 연결 확인 (10%) ---
        await set_task_status(task_id, TaskStatus.PROCESSING, progress=10)

        if not is_db_available():
            reset_db_availability()
            if not check_db_connection():
                await set_task_status(
                    task_id, TaskStatus.FAILED,
                    error="데이터베이스 연결을 확인할 수 없습니다. 잠시 후 다시 시도해주세요."
                )
                return

        ocr_results = {'ledger': {}, 'registry': {}}

        # --- Step 2: 건축물대장 OCR (30%) ---
        await set_task_status(task_id, TaskStatus.PROCESSING, progress=20)
        logger.info(f"[Task {task_id[:8]}] 건축물대장 OCR 시작...")

        if ledger_paths:
            try:
                # OCR은 CPU 바운드 → 스레드풀에서 실행
                ocr_results['ledger'] = await asyncio.to_thread(
                    extract_building_ledger, ledger_paths
                )
            except Exception as e:
                await set_task_status(
                    task_id, TaskStatus.FAILED, progress=25,
                    error=f"건축물대장 OCR 처리 실패: {str(e)}. 선명한 이미지를 업로드해주세요."
                )
                return

        await set_task_status(task_id, TaskStatus.PROCESSING, progress=40)

        # --- Step 3: 등기부등본 OCR (50%) ---
        logger.info(f"[Task {task_id[:8]}] 등기부등본 OCR 시작...")

        if registry_paths:
            try:
                ocr_results['registry'] = await asyncio.to_thread(
                    extract_real_estate_data, registry_paths
                )
            except Exception as e:
                await set_task_status(
                    task_id, TaskStatus.FAILED, progress=45,
                    error=f"등기부등본 OCR 처리 실패: {str(e)}. 선명한 PDF를 업로드해주세요."
                )
                return

        await set_task_status(task_id, TaskStatus.PROCESSING, progress=60)

        # --- Step 4: 문서 매칭 검증 (60%) ---
        logger.info(f"[Task {task_id[:8]}] 문서 매칭 검증...")

        ledger_data = ocr_results.get('ledger', {})
        registry_data = ocr_results.get('registry', {})

        if ledger_data and registry_data:
            is_valid, message, details = await asyncio.to_thread(
                validate_document_match, ledger_data, registry_data
            )

            if not is_valid:
                error_result = {
                    "meta": {
                        "code": 422,
                        "message": "문서 불일치 오류",
                        "timestamp": datetime.now().isoformat()
                    },
                    "errors": [{
                        "field": "documents",
                        "message": message,
                        "details": {
                            "confidence": f"{details['confidence']:.1%}",
                            "issues": details['errors'],
                            "match_scores": {
                                k: f"{v:.1%}"
                                for k, v in details['match_scores'].items()
                            }
                        }
                    }],
                    "suggestions": [
                        "건축물대장과 등기부등본이 같은 주소의 문서인지 확인해주세요",
                        "호수(동/호)가 정확히 일치하는지 확인해주세요"
                    ]
                }
                await set_task_status(
                    task_id, TaskStatus.FAILED, progress=60,
                    error=message, result=error_result
                )
                return

            if details.get('warnings'):
                logger.warning(f"[문서검증 경고] {details['warnings']}")

        await set_task_status(task_id, TaskStatus.PROCESSING, progress=75)

        # --- Step 5: 위험도 예측 (90%) ---
        logger.info(f"[Task {task_id[:8]}] 위험도 예측 시작...")

        result = await asyncio.to_thread(
            predict_risk_with_ocr, address, deposit, ocr_results
        )

        await set_task_status(task_id, TaskStatus.PROCESSING, progress=95)

        # --- Step 6: 결과 캐싱 + 완료 (100%) ---
        logger.info(f"[Task {task_id[:8]}] 결과 캐싱...")

        # 성공 결과만 캐싱 (에러 응답은 캐싱하지 않음)
        if "meta" in result and result["meta"].get("code") == 200:
            await set_cached_result(cache_key, result)

        await set_task_status(
            task_id, TaskStatus.COMPLETED,
            progress=100, result=result
        )
        logger.info(f"✅ [Task {task_id[:8]}] 분석 완료")

    except DatabaseConnectionError as e:
        logger.error(f"[Task {task_id[:8]}] DB 연결 실패: {e}")
        await set_task_status(
            task_id, TaskStatus.FAILED,
            error=f"분석 실패: {str(e)}"
        )

    except ServiceUnavailableError as e:
        logger.error(f"[Task {task_id[:8]}] 서비스 불가: {e}")
        await set_task_status(
            task_id, TaskStatus.FAILED,
            error=f"서비스를 일시적으로 사용할 수 없습니다: {str(e)}"
        )

    except Exception as e:
        logger.exception(f"[Task {task_id[:8]}] 예측 중 오류: {e}")
        error_msg = str(e)

        if "Can't connect" in error_msg or "Connection refused" in error_msg:
            error_msg = "Database connection failed"

        await set_task_status(
            task_id, TaskStatus.FAILED,
            error=f"분석 실패: {error_msg}"
        )

    finally:
        # 임시 파일 정리
        _cleanup_temp_files(ledger_paths + registry_paths, temp_dir)


def _cleanup_temp_files(file_paths: list, temp_dir: str):
    """임시 파일 및 디렉토리 정리"""
    for path in file_paths:
        if os.path.exists(path):
            try:
                os.remove(path)
            except Exception as e:
                logger.warning(f"임시 파일 삭제 실패: {path} - {e}")

    if temp_dir and os.path.exists(temp_dir):
        try:
            shutil.rmtree(temp_dir, ignore_errors=True)
        except Exception as e:
            logger.warning(f"임시 디렉토리 삭제 실패: {temp_dir} - {e}")


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)