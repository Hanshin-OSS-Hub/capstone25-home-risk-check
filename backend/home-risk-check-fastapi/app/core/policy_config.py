"""
HUG·공시가격 현실화율 정책 이력 관리

담당 기능:
- HUG 보증보험 공시가 배수 (적용일자 기준 조회)
- 공시가격 현실화율 기반 시세 추정 배수 (건물유형 + 적용일자)

[설계 원칙]
- 정책 이력은 코드에 명시적으로 기록 (git history로 추적 가능)
- DB 의존성 없음 (변경 빈도 낮음, 정책은 정적 데이터)
- 적용일자 기준 조회로 과거 시점 분석 가능 (계약일 기준 등)

[근거 문서]
- HUG: 「전세보증금반환보증 운영지침」
- 현실화율: 국토교통부 「부동산 공시가격 현실화 계획」 (2020.11.03)
  2023~2026년 4년 연속 동결 (2025.11.13 중앙부동산가격공시위원회 심의·의결)
  https://www.korea.kr/news/policyNewsView.do?newsId=148879449
"""
from datetime import date
from typing import Dict, List, NamedTuple, Optional, Tuple


# =============================================================================
# 1. HUG 공시가 배수 정책
# =============================================================================
class HugPolicy(NamedTuple):
    """HUG 보증보험 한도 산정용 공시가 배수 정책 한 건"""
    public_price_multiplier: float    # 공시가 × N = HUG 한도
    effective_from: date              # 적용 시작일 (포함)
    effective_to: Optional[date]      # 적용 종료일 (포함). None = 현재 유효
    source: str                       # 근거 (지침명·개정일)


# 신규 정책이 위에 오도록 정렬 (조회 시 첫 매치 반환)
HUG_POLICY_HISTORY: List[HugPolicy] = [
    HugPolicy(
        public_price_multiplier=1.26,           # 공시가 × 140% × 90%
        effective_from=date(2023, 5, 1),
        effective_to=None,
        source="HUG 전세보증금반환보증 운영지침 (2023.05.01 개정)",
    ),
    HugPolicy(
        public_price_multiplier=1.50,           # 공시가 × 150% × 100% (구 기준)
        effective_from=date(2017, 1, 1),
        effective_to=date(2023, 4, 30),
        source="HUG 전세보증금반환보증 운영지침 (구 기준, ~2023.04.30)",
    ),
]


def get_hug_multiplier(at: Optional[date] = None) -> Tuple[float, str]:
    """
    주어진 시점에 적용되는 HUG 공시가 배수 반환.

    Args:
        at: 기준 일자. None이면 오늘 날짜 사용.

    Returns:
        (배수, 근거) 튜플

    Raises:
        ValueError: 해당 시점에 적용되는 정책이 없는 경우
    """
    target = at or date.today()
    for policy in HUG_POLICY_HISTORY:
        if policy.effective_from <= target and (
            policy.effective_to is None or target <= policy.effective_to
        ):
            return policy.public_price_multiplier, policy.source
    raise ValueError(f"{target}에 적용되는 HUG 정책이 없습니다")


# =============================================================================
# 2. 공시가격 현실화율 기반 배수 정책
# =============================================================================
class RealizationPolicy(NamedTuple):
    """공시가→시세 추정용 배수 정책 한 건"""
    multipliers_by_keyword: Dict[str, float]   # 건물유형 키워드 → 배수
    default_multiplier: float                   # 매칭 실패 시 기본값
    effective_from: date
    effective_to: Optional[date]
    source: str


# 2023~2026년 동결 정책 적용
# - 공동주택 현실화율 69.0% → 1/0.69 = 1.449
# - 단독주택 현실화율 53.6% → 1/0.536 = 1.866
# - 오피스텔은 국세청 기준시가로 별도 관리되나, 공동주택과 유사 분포로 1.449 적용
REALIZATION_POLICY_HISTORY: List[RealizationPolicy] = [
    RealizationPolicy(
        multipliers_by_keyword={
            "아파트":    1.449,    # 공동주택 69.0%
            "연립":      1.449,
            "다세대":    1.449,
            "오피스텔":  1.449,
            "단독":      1.866,    # 단독주택 53.6%
            "다가구":    1.866,
        },
        default_multiplier=1.449,   # 가장 보수적(시세 낮게 추정) = 공동주택 기준
        effective_from=date(2023, 1, 1),
        effective_to=None,           # 2026년까지 동결 확정 (2025.11.13 심의)
        source="국토부 부동산 공시가격 현실화 계획 동결 (2023~2026)",
    ),
]


def get_realization_multiplier(
        building_type: str,
        at: Optional[date] = None,
) -> Tuple[float, str]:
    """
    건물유형 + 적용일자 기준 공시가→시세 변환 배수 반환.

    Args:
        building_type: 주용도 (예: "다세대주택", "아파트", "오피스텔")
        at: 기준 일자. None이면 오늘 날짜 사용.

    Returns:
        (배수, 매칭된 키워드 또는 "기본값(공동주택)") 튜플

    Raises:
        ValueError: 해당 시점에 적용되는 정책이 없는 경우
    """
    target = at or date.today()
    for policy in REALIZATION_POLICY_HISTORY:
        if policy.effective_from <= target and (
            policy.effective_to is None or target <= policy.effective_to
        ):
            if not building_type:
                return policy.default_multiplier, "기본값(공동주택)"
            for keyword, multiplier in policy.multipliers_by_keyword.items():
                if keyword in building_type:
                    return multiplier, keyword
            return policy.default_multiplier, "기본값(공동주택)"
    raise ValueError(f"{target}에 적용되는 현실화율 정책이 없습니다")