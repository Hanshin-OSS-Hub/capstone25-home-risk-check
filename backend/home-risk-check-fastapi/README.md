# 🏠 전세사기 위험도 분석 시스템 (Fraud Detector AI)

전월세 계약 시 발생할 수 있는 전세사기 위험도를 분석하는 FastAPI 기반 백엔드 서비스입니다.  
건축물대장(이미지)과 등기부등본(PDF)을 업로드하면 OCR로 문서를 파싱하고, 실거래가 데이터와 비교하여 위험도를 예측합니다.

---

## 📁 프로젝트 구조

```
fraud-detector/
├── app/
│   ├── core/
│   │   ├── config.py              # 환경 설정 (Pydantic Settings)
│   │   ├── exceptions.py          # 커스텀 예외 클래스
│   │   └── redis_config.py        # Redis 연결 및 캐시 관리
│   ├── router/
│   │   └── stats.py               # 통계 API 라우터
│   └── services/
│       ├── ocr/
│       │   ├── ledger_parser.py   # 건축물대장 OCR 파서
│       │   └── registry_parser.py # 등기부등본 OCR 파서
│       ├── address_service.py     # 주소 파싱/정규화
│       ├── building_service.py    # 건물 정보 조회
│       ├── document_validator.py  # 문서 일치 검증
│       ├── feature_service.py     # ML 피처 생성
│       ├── map_service.py         # 지도 데이터 서비스
│       ├── ocr_parser_service.py  # OCR 통합 서비스
│       ├── predict_service.py     # 위험도 예측 서비스
│       ├── price_service.py       # 시세 조회 서비스
│       ├── result_service.py      # 결과 포맷팅
│       └── risk_calculator.py     # 위험도 계산 로직
├── scripts/
│   ├── fetch_rent_data.py         # 전월세 실거래가 수집
│   ├── fetch_trade_data.py        # 매매 실거래가 수집
│   ├── import_official_price.py   # 공시지가 수집
│   ├── setup_region_codes.py      # 지역코드 초기화
│   ├── update_coords.py           # 좌표 업데이트
│   └── update_stats.py            # 통계 갱신
├── main.py                        # FastAPI 앱 진입점
├── database.py                    # DB 연결 및 모델 정의
├── schemas.py                     # Pydantic 스키마
├── data_processor.py              # 데이터 전처리
├── train_model.py                 # ML 모델 학습
├── risk_pipeline.py               # 위험도 분석 파이프라인
├── docker-compose.yml             # MySQL + Redis 컨테이너 설정
├── .env.example                   # 환경변수 예시 파일
└── requirements.txt               # Python 의존성
```

---

## ⚙️ 기술 스택

| 분류 | 기술 |
|------|------|
| 웹 프레임워크 | FastAPI + Uvicorn |
| 데이터베이스 | MySQL 8.0 (SQLAlchemy) |
| 캐시/작업큐 | Redis (aioredis) |
| OCR | Google Gemini Vision API |
| 지도/주소 | Kakao Local API |
| 실거래가 | 국토교통부 공공데이터 API |
| ML | Scikit-learn (학습된 모델 사용) |
| 컨테이너 | Docker + Docker Compose |

---

## 🚀 빠른 시작

### 1. 사전 요구사항

- Python 3.10+
- Docker & Docker Compose
- 각 API 키 발급 (아래 환경변수 섹션 참고)

1. 환경 설정
   - .env 작성 (API_SERVICE_KEY, KAKAO_API_KEY, Gemini, DB 접속정보 등)
   - pip install -r requirements.txt
   - DB/Redis 실행 (Docker Compose)

2. setup_region_codes.py
   - data/ 폴더에 법정동코드 CSV 파일 필요
   - meta_sgg_codes, meta_bjdong_codes, regions 테이블 생성

3. update_coords.py
   - regions 테이블의 lat/lng를 Kakao API로 채움
   - setup_region_codes 직후 실행 (update_coords.py 자체 주석에도 명시)

4. fetch_rent_data.py + fetch_trade_data.py
   - meta_sgg_codes 기반으로 전국 데이터 수집
   - 시간이 매우 오래 걸릴 수 있어서 병렬/순차 실행 선택

5. train_model.py
   - raw_rent, raw_trade 데이터가 충분히 쌓인 후 실행
   - data_processor → feature_service → RandomForest 학습 → joblib 저장

6. FastAPI 실행
   - uvicorn app.main:app

---

## 📡 주요 API

### 헬스체크
```
GET /
```

### 위험도 분석 요청 (비동기)
```
POST /predict
```
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `address` | string | 분석할 주소 |
| `deposit` | int | 보증금 (만원) |
| `ledger_files` | File[] | 건축물대장 이미지 (PNG/JPG, 최대 5개, 각 10MB) |
| `registry_files` | File[] | 등기부등본 PDF (최대 3개, 각 20MB) |
| `skip_cache` | bool | 캐시 무시 여부 (기본: false) |

요청 즉시 `task_id`를 반환하고, 백그라운드에서 분석을 수행합니다.

### 분석 결과 조회 (폴링)
```
GET /predict/{task_id}
```
`PENDING` → `PROCESSING` → `COMPLETED` / `FAILED` 순으로 상태가 변경됩니다.

### 캐시 삭제
```
DELETE /predict/cache/{cache_key}
```