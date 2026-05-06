"""
위험도 계산 서비스

담당 기능:
- OCR/DB 원천 데이터 → feature_service 피처 변환 (어댑터)
- AI 모델 예측
- 하이브리드 위험 등급 판정 (룰 베이스 60% + ML 40%)
- 위험 요인 분석

[변경 이력]
- 중복 calculate_risk_features() 제거 → feature_service.py로 단일화
- build_features_from_sources(): OCR/DB dict → feature_service 시그니처 변환 어댑터 추가
- predict_with_model(): feature_service.convert_to_model_input() 사용으로 통일
- bare except → except Exception 수정
- [NEW] 하이브리드 등급 판정: determine_risk_level() → 룰 베이스 + ML 가중 결합
- [NEW] _rule_based_score(): 룰 베이스 점수를 0~1 연속값으로 반환
"""
import os
import logging
from datetime import datetime
from typing import Dict, List, Any, Optional

import numpy as np
import pandas as pd
import joblib

# 단일 피처 소스
from app.services.feature_service import (
    calculate_risk_features,
    convert_to_model_input,
    TRAIN_FEATURES,
)

logger = logging.getLogger(__name__)

# 프로젝트 루트
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))

# =============================================================================
# 하이브리드 가중치 설정
# =============================================================================
RULE_WEIGHT = 0.6   # 룰 베이스 비중
ML_WEIGHT = 0.4     # ML 모델 비중


# =============================================================================
# AI 모델 로더 (Lazy Loading)
# =============================================================================
_rf_model = None


def get_model():
    """Random Forest 모델 로드 (싱글톤)"""
    global _rf_model

    if _rf_model is not None:
        return _rf_model

    model_path = os.path.join(PROJECT_ROOT, 'models', 'fraud_rf_model.pkl')

    if not os.path.exists(model_path):
        logger.warning(f"모델 파일 없음: {model_path}")
        return None

    try:
        _rf_model = joblib.load(model_path)
        logger.info(f"모델 로드 성공: {model_path}")
        return _rf_model
    except Exception as e:
        logger.error(f"모델 로드 실패: {e}")
        return None


# =============================================================================
# 어댑터: OCR/DB 원천 데이터 → feature_service 피처 변환
# =============================================================================
def build_features_from_sources(
        deposit_manwon: float,
        market_price_manwon: float,
        public_price_won: float = 0,
        building_info: Optional[Dict] = None,
        ocr_features: Optional[Dict] = None
) -> Dict[str, Any]:
    """
    predict_service에서 호출하는 어댑터 함수.
    OCR 결과 dict, DB 건물 정보 dict 등 다양한 소스에서
    feature_service.calculate_risk_features()가 요구하는 시그니처로 변환합니다.

    Args:
        deposit_manwon: 보증금 (만원)
        market_price_manwon: 시세 (만원)
        public_price_won: 공시가 (원) — 참고용으로 결과에 추가
        building_info: 건물 정보 (DB 조회 결과)
        ocr_features: OCR 추출 피처

    Returns:
        피처 딕셔너리 (TRAIN_FEATURES + _ref_ 참고 수치 + 어댑터 추가 키)
    """
    building_info = building_info or {}
    ocr_features = ocr_features or {}

    # --- 1. 원천 데이터에서 feature_service 파라미터 추출 ---

    # 선순위 채권 (만원)
    real_debt = ocr_features.get('real_debt_manwon', 0)

    # 신탁 여부
    owner_name = str(building_info.get('owner_name', ''))
    is_trust = ocr_features.get('is_trust_owner', 0) or (1 if '신탁' in owner_name else 0)

    # 단기 소유 가중치
    short_term_weight = ocr_features.get('short_term_weight', 0)
    if short_term_weight == 0 and building_info.get('ownership_changed_date'):
        try:
            own_date = pd.to_datetime(building_info['ownership_changed_date'])
            days_diff = (datetime.now() - own_date).days

            if days_diff < 90:
                short_term_weight = 0.3
            elif days_diff < 730:
                short_term_weight = 0.1
        except Exception:
            pass

    # 주용도
    main_use = str(building_info.get('main_use', '') or ocr_features.get('main_use', ''))

    # 위반 건축물 여부
    is_illegal = ocr_features.get('is_illegal', 0) or (
        1 if str(building_info.get('is_violating_building', '')).strip() == 'Y' else 0
    )

    # 사용승인일
    use_apr_day = building_info.get('use_apr_day') or ocr_features.get('usage_approval_date', '')

    # 세대수
    household_cnt = building_info.get('household_cnt') or 1
    if household_cnt < 1:
        household_cnt = 1

    # --- 2. 단일 피처 함수 호출 ---
    features = calculate_risk_features(
        deposit_amount=deposit_manwon,
        market_price=market_price_manwon,
        real_debt=real_debt,
        main_use=main_use,
        usage_approval_date=use_apr_day,
        is_illegal=is_illegal,
        is_trust_owner=is_trust,
        short_term_weight=short_term_weight,
        household_count=household_cnt,
    )

    # --- 3. predict_service가 필요로 하는 추가 키 보강 ---
    # 공시가 기반 HUG 위험 비율 (실제 공시가가 있는 경우 덮어쓰기)
    if public_price_won > 0:
        hug_limit_manwon = (public_price_won * 1.26) / 10000
        features['_ref_hug_risk_ratio'] = deposit_manwon / hug_limit_manwon if hug_limit_manwon > 0 else 0

    # predict_service에서 사용하는 레거시 키 호환
    # (analyze_risk_factors, API 응답 등에서 참조)
    features['jeonse_ratio'] = features.get('jeonse_ratio', 0)
    features['total_risk_ratio'] = features.get('_ref_total_risk_ratio', 0)
    features['hug_risk_ratio'] = features.get('_ref_hug_risk_ratio', 0)
    features['real_debt'] = real_debt

    return features


# =============================================================================
# 룰 베이스 위험도 점수 (0.0 ~ 1.0 연속값)
# =============================================================================
def _rule_based_score(features: Dict[str, Any]) -> float:
    """
    룰 베이스 위험도 점수를 0.0 ~ 1.0 연속값으로 반환.

    하이브리드 판정에서 ML 점수와 결합하기 위해 사용.
    기존 _rule_based_prediction()의 로직을 그대로 유지하되,
    명확히 "룰 베이스 점수"임을 구분.
    """
    score = 0.0

    # 전세가율
    jeonse_ratio = features.get('jeonse_ratio', 0)
    if jeonse_ratio >= 0.9:
        score = max(score, 0.95)
    elif jeonse_ratio >= 0.8:
        score = max(score, 0.8)
    elif jeonse_ratio >= 0.7:
        score = max(score, 0.5)
    elif jeonse_ratio >= 0.6:
        score = max(score, 0.3)

    # 총 위험 비율 (선순위 채권 포함)
    total_risk = features.get('total_risk_ratio', 0) or features.get('_ref_total_risk_ratio', 0)
    if total_risk >= 0.9:
        score = max(score, 0.9)
    elif total_risk >= 0.8:
        score = max(score, 0.7)

    # HUG 보증보험 불가
    hug_risk = features.get('hug_risk_ratio', 0) or features.get('_ref_hug_risk_ratio', 0)
    if hug_risk > 1.0:
        score = max(score, 0.85)

    # 위반 건축물
    if features.get('is_illegal', 0):
        score = max(score, 0.6)

    # 신탁 + 단기 소유 (복합 위험)
    if features.get('is_trust_owner', 0) and features.get('short_term_weight', 0) > 0:
        score = max(score, 0.6)
    elif features.get('is_trust_owner', 0):
        score = max(score, 0.4)
    elif features.get('short_term_weight', 0) >= 0.3:
        score = max(score, 0.45)

    return float(np.clip(score, 0.0, 1.0))


# =============================================================================
# AI 모델 예측
# =============================================================================
def predict_with_model(features: Dict[str, Any]) -> float:
    """
    AI 모델로 위험 확률 예측

    Args:
        features: 피처 딕셔너리

    Returns:
        위험 확률 (0.0 ~ 1.0)
    """
    model = get_model()

    if model is None:
        # 모델 없으면 None 반환 (하이브리드에서 룰 베이스 100%로 처리)
        return None

    try:
        df_input = convert_to_model_input(features)
        prob = model.predict_proba(df_input)[0][1]
        return float(prob)

    except Exception as e:
        logger.error(f"모델 예측 오류: {e}")
        return None


# =============================================================================
# 하이브리드 위험도 판정 (핵심 변경)
# =============================================================================
def calculate_hybrid_score(features: Dict[str, Any]) -> Dict[str, Any]:
    """
    룰 베이스 점수와 ML 모델 점수를 가중 결합하여
    최종 위험 점수와 등급을 산출.

    가중치: 룰 베이스 60% + ML 40% (ML 모델이 없으면 룰 베이스 100%)

    Args:
        features: 피처 딕셔너리

    Returns:
        {
            "final_score": float (0~100),
            "risk_level": str ("SAFE" | "CAUTION" | "RISKY"),
            "rule_score": float (0~1),
            "ml_score": float | None (0~1),
            "weights": {"rule": float, "ml": float}
        }
    """
    # 1. 룰 베이스 점수 계산
    rule_score = _rule_based_score(features)

    # 2. ML 모델 점수 계산
    ml_score = predict_with_model(features)

    # 3. 가중 결합
    if ml_score is not None:
        combined = rule_score * RULE_WEIGHT + ml_score * ML_WEIGHT
        used_weights = {"rule": RULE_WEIGHT, "ml": ML_WEIGHT}
    else:
        # ML 모델이 없으면 룰 베이스 100%
        combined = rule_score
        used_weights = {"rule": 1.0, "ml": 0.0}
        logger.info("ML 모델 미사용 — 룰 베이스 100%로 판정")

    # 4. 0~100 점수 변환
    final_score = round(combined * 100, 2)

    # 5. 등급 판정
    risk_level = _score_to_level(combined)

    return {
        "final_score": final_score,
        "risk_level": risk_level,
        "rule_score": round(rule_score, 4),
        "ml_score": round(ml_score, 4) if ml_score is not None else None,
        "weights": used_weights,
    }


def _score_to_level(score: float) -> str:
    """
    결합 점수 → 위험 등급 변환

    Args:
        score: 결합 점수 (0.0 ~ 1.0)

    Returns:
        "SAFE" | "CAUTION" | "RISKY"
    """
    if score >= 0.7:
        return "RISKY"
    elif score >= 0.4:
        return "CAUTION"
    else:
        return "SAFE"


# =============================================================================
# 하위 호환용: determine_risk_level (기존 인터페이스 유지)
# =============================================================================
def determine_risk_level(prob: float) -> str:
    """
    [하위 호환] 단일 확률값 → 등급 변환.

    주의: 새 코드에서는 calculate_hybrid_score()를 사용하세요.
    이 함수는 기존 코드와의 호환성을 위해 유지됩니다.
    """
    return _score_to_level(prob)


# =============================================================================
# 위험 요인 분석
# =============================================================================
def analyze_risk_factors(
        features: Dict[str, Any],
        is_hug_safe: bool
) -> List[Dict[str, str]]:
    """
    위험 요인을 구조화된 형태로 분석

    Args:
        features: 피처 딕셔너리
        is_hug_safe: HUG 가입 가능 여부

    Returns:
        위험 요인 리스트 [{"type": ..., "severity": ..., "message": ...}, ...]
    """
    risk_factors = []

    # 1. HUG 불가
    if not is_hug_safe:
        risk_factors.append({
            "type": "HUG_INELIGIBLE",
            "severity": "CRITICAL",
            "message": "HUG 전세보증금 반환보증 가입 불가"
        })

    # 2. 높은 전세가율
    jeonse_ratio = features.get('jeonse_ratio', 0)
    if jeonse_ratio > 0.8:
        risk_factors.append({
            "type": "HIGH_LTV",
            "severity": "HIGH",
            "message": f"전세가율이 {jeonse_ratio * 100:.1f}%로 매우 높음 (깡통전세 위험)"
        })
    elif jeonse_ratio > 0.7:
        risk_factors.append({
            "type": "HIGH_LTV",
            "severity": "MEDIUM",
            "message": f"전세가율이 {jeonse_ratio * 100:.1f}%로 다소 높음"
        })

    # 3. 선순위 채권
    real_debt = features.get('real_debt', 0) or features.get('_ref_real_debt', 0)
    if real_debt > 0:
        risk_factors.append({
            "type": "SENIOR_DEBT",
            "severity": "HIGH",
            "message": f"선순위 채권 {real_debt:,.0f}만원 존재 (보증금 회수 우선순위 낮음)"
        })

    # 4. 위반 건축물
    if features.get('is_illegal', 0):
        risk_factors.append({
            "type": "ILLEGAL_BUILDING",
            "severity": "HIGH",
            "message": "위반 건축물로 등재됨 (법적 제재 가능)"
        })

    # 5. 신탁 부동산
    if features.get('is_trust_owner', 0):
        risk_factors.append({
            "type": "TRUST_PROPERTY",
            "severity": "MEDIUM",
            "message": "신탁 부동산으로 권리 관계가 복잡할 수 있음"
        })

    # 6. 단기 소유
    if features.get('short_term_weight', 0) > 0:
        severity = "HIGH" if features['short_term_weight'] >= 0.3 else "MEDIUM"
        risk_factors.append({
            "type": "SHORT_OWNERSHIP",
            "severity": severity,
            "message": "건물 소유 기간이 짧음 (투기 의심)"
        })

    # 7. 노후 건물
    building_age = features.get('building_age', 0)
    if building_age > 30:
        risk_factors.append({
            "type": "OLD_BUILDING",
            "severity": "LOW",
            "message": f"건물 연식 {building_age:.0f}년으로 노후화됨"
        })

    # 위험 요인 없음
    if not risk_factors:
        risk_factors.append({
            "type": "NONE",
            "severity": "LOW",
            "message": "특이한 위험 요인이 발견되지 않았습니다"
        })

    return risk_factors


# =============================================================================
# 권장사항 생성
# =============================================================================
def generate_recommendations(
        risk_level: str,
        is_hug_safe: bool,
        jeonse_ratio: float
) -> List[str]:
    """
    사용자 권장 조치사항 생성

    Args:
        risk_level: 위험 등급
        is_hug_safe: HUG 가입 가능 여부
        jeonse_ratio: 전세가율

    Returns:
        권장사항 리스트
    """
    recommendations = []

    # HUG 관련
    if is_hug_safe:
        recommendations.append("HUG 보증보험 가입을 권장합니다")
    else:
        recommendations.append("HUG 보증보험 가입이 불가하므로 계약 재검토를 권장합니다")

    # 위험 등급 관련
    if risk_level in ["CAUTION", "RISKY"]:
        recommendations.append("등기부등본 재확인 권장 (최근 3개월 이내)")
        recommendations.append("임대인의 재정 상태 및 다른 채무 여부 확인 필요")

    # 전세가율 관련
    if jeonse_ratio > 0.75:
        recommendations.append("전세가율이 높으므로 월세 전환 또는 보증금 인하 협상 검토")

    # 공통
    recommendations.append("계약 전 법무사 자문을 통한 권리 관계 검토 권장")

    return recommendations