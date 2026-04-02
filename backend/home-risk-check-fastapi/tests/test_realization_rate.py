"""
공시가격 현실화율 기반 시세 추정 배수 테스트

테스트 항목:
1. _get_realization_multiplier(): 건물 유형별 배수 매칭 정확성
2. estimate_market_price(): 공시가 fallback 시 유형별 배수 적용 검증
3. predict_service 통합: building_type이 estimate_market_price에 전달되는지 검증

근거: 국토교통부 "'24년 현실화율, 올해와 동일하게 동결" (2023.11.21)
      공동주택 69.0%, 단독주택 53.6%, 토지 65.5%
"""
import pytest
from unittest.mock import patch, MagicMock


# =============================================================================
# 1. _get_realization_multiplier 단위 테스트
# =============================================================================
class TestGetRealizationMultiplier:
    """건물 유형별 배수 매칭 검증"""

    def _get_fn(self):
        from app.services.price_service import _get_realization_multiplier
        return _get_realization_multiplier

    # --- 공동주택 (69.0% → 1.449) ---

    def test_apartment_returns_1_449(self):
        """아파트 → 공동주택 배수 1.449"""
        multiplier, matched = self._get_fn()("아파트")
        assert multiplier == 1.449
        assert matched == "아파트"

    def test_yeonrip_returns_1_449(self):
        """연립주택 → 공동주택 배수 1.449"""
        multiplier, matched = self._get_fn()("연립주택")
        assert multiplier == 1.449
        assert matched == "연립"

    def test_dasedae_returns_1_449(self):
        """다세대주택 → 공동주택 배수 1.449"""
        multiplier, matched = self._get_fn()("다세대주택")
        assert multiplier == 1.449
        assert matched == "다세대"

    # --- 단독주택 (53.6% → 1.866) ---

    def test_dandok_returns_1_866(self):
        """단독주택 → 단독주택 배수 1.866"""
        multiplier, matched = self._get_fn()("단독주택")
        assert multiplier == 1.866
        assert matched == "단독"

    def test_dagagu_returns_1_866(self):
        """다가구주택 → 단독주택 배수 1.866"""
        multiplier, matched = self._get_fn()("다가구주택")
        assert multiplier == 1.866
        assert matched == "다가구"

    # --- 기본값 (매칭 실패 → 공동주택 기준) ---

    def test_empty_string_returns_default(self):
        """빈 문자열 → 기본값(공동주택) 1.449"""
        multiplier, matched = self._get_fn()("")
        assert multiplier == 1.449
        assert "기본값" in matched

    def test_none_returns_default(self):
        """None → 기본값(공동주택) 1.449"""
        multiplier, matched = self._get_fn()(None)
        assert multiplier == 1.449
        assert "기본값" in matched

    def test_unknown_type_returns_default(self):
        """알 수 없는 유형 → 기본값(공동주택) 1.449"""
        multiplier, matched = self._get_fn()("상가")
        assert multiplier == 1.449
        assert "기본값" in matched

    # --- 복합 문자열 매칭 ---

    def test_main_use_with_suffix(self):
        """'제2종근린생활시설(다세대주택)' 같은 복합 문자열에서도 매칭"""
        multiplier, matched = self._get_fn()("제2종근린생활시설(다세대주택)")
        assert multiplier == 1.449
        assert matched == "다세대"


# =============================================================================
# 2. estimate_market_price 공시가 fallback 테스트
# =============================================================================
class TestEstimateMarketPriceRealizationRate:
    """공시가 fallback 시 유형별 배수 적용 검증"""

    @patch('app.services.price_service.get_public_price')
    @patch('app.services.price_service.get_trade_price')
    @patch('app.services.price_service.parse_pnu')
    def test_villa_uses_1_449_multiplier(self, mock_parse, mock_trade, mock_public):
        """다세대주택 → 공시가 × 1.449"""
        from app.services.price_service import estimate_market_price

        mock_parse.return_value = {"sigungu_code": "28200", "bjdong_code": "10100"}
        mock_trade.return_value = (0, "Unknown")  # 실거래가 없음
        mock_public.return_value = 43100000       # 공시가 4,310만원

        price, source = estimate_market_price(
            "2820010100-3-11870003", 36.0,
            fetch_if_missing=False,
            building_type="다세대주택"
        )

        expected = (43100000 / 10000) * 1.449  # 4310 × 1.449 = 6,245.19
        assert abs(price - expected) < 0.01
        assert source == "Public_Price_Estimate"

    @patch('app.services.price_service.get_public_price')
    @patch('app.services.price_service.get_trade_price')
    @patch('app.services.price_service.parse_pnu')
    def test_dandok_uses_1_866_multiplier(self, mock_parse, mock_trade, mock_public):
        """단독주택 → 공시가 × 1.866"""
        from app.services.price_service import estimate_market_price

        mock_parse.return_value = {"sigungu_code": "11110", "bjdong_code": "10100"}
        mock_trade.return_value = (0, "Unknown")
        mock_public.return_value = 50000000  # 공시가 5,000만원

        price, source = estimate_market_price(
            "1111010100-3-00010001", 60.0,
            fetch_if_missing=False,
            building_type="단독주택"
        )

        expected = (50000000 / 10000) * 1.866  # 5000 × 1.866 = 9,330
        assert abs(price - expected) < 0.01
        assert source == "Public_Price_Estimate"

    @patch('app.services.price_service.get_public_price')
    @patch('app.services.price_service.get_trade_price')
    @patch('app.services.price_service.parse_pnu')
    def test_empty_type_uses_default_multiplier(self, mock_parse, mock_trade, mock_public):
        """유형 미지정 → 기본값(공동주택) 배수 1.449"""
        from app.services.price_service import estimate_market_price

        mock_parse.return_value = {"sigungu_code": "28200", "bjdong_code": "10100"}
        mock_trade.return_value = (0, "Unknown")
        mock_public.return_value = 30000000  # 공시가 3,000만원

        price, source = estimate_market_price(
            "2820010100-3-00010001", 40.0,
            fetch_if_missing=False,
            building_type=""
        )

        expected = (30000000 / 10000) * 1.449  # 3000 × 1.449 = 4,347
        assert abs(price - expected) < 0.01

    @patch('app.services.price_service.get_public_price')
    @patch('app.services.price_service.get_trade_price')
    @patch('app.services.price_service.parse_pnu')
    def test_no_building_type_param_uses_default(self, mock_parse, mock_trade, mock_public):
        """building_type 파라미터 생략 시 기본값 적용 (하위호환)"""
        from app.services.price_service import estimate_market_price

        mock_parse.return_value = {"sigungu_code": "28200", "bjdong_code": "10100"}
        mock_trade.return_value = (0, "Unknown")
        mock_public.return_value = 30000000

        # building_type 생략
        price, source = estimate_market_price(
            "2820010100-3-00010001", 40.0,
            fetch_if_missing=False
        )

        expected = (30000000 / 10000) * 1.449
        assert abs(price - expected) < 0.01

    @patch('app.services.price_service.get_trade_price')
    @patch('app.services.price_service.parse_pnu')
    def test_trade_price_exists_ignores_building_type(self, mock_parse, mock_trade):
        """실거래가가 있으면 building_type 무시하고 실거래가 반환"""
        from app.services.price_service import estimate_market_price

        mock_parse.return_value = {"sigungu_code": "28200", "bjdong_code": "10100"}
        mock_trade.return_value = (15000, "DB_Trade")  # 실거래가 1.5억

        price, source = estimate_market_price(
            "2820010100-3-11870003", 36.0,
            fetch_if_missing=False,
            building_type="단독주택"
        )

        assert price == 15000
        assert source == "DB_Trade"


# =============================================================================
# 3. predict_service 통합 테스트 — building_type 전달 검증
# =============================================================================
class TestPredictServiceBuildingTypePassthrough:
    """predict_service가 estimate_market_price에 building_type을 전달하는지 검증"""

    @patch('app.services.predict_service.save_prediction_result')
    @patch('app.services.predict_service.calculate_hybrid_score')
    @patch('app.services.predict_service.build_features_from_sources')
    @patch('app.services.predict_service.calculate_hug_eligibility')
    @patch('app.services.predict_service.get_public_price')
    @patch('app.services.predict_service.estimate_market_price')
    @patch('app.services.predict_service._get_building_info')
    @patch('app.services.predict_service.fetch_building_ledger')
    @patch('app.services.predict_service._resolve_address')
    def test_predict_risk_passes_main_use(
        self, mock_resolve, mock_fetch, mock_building,
        mock_estimate, mock_public, mock_hug,
        mock_features, mock_hybrid, mock_save
    ):
        """predict_risk()가 building_info['main_use']를 전달하는지 확인"""
        from app.services.predict_service import predict_risk

        mock_resolve.return_value = {
            "lot_address": "인천시 남동구 구월동 1187-3",
            "road_address": "인천시 남동구 구월로 100",
            "pnu": "2820010100-3-11870003"
        }
        mock_fetch.return_value = (True, "OK")
        mock_building.return_value = {
            "unique_number": "2820010100-3-11870003",
            "exclusive_area": 36.0,
            "main_use": "다세대주택",
            "owner_name": "홍길동"
        }
        mock_estimate.return_value = (6245, "Public_Price_Estimate")
        mock_public.return_value = 43100000
        mock_hug.return_value = (True, 5430, "가입 가능 (안전 ✅)")
        mock_features.return_value = {"jeonse_ratio": 0.72, "hug_risk_ratio": 0.72}
        mock_hybrid.return_value = {
            "risk_level": "CAUTION",
            "final_score": 55.0,
            "rule_score": 0.6,
            "ml_score": 0.48,
            "weights": {"rule": 0.6, "ml": 0.4}
        }

        predict_risk("인천시 남동구 구월동 1187-3", 4500)

        # estimate_market_price 호출 시 building_type="다세대주택" 전달 확인
        mock_estimate.assert_called_once()
        call_kwargs = mock_estimate.call_args
        assert call_kwargs.kwargs.get('building_type') == "다세대주택" or \
               (len(call_kwargs.args) >= 3 and "다세대" in str(call_kwargs))

    @patch('app.services.predict_service.save_prediction_result')
    @patch('app.services.predict_service.calculate_hybrid_score')
    @patch('app.services.predict_service.build_features_from_sources')
    @patch('app.services.predict_service.calculate_hug_eligibility')
    @patch('app.services.predict_service.get_public_price')
    @patch('app.services.predict_service.estimate_market_price')
    @patch('app.services.predict_service.extract_ocr_features')
    def test_predict_risk_with_ocr_passes_main_use(
        self, mock_ocr, mock_estimate, mock_public, mock_hug,
        mock_features, mock_hybrid, mock_save
    ):
        """predict_risk_with_ocr()가 ocr_features['main_use']를 전달하는지 확인"""
        from app.services.predict_service import predict_risk_with_ocr

        mock_ocr.return_value = {
            "unique_number": "2820010100-3-11870003",
            "main_use": "단독주택",
            "area_size": 60.0,
            "is_illegal": 0,
            "is_trust_owner": 0,
            "short_term_weight": 0,
            "real_debt_manwon": 0,
            "ownership_duration_months": 24,
            "usage_approval_date": "2000-01-01"
        }
        mock_estimate.return_value = (9330, "Public_Price_Estimate")
        mock_public.return_value = 50000000
        mock_hug.return_value = (True, 6300, "가입 가능 (안전 ✅)")
        mock_features.return_value = {"jeonse_ratio": 0.54, "hug_risk_ratio": 0.54}
        mock_hybrid.return_value = {
            "risk_level": "SAFE",
            "final_score": 30.0,
            "rule_score": 0.3,
            "ml_score": 0.3,
            "weights": {"rule": 0.6, "ml": 0.4}
        }

        predict_risk_with_ocr(
            "서울시 종로구 청운동 1-2", 5000,
            {"ledger": {}, "registry": {}}
        )

        mock_estimate.assert_called_once()
        call_kwargs = mock_estimate.call_args
        assert call_kwargs.kwargs.get('building_type') == "단독주택" or \
               (len(call_kwargs.args) >= 3 and "단독" in str(call_kwargs))


# =============================================================================
# 4. 현실화율 배수 값 정확성 테스트
# =============================================================================
class TestRealizationMultiplierValues:
    """국토부 현실화율 기반 배수 값 정확성 검증"""

    def test_multiplier_matches_realization_rate_69(self):
        """공동주택: 1/0.69 = 1.449 (소수점 3자리)"""
        expected = round(1 / 0.69, 3)
        assert expected == 1.449

    def test_multiplier_matches_realization_rate_536(self):
        """단독주택: 1/0.536 = 1.866 (소수점 3자리)"""
        expected = round(1 / 0.536, 3)
        assert expected == 1.866

    def test_default_multiplier_is_most_conservative(self):
        """기본값은 가장 보수적(시세 낮게 추정)인 공동주택 배수"""
        from app.services.price_service import DEFAULT_MULTIPLIER, REALIZATION_MULTIPLIER
        all_multipliers = list(REALIZATION_MULTIPLIER.values())
        assert DEFAULT_MULTIPLIER == min(all_multipliers)