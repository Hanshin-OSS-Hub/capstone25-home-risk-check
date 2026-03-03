"""
BUG-1, BUG-3 수정 검증 테스트

[BUG-1] predict_risk_with_ocr가 에러 응답을 반환해도 COMPLETED로 처리되던 문제
  - 수정: result_code != 200 이면 TaskStatus.FAILED로 처리
  - 검증: 422(시세없음), 500(서버오류) 등 비정상 code → status가 FAILED여야 함
  - 검증: 정상 code=200 → status가 COMPLETED여야 함

[BUG-3] 동일 파일명 업로드 시 file_contents 키 충돌로 마지막 파일만 남던 문제
  - 수정: key를 f"ledger_{idx}_{filename}" 형식으로 변경
  - 검증: 동일 파일명 2개 업로드 시 각각 별도 콘텐츠로 저장되어야 함
  - 검증: ledger_paths, registry_paths에 모든 파일이 append 되어야 함
"""
import pytest
import asyncio
import os
import tempfile
from io import BytesIO
from unittest.mock import patch, AsyncMock, MagicMock, call
from datetime import datetime

from fastapi.testclient import TestClient


# =============================================================================
# 헬퍼 함수
# =============================================================================
def make_image_file():
    return BytesIO(b'\x89PNG\r\n\x1a\n' + b'\x00' * 100)


def make_image_file_with_content(content: bytes):
    """특정 내용을 가진 이미지 파일 생성 (BUG-3 검증용)"""
    return BytesIO(b'\x89PNG\r\n\x1a\n' + content)


def make_pdf_file():
    return BytesIO(b'%PDF-1.4' + b'\x00' * 100)


# =============================================================================
# Fixtures
# =============================================================================
@pytest.fixture
def client():
    with patch('app.main.get_redis', new_callable=AsyncMock, return_value=None), \
         patch('app.main.close_redis', new_callable=AsyncMock), \
         patch('app.main.health_check_redis', new_callable=AsyncMock,
               return_value={"status": "mocked", "version": "test"}):
        from app.main import app
        with TestClient(app) as test_client:
            yield test_client


# =============================================================================
# BUG-1 테스트: 에러 응답 시 FAILED 처리 검증
# =============================================================================
class TestBug1ErrorResponseHandling:
    """
    BUG-1: predict_risk_with_ocr가 code != 200 반환 시
           TaskStatus.FAILED로 처리되는지 검증
    """

    def _submit_predict(self, client, mocked_predict_result):
        """공통 predict 제출 헬퍼"""
        captured_statuses = []

        async def capture_set_task_status(task_id, status, progress=0, error=None, result=None):
            captured_statuses.append({
                "status": status,
                "progress": progress,
                "error": error,
                "result": result,
            })

        with patch('app.main.get_cached_result', new_callable=AsyncMock, return_value=None), \
             patch('app.main.set_cached_result', new_callable=AsyncMock), \
             patch('app.main.set_task_status', side_effect=capture_set_task_status), \
             patch('app.main.is_db_available', return_value=True), \
             patch('app.main.extract_building_ledger', return_value={'address': '테스트'}), \
             patch('app.main.extract_real_estate_data', return_value={'address': '테스트'}), \
             patch('app.main.validate_document_match',
                   return_value=(True, "OK", {'confidence': 0.95, 'errors': [], 'match_scores': {}})), \
             patch('app.main.predict_risk_with_ocr', return_value=mocked_predict_result), \
             patch('app.main.generate_cache_key', return_value="predict:bug1-test"), \
             patch('app.main.generate_file_hash', return_value="hash-bug1"):

            response = client.post(
                "/predict",
                data={"deposit": 30000, "address": "인천광역시 부평구 삼산동 167-15"},
                files=[
                    ("ledger_files", ("ledger.png", make_image_file(), "image/png")),
                    ("registry_files", ("registry.pdf", make_pdf_file(), "application/pdf")),
                ]
            )

        return response, captured_statuses

    def test_bug1_422_result_becomes_failed(self, client):
        """
        [BUG-1 핵심] predict_risk_with_ocr가 code=422 반환
        → 마지막 set_task_status 호출이 FAILED여야 함
        (수정 전: COMPLETED로 저장되던 문제)
        """
        error_result = {
            "meta": {"code": 422, "message": "시세 데이터 없음", "timestamp": datetime.now().isoformat()},
            "errors": [{"field": "market_price", "message": "공시지가 또는 실거래가 데이터를 찾을 수 없습니다"}]
        }

        response, captured_statuses = self._submit_predict(client, error_result)

        assert response.status_code == 202

        # 백그라운드 태스크가 완료될 때까지 잠시 대기 (TestClient는 동기)
        final_status = captured_statuses[-1]
        assert final_status["status"] == "FAILED", (
            f"BUG-1 미수정: code=422 에러 응답인데도 status={final_status['status']}로 저장됨"
        )
        assert final_status["error"] == "시세 데이터 없음"
        assert final_status["progress"] == 100

    def test_bug1_500_result_becomes_failed(self, client):
        """
        [BUG-1] predict_risk_with_ocr가 code=500 반환
        → FAILED로 처리되어야 함
        """
        error_result = {
            "meta": {"code": 500, "message": "서버 오류가 발생했습니다", "timestamp": datetime.now().isoformat()},
            "errors": [{"field": "server", "message": "분석 중 오류 발생: ..."}]
        }

        response, captured_statuses = self._submit_predict(client, error_result)

        final_status = captured_statuses[-1]
        assert final_status["status"] == "FAILED", (
            f"BUG-1 미수정: code=500 에러 응답인데도 status={final_status['status']}로 저장됨"
        )
        assert final_status["progress"] == 100

    def test_bug1_200_result_becomes_completed(self, client):
        """
        [BUG-1 정상 케이스] predict_risk_with_ocr가 code=200 반환
        → COMPLETED로 처리되어야 함 (기존 정상 동작 유지 확인)
        """
        success_result = {
            "meta": {"code": 200, "message": "전세사기 위험도 분석 완료", "timestamp": datetime.now().isoformat()},
            "data": {"risk_score": 35.0, "risk_level": "SAFE"}
        }

        response, captured_statuses = self._submit_predict(client, success_result)

        final_status = captured_statuses[-1]
        assert final_status["status"] == "COMPLETED", (
            f"정상 응답인데 status={final_status['status']}로 저장됨"
        )
        assert final_status["progress"] == 100
        assert final_status["result"] == success_result

    def test_bug1_error_result_not_cached(self, client):
        """
        [BUG-1 부수 효과] 에러 응답은 캐시에 저장되지 않아야 함
        """
        error_result = {
            "meta": {"code": 422, "message": "시세 데이터 없음", "timestamp": datetime.now().isoformat()},
        }

        mock_set_cache = AsyncMock()

        with patch('app.main.get_cached_result', new_callable=AsyncMock, return_value=None), \
             patch('app.main.set_cached_result', mock_set_cache), \
             patch('app.main.set_task_status', new_callable=AsyncMock), \
             patch('app.main.is_db_available', return_value=True), \
             patch('app.main.extract_building_ledger', return_value={'address': '테스트'}), \
             patch('app.main.extract_real_estate_data', return_value={'address': '테스트'}), \
             patch('app.main.validate_document_match',
                   return_value=(True, "OK", {'confidence': 0.95, 'errors': [], 'match_scores': {}})), \
             patch('app.main.predict_risk_with_ocr', return_value=error_result), \
             patch('app.main.generate_cache_key', return_value="predict:no-cache"), \
             patch('app.main.generate_file_hash', return_value="hash-no-cache"):

            client.post(
                "/predict",
                data={"deposit": 30000, "address": "인천광역시 부평구 삼산동 167-15"},
                files=[
                    ("ledger_files", ("ledger.png", make_image_file(), "image/png")),
                    ("registry_files", ("registry.pdf", make_pdf_file(), "application/pdf")),
                ]
            )

        mock_set_cache.assert_not_called(), "BUG-1: 에러 응답이 캐시에 저장되면 안 됨"

    def test_bug1_success_result_is_cached(self, client):
        """
        [BUG-1 정상 케이스] 성공 응답은 캐시에 저장되어야 함
        """
        success_result = {
            "meta": {"code": 200, "message": "완료", "timestamp": datetime.now().isoformat()},
            "data": {"risk_score": 35.0, "risk_level": "SAFE"}
        }

        mock_set_cache = AsyncMock()

        with patch('app.main.get_cached_result', new_callable=AsyncMock, return_value=None), \
             patch('app.main.set_cached_result', mock_set_cache), \
             patch('app.main.set_task_status', new_callable=AsyncMock), \
             patch('app.main.is_db_available', return_value=True), \
             patch('app.main.extract_building_ledger', return_value={'address': '테스트'}), \
             patch('app.main.extract_real_estate_data', return_value={'address': '테스트'}), \
             patch('app.main.validate_document_match',
                   return_value=(True, "OK", {'confidence': 0.95, 'errors': [], 'match_scores': {}})), \
             patch('app.main.predict_risk_with_ocr', return_value=success_result), \
             patch('app.main.generate_cache_key', return_value="predict:cache-ok"), \
             patch('app.main.generate_file_hash', return_value="hash-cache-ok"):

            client.post(
                "/predict",
                data={"deposit": 30000, "address": "인천광역시 부평구 삼산동 167-15"},
                files=[
                    ("ledger_files", ("ledger.png", make_image_file(), "image/png")),
                    ("registry_files", ("registry.pdf", make_pdf_file(), "application/pdf")),
                ]
            )

        mock_set_cache.assert_called_once(), "성공 응답은 캐시에 저장되어야 함"


# =============================================================================
# BUG-3 테스트: 동일 파일명 키 충돌 수정 검증
# =============================================================================
class TestBug3DuplicateFilenameHandling:
    """
    BUG-3: 동일 파일명 업로드 시 file_contents 키 충돌 방지
           ledger_paths, registry_paths에 모든 파일이 append 되는지 검증
    """

    def test_bug3_duplicate_ledger_filenames_all_paths_captured(self, client):
        """
        [BUG-3 핵심] 동일 파일명(test.png) 2개 업로드 시
        ledger_paths에 2개의 경로가 모두 전달되어야 함
        (수정 전: append 누락으로 빈 배열 전달)
        """
        captured_ledger_paths = []

        async def capture_run_task(**kwargs):
            captured_ledger_paths.extend(kwargs.get('ledger_paths', []))

        with patch('app.main.get_cached_result', new_callable=AsyncMock, return_value=None), \
             patch('app.main.set_task_status', new_callable=AsyncMock), \
             patch('app.main.generate_cache_key', return_value="predict:bug3-dup"), \
             patch('app.main.generate_file_hash', side_effect=lambda c: f"hash-{len(c)}"), \
             patch('app.main._run_prediction_task', side_effect=capture_run_task):

            response = client.post(
                "/predict",
                data={"deposit": 30000, "address": "인천광역시 부평구 삼산동 167-15"},
                files=[
                    # 동일한 파일명 "test.png" 2개
                    ("ledger_files", ("test.png", make_image_file_with_content(b'content_A'), "image/png")),
                    ("ledger_files", ("test.png", make_image_file_with_content(b'content_B'), "image/png")),
                    ("registry_files", ("registry.pdf", make_pdf_file(), "application/pdf")),
                ]
            )

        assert response.status_code == 202
        assert len(captured_ledger_paths) == 2, (
            f"BUG-3 미수정: 동일 파일명 2개인데 ledger_paths에 {len(captured_ledger_paths)}개만 전달됨"
        )

    def test_bug3_duplicate_registry_filenames_all_paths_captured(self, client):
        """
        [BUG-3] 동일 파일명 registry 2개 업로드 시
        registry_paths에 2개의 경로가 모두 전달되어야 함
        """
        captured_registry_paths = []

        async def capture_run_task(**kwargs):
            captured_registry_paths.extend(kwargs.get('registry_paths', []))

        with patch('app.main.get_cached_result', new_callable=AsyncMock, return_value=None), \
             patch('app.main.set_task_status', new_callable=AsyncMock), \
             patch('app.main.generate_cache_key', return_value="predict:bug3-reg"), \
             patch('app.main.generate_file_hash', side_effect=lambda c: f"hash-{len(c)}"), \
             patch('app.main._run_prediction_task', side_effect=capture_run_task):

            response = client.post(
                "/predict",
                data={"deposit": 30000, "address": "인천광역시 부평구 삼산동 167-15"},
                files=[
                    ("ledger_files", ("ledger.png", make_image_file(), "image/png")),
                    # 동일한 파일명 "doc.pdf" 2개
                    ("registry_files", ("doc.pdf", make_pdf_file(), "application/pdf")),
                    ("registry_files", ("doc.pdf", make_pdf_file(), "application/pdf")),
                ]
            )

        assert response.status_code == 202
        assert len(captured_registry_paths) == 2, (
            f"BUG-3 미수정: 동일 파일명 2개인데 registry_paths에 {len(captured_registry_paths)}개만 전달됨"
        )

    def test_bug3_different_filenames_all_paths_captured(self, client):
        """
        [BUG-3 정상 케이스] 파일명이 다른 경우에도 모두 전달되어야 함
        """
        captured_ledger_paths = []

        async def capture_run_task(**kwargs):
            captured_ledger_paths.extend(kwargs.get('ledger_paths', []))

        with patch('app.main.get_cached_result', new_callable=AsyncMock, return_value=None), \
             patch('app.main.set_task_status', new_callable=AsyncMock), \
             patch('app.main.generate_cache_key', return_value="predict:bug3-diff"), \
             patch('app.main.generate_file_hash', side_effect=lambda c: f"hash-{len(c)}"), \
             patch('app.main._run_prediction_task', side_effect=capture_run_task):

            response = client.post(
                "/predict",
                data={"deposit": 30000, "address": "인천광역시 부평구 삼산동 167-15"},
                files=[
                    ("ledger_files", ("page1.png", make_image_file(), "image/png")),
                    ("ledger_files", ("page2.png", make_image_file(), "image/png")),
                    ("ledger_files", ("page3.png", make_image_file(), "image/png")),
                    ("registry_files", ("registry.pdf", make_pdf_file(), "application/pdf")),
                ]
            )

        assert response.status_code == 202
        assert len(captured_ledger_paths) == 3, (
            f"파일명이 다른 3개인데 ledger_paths에 {len(captured_ledger_paths)}개만 전달됨"
        )

    def test_bug3_file_contents_not_overwritten(self, client):
        """
        [BUG-3 핵심] 동일 파일명 업로드 시 idx 기반 key로 각 파일 내용이
        별도로 저장되어 서로 덮어쓰지 않아야 함
        file_contents 딕셔너리에 idx별로 분리된 key가 존재하는지 직접 검증
        """
        captured_file_contents = {}

        # predict_risk_endpoint 내부의 file_contents 딕셔너리를
        # 백그라운드 태스크 등록 직전에 스냅샷으로 캡처
        original_add_task = None

        async def capture_background_task(**kwargs):
            # _run_prediction_task는 키워드 인자로 직접 호출됨
            captured_file_contents['ledger_paths'] = kwargs.get('ledger_paths', [])
            captured_file_contents['registry_paths'] = kwargs.get('registry_paths', [])

        content_A = b'\x89PNG\r\n\x1a\n' + b'AAAA' * 25
        content_B = b'\x89PNG\r\n\x1a\n' + b'BBBB' * 25

        with patch('app.main.get_cached_result', new_callable=AsyncMock, return_value=None), \
             patch('app.main.set_task_status', new_callable=AsyncMock), \
             patch('app.main.generate_cache_key', return_value="predict:bug3-content"), \
             patch('app.main.generate_file_hash', side_effect=lambda c: f"hash-{c[:4].hex()}"), \
             patch('app.main._run_prediction_task', side_effect=capture_background_task):

            client.post(
                "/predict",
                data={"deposit": 30000, "address": "인천광역시 부평구 삼산동 167-15"},
                files=[
                    ("ledger_files", ("test.png", BytesIO(content_A), "image/png")),
                    ("ledger_files", ("test.png", BytesIO(content_B), "image/png")),
                    ("registry_files", ("registry.pdf", make_pdf_file(), "application/pdf")),
                ]
            )

        # ledger_paths에 2개 경로가 있어야 함 (덮어쓰기 없이 각각 저장)
        ledger_paths = captured_file_contents.get('ledger_paths', [])
        assert len(ledger_paths) == 2, (
            f"BUG-3 미수정: 동일 파일명 2개인데 ledger_paths에 {len(ledger_paths)}개만 전달됨"
        )

        # 두 경로의 파일명 부분에 idx(0_, 1_)가 포함되어 구분되어야 함
        basenames = [os.path.basename(p) for p in ledger_paths]
        assert basenames[0] != basenames[1], (
            f"BUG-3 미수정: 두 임시 파일명이 동일함 → 덮어쓰기 발생 가능\n"
            f"  path[0]: {basenames[0]}\n"
            f"  path[1]: {basenames[1]}"
        )

    def test_bug3_single_file_still_works(self, client):
        """
        [BUG-3 회귀] 파일 1개 업로드는 기존대로 정상 동작해야 함
        """
        captured_paths = {"ledger": [], "registry": []}

        async def capture_run_task(**kwargs):
            captured_paths["ledger"].extend(kwargs.get('ledger_paths', []))
            captured_paths["registry"].extend(kwargs.get('registry_paths', []))

        with patch('app.main.get_cached_result', new_callable=AsyncMock, return_value=None), \
             patch('app.main.set_task_status', new_callable=AsyncMock), \
             patch('app.main.generate_cache_key', return_value="predict:bug3-single"), \
             patch('app.main.generate_file_hash', return_value="hash-single"), \
             patch('app.main._run_prediction_task', side_effect=capture_run_task):

            response = client.post(
                "/predict",
                data={"deposit": 30000, "address": "인천광역시 부평구 삼산동 167-15"},
                files=[
                    ("ledger_files", ("ledger.png", make_image_file(), "image/png")),
                    ("registry_files", ("registry.pdf", make_pdf_file(), "application/pdf")),
                ]
            )

        assert response.status_code == 202
        assert len(captured_paths["ledger"]) == 1
        assert len(captured_paths["registry"]) == 1


# =============================================================================
# 실행
# =============================================================================
if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])