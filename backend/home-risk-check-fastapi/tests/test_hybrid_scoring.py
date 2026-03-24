"""
하이브리드 위험도 판정 테스트

테스트 항목:
1. _rule_based_score(): 룰 베이스 점수 계산 정확성
2. _score_to_level(): 점수 → 등급 변환 정확성
3. calculate_hybrid_score(): 하이브리드 결합 로직
   - ML 모델 있을 때: 룰 60% + ML 40%
   - ML 모델 없을 때: 룰 100% 폴백
4. predict_service 통합: calculate_hybrid_score() 호출 및 응답 구조 검증
"""
import pytest
import numpy as np
from unittest.mock import patch, MagicMock


# =============================================================================
# 1. _rule_based_score 테스트
# =============================================================================
class TestRuleBasedScore:
    """룰 베이스 점수 계산 검증"""

    def _get_rule_based_score(self):
        from app.services.risk_calculator import _rule_based_score
        return _rule_based_score

    def _base_features(self, **overrides):
        """기본 안전 피처 (모든 값 0) + 오버라이드"""
        features = {
            'jeonse_ratio': 0.5,
            'total_risk_ratio': 0.5,
            'hug_risk_ratio': 0.5,
            'is_illegal': 0,
            'is_trust_owner': 0,
            'short_term_weight': 0.0,
        }
        features.update(overrides)
        return features

    def test_safe_property_returns_low_score(self):
        """안전한 매물 (전세가율 50%) → 낮은 점수"""
        score = self._get_rule_based_score()(self._base_features())
        assert score < 0.4, f"안전 매물인데 점수가 {score}로 너무 높음"

    def test_high_jeonse_ratio_90_returns_very_high_score(self):
        """전세가율 90% → 0.95"""
        score = self._get_rule_based_score()(self._base_features(jeonse_ratio=0.9))
        assert score == 0.95

    def test_high_jeonse_ratio_80_returns_high_score(self):
        """전세가율 80% → 0.8"""
        score = self._get_rule_based_score()(self._base_features(jeonse_ratio=0.8))
        assert score == 0.8

    def test_jeonse_ratio_70_returns_caution_score(self):
        """전세가율 70% → 0.5"""
        score = self._get_rule_based_score()(self._base_features(jeonse_ratio=0.7))
        assert score == 0.5

    def test_jeonse_ratio_60_returns_moderate_score(self):
        """전세가율 60% → 0.3"""
        score = self._get_rule_based_score()(self._base_features(jeonse_ratio=0.6))
        assert score == 0.3

    def test_total_risk_ratio_90_returns_high_score(self):
        """깡통전세율 90% (선순위 채권 포함) → 0.9"""
        score = self._get_rule_based_score()(self._base_features(total_risk_ratio=0.9))
        assert score >= 0.9

    def test_hug_ineligible_returns_high_score(self):
        """HUG 보증보험 불가 (hug_risk_ratio > 1.0) → 0.85"""
        score = self._get_rule_based_score()(self._base_features(hug_risk_ratio=1.1))
        assert score >= 0.85

    def test_illegal_building_returns_caution_score(self):
        """위반 건축물 → 0.6"""
        score = self._get_rule_based_score()(self._base_features(is_illegal=1))
        assert score == 0.6

    def test_trust_and_short_term_combo_returns_high_score(self):
        """신탁 + 단기소유 복합 → 0.6"""
        score = self._get_rule_based_score()(
            self._base_features(is_trust_owner=1, short_term_weight=0.3)
        )
        assert score == 0.6

    def test_trust_only_returns_moderate_score(self):
        """신탁만 (단기소유 없음) → 0.4"""
        score = self._get_rule_based_score()(
            self._base_features(is_trust_owner=1, short_term_weight=0.0)
        )
        assert score == 0.4

    def test_short_term_only_returns_moderate_score(self):
        """단기소유만 (90일 미만) → 0.45"""
        score = self._get_rule_based_score()(
            self._base_features(is_trust_owner=0, short_term_weight=0.3)
        )
        assert score == 0.45

    def test_max_condition_wins(self):
        """여러 조건 중 가장 높은 점수가 반환됨 (max 로직)"""
        score = self._get_rule_based_score()(
            self._base_features(jeonse_ratio=0.9, is_illegal=1)
        )
        # jeonse_ratio 0.9 → 0.95 vs is_illegal → 0.6 → max = 0.95
        assert score == 0.95

    def test_score_clipped_to_0_1(self):
        """점수는 항상 0.0 ~ 1.0 범위"""
        score = self._get_rule_based_score()(
            self._base_features(jeonse_ratio=0.99, total_risk_ratio=0.99, hug_risk_ratio=2.0)
        )
        assert 0.0 <= score <= 1.0

    def test_ref_prefix_total_risk_ratio_fallback(self):
        """total_risk_ratio가 0이면 _ref_total_risk_ratio를 참조"""
        features = self._base_features(total_risk_ratio=0)
        features['_ref_total_risk_ratio'] = 0.9
        score = self._get_rule_based_score()(features)
        assert score >= 0.9


# =============================================================================
# 2. _score_to_level 테스트
# =============================================================================
class TestScoreToLevel:
    """점수 → 등급 변환 검증"""

    def _get_score_to_level(self):
        from app.services.risk_calculator import _score_to_level
        return _score_to_level

    def test_risky_threshold(self):
        """0.7 이상 → RISKY"""
        assert self._get_score_to_level()(0.7) == "RISKY"
        assert self._get_score_to_level()(0.85) == "RISKY"
        assert self._get_score_to_level()(1.0) == "RISKY"

    def test_caution_threshold(self):
        """0.4 이상 0.7 미만 → CAUTION"""
        assert self._get_score_to_level()(0.4) == "CAUTION"
        assert self._get_score_to_level()(0.55) == "CAUTION"
        assert self._get_score_to_level()(0.69) == "CAUTION"

    def test_safe_threshold(self):
        """0.4 미만 → SAFE"""
        assert self._get_score_to_level()(0.0) == "SAFE"
        assert self._get_score_to_level()(0.2) == "SAFE"
        assert self._get_score_to_level()(0.39) == "SAFE"

    def test_boundary_values(self):
        """경계값 정확성"""
        assert self._get_score_to_level()(0.399) == "SAFE"
        assert self._get_score_to_level()(0.4) == "CAUTION"
        assert self._get_score_to_level()(0.699) == "CAUTION"
        assert self._get_score_to_level()(0.7) == "RISKY"


# =============================================================================
# 3. calculate_hybrid_score 테스트
# =============================================================================
class TestCalculateHybridScore:
    """하이브리드 결합 로직 검증"""

    def _base_features(self, **overrides):
        features = {
            'jeonse_ratio': 0.5,
            'total_risk_ratio': 0.5,
            'hug_risk_ratio': 0.5,
            'is_illegal': 0,
            'is_trust_owner': 0,
            'short_term_weight': 0.0,
            'building_age': 10,
            'is_micro_complex': 0,
            'type_APT': 1, 'type_OFFICETEL': 0, 'type_VILLA': 0, 'type_ETC': 0,
        }
        features.update(overrides)
        return features

    def test_ml_model_applied_with_correct_weights(self):
        """ML 모델이 있을 때 룰 60% + ML 40% 결합"""
        mock_model = MagicMock()
        mock_model.predict_proba.return_value = np.array([[0.6, 0.4]])

        with patch('app.services.risk_calculator.get_model', return_value=mock_model):
            from app.services.risk_calculator import calculate_hybrid_score, _rule_based_score
            features = self._base_features(jeonse_ratio=0.8)
            result = calculate_hybrid_score(features)

        rule_score = result["rule_score"]
        ml_score = result["ml_score"]

        # 가중 결합 검증
        expected = round((rule_score * 0.6 + ml_score * 0.4) * 100, 2)
        assert result["final_score"] == expected
        assert result["weights"] == {"rule": 0.6, "ml": 0.4}

    def test_no_model_falls_back_to_rule_only(self):
        """ML 모델이 없을 때 룰 베이스 100%"""
        with patch('app.services.risk_calculator.get_model', return_value=None):
            from app.services.risk_calculator import calculate_hybrid_score
            features = self._base_features(jeonse_ratio=0.85)
            result = calculate_hybrid_score(features)

        assert result["ml_score"] is None
        assert result["weights"] == {"rule": 1.0, "ml": 0.0}

        # 룰 베이스 점수만으로 최종 점수 결정
        expected = round(result["rule_score"] * 100, 2)
        assert result["final_score"] == expected

    def test_ml_lowers_risk_level(self):
        """
        ML이 안전 판단 시 최종 등급이 내려가는 효과 검증
        룰 베이스: 전세가율 80% → 0.8 (RISKY)
        ML: 0.2 (안전)
        결합: 0.8 * 0.6 + 0.2 * 0.4 = 0.56 → CAUTION
        """
        mock_model = MagicMock()
        mock_model.predict_proba.return_value = np.array([[0.8, 0.2]])

        with patch('app.services.risk_calculator.get_model', return_value=mock_model):
            from app.services.risk_calculator import calculate_hybrid_score
            features = self._base_features(jeonse_ratio=0.8)
            result = calculate_hybrid_score(features)

        assert result["risk_level"] == "CAUTION", (
            f"ML이 안전 판단(0.2)했는데 등급이 {result['risk_level']}로 내려가지 않음"
        )

    def test_ml_raises_risk_level(self):
        """
        ML이 위험 판단 시 최종 등급이 올라가는 효과 검증
        룰 베이스: 전세가율 60% → 0.3 (SAFE 경계)
        ML: 0.9 (위험)
        결합: 0.3 * 0.6 + 0.9 * 0.4 = 0.54 → CAUTION
        """
        mock_model = MagicMock()
        mock_model.predict_proba.return_value = np.array([[0.1, 0.9]])

        with patch('app.services.risk_calculator.get_model', return_value=mock_model):
            from app.services.risk_calculator import calculate_hybrid_score
            features = self._base_features(jeonse_ratio=0.6)
            result = calculate_hybrid_score(features)

        assert result["risk_level"] == "CAUTION", (
            f"ML이 위험 판단(0.9)했는데 등급이 {result['risk_level']}로 올라가지 않음"
        )

    def test_model_error_falls_back_to_rule_only(self):
        """모델 예측 중 에러 발생 시 룰 베이스 100% 폴백"""
        mock_model = MagicMock()
        mock_model.predict_proba.side_effect = Exception("모델 오류")

        with patch('app.services.risk_calculator.get_model', return_value=mock_model):
            from app.services.risk_calculator import calculate_hybrid_score
            features = self._base_features(jeonse_ratio=0.85)
            result = calculate_hybrid_score(features)

        assert result["ml_score"] is None
        assert result["weights"] == {"rule": 1.0, "ml": 0.0}

    def test_result_structure(self):
        """반환 딕셔너리 구조 검증"""
        with patch('app.services.risk_calculator.get_model', return_value=None):
            from app.services.risk_calculator import calculate_hybrid_score
            result = calculate_hybrid_score(self._base_features())

        assert "final_score" in result
        assert "risk_level" in result
        assert "rule_score" in result
        assert "ml_score" in result
        assert "weights" in result
        assert isinstance(result["final_score"], float)
        assert result["risk_level"] in ("SAFE", "CAUTION", "RISKY")

    def test_safe_property_all_clear(self):
        """안전 매물 (전세가율 50%, 위험 요소 없음) → SAFE"""
        with patch('app.services.risk_calculator.get_model', return_value=None):
            from app.services.risk_calculator import calculate_hybrid_score
            result = calculate_hybrid_score(self._base_features(jeonse_ratio=0.5))

        assert result["risk_level"] == "SAFE"

    def test_dangerous_property_all_red(self):
        """위험 매물 (전세가율 95%, 위반건축물, 신탁, 단기소유) → RISKY"""
        with patch('app.services.risk_calculator.get_model', return_value=None):
            from app.services.risk_calculator import calculate_hybrid_score
            features = self._base_features(
                jeonse_ratio=0.95,
                is_illegal=1,
                is_trust_owner=1,
                short_term_weight=0.3
            )
            result = calculate_hybrid_score(features)

        assert result["risk_level"] == "RISKY"
        assert result["final_score"] >= 70


# =============================================================================
# 4. predict_service 통합 테스트 (scoring_detail 응답 검증)
# =============================================================================
class TestPredictServiceHybridIntegration:
    """predict_service에서 하이브리드 판정이 올바르게 호출되는지 검증"""

    def _mock_hybrid_result(self, **overrides):
        result = {
            "final_score": 56.0,
            "risk_level": "CAUTION",
            "rule_score": 0.8,
            "ml_score": 0.2,
            "weights": {"rule": 0.6, "ml": 0.4},
        }
        result.update(overrides)
        return result

    @patch('app.services.predict_service.save_prediction_result')
    @patch('app.services.predict_service.calculate_hybrid_score')
    @patch('app.services.predict_service.build_features_from_sources', return_value={
        'jeonse_ratio': 0.8, 'total_risk_ratio': 0.8,
        'hug_risk_ratio': 0.9, 'is_trust_owner': 0, 'short_term_weight': 0,
        'building_age': 10, 'is_illegal': 0,
    })
    @patch('app.services.predict_service.calculate_hug_eligibility', return_value=(True, 20000, "가입 가능"))
    @patch('app.services.predict_service.get_public_price', return_value=150000000)
    @patch('app.services.predict_service.estimate_market_price', return_value=(25000, "실거래가"))
    @patch('app.services.predict_service._get_building_info', return_value={
        'building_info_id': 1, 'unique_number': 'test', 'detail_address': '테스트 건물',
        'exclusive_area': 59.9
    })
    @patch('app.services.predict_service.fetch_building_ledger', return_value=(True, "OK"))
    @patch('app.services.predict_service._resolve_address', return_value={
        'lot_address': '인천 부평구 삼산동 167-15',
        'road_address': '인천 부평구 삼산로 123',
        'pnu': '2823710100'
    })
    def test_predict_risk_returns_scoring_detail(
        self, mock_addr, mock_ledger, mock_building, mock_market,
        mock_public, mock_hug, mock_features, mock_hybrid, mock_save
    ):
        """predict_risk() 응답에 scoring_detail이 포함되는지 검증"""
        mock_hybrid.return_value = self._mock_hybrid_result()

        from app.services.predict_service import predict_risk
        result = predict_risk("인천광역시 부평구 삼산동 167-15", 20000)

        # scoring_detail 존재 확인
        assert "scoring_detail" in result
        assert result["scoring_detail"]["rule_score"] == 0.8
        assert result["scoring_detail"]["ml_score"] == 0.2
        assert result["scoring_detail"]["weights"] == {"rule": 0.6, "ml": 0.4}

        # 최종 점수와 등급
        assert result["risk_score"] == 56.0
        assert result["risk_level"] == "CAUTION"

    @patch('app.services.predict_service.save_prediction_result')
    @patch('app.services.predict_service.calculate_hybrid_score')
    @patch('app.services.predict_service.build_features_from_sources', return_value={
        'jeonse_ratio': 0.5, 'total_risk_ratio': 0.5,
        'hug_risk_ratio': 0.5, 'is_trust_owner': 0, 'short_term_weight': 0,
        'building_age': 5, 'is_illegal': 0,
    })
    @patch('app.services.predict_service.calculate_hug_eligibility', return_value=(True, 20000, "가입 가능"))
    @patch('app.services.predict_service.get_public_price', return_value=150000000)
    @patch('app.services.predict_service.estimate_market_price', return_value=(30000, "실거래가"))
    @patch('app.services.predict_service._get_building_info', return_value={
        'building_info_id': 1, 'unique_number': 'test', 'detail_address': '안전 건물',
        'exclusive_area': 84.0
    })
    @patch('app.services.predict_service.fetch_building_ledger', return_value=(True, "OK"))
    @patch('app.services.predict_service._resolve_address', return_value={
        'lot_address': '인천 부평구 삼산동 100',
        'road_address': '인천 부평구 삼산로 100',
        'pnu': '2823710100'
    })
    def test_predict_risk_ml_none_shows_rule_only(
        self, mock_addr, mock_ledger, mock_building, mock_market,
        mock_public, mock_hug, mock_features, mock_hybrid, mock_save
    ):
        """ML 모델 없을 때 scoring_detail에 ml_score=None, 가중치 1.0/0.0"""
        mock_hybrid.return_value = self._mock_hybrid_result(
            final_score=20.0, risk_level="SAFE",
            rule_score=0.2, ml_score=None,
            weights={"rule": 1.0, "ml": 0.0}
        )

        from app.services.predict_service import predict_risk
        result = predict_risk("인천광역시 부평구 삼산동 100", 15000)

        assert result["scoring_detail"]["ml_score"] is None
        assert result["scoring_detail"]["weights"]["rule"] == 1.0
        assert result["scoring_detail"]["weights"]["ml"] == 0.0
        assert result["risk_level"] == "SAFE"


# =============================================================================
# 실행
# =============================================================================
if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])