"""
DB 스키마 변경 검증 테스트

테스트 목표:
1. save_prediction_result가 building_info_id 없이 정상 호출되는지
2. INSERT SQL에 building_info_id가 포함되지 않는지
3. 조회 시 analyzed_at 기준 정렬이 사용되는지
"""
import pytest
from unittest.mock import patch, MagicMock, call


# =============================================================================
# save_prediction_result 시그니처 변경 검증
# =============================================================================
class TestSavePredictionResultSchema:
    """building_info_id 제거 후 save_prediction_result 동작 검증"""

    def test_no_building_info_id_in_signature(self):
        """함수 시그니처에 building_info_id가 없는지 확인"""
        import inspect
        from app.services.result_service import save_prediction_result

        sig = inspect.signature(save_prediction_result)
        param_names = list(sig.parameters.keys())

        assert "building_info_id" not in param_names, \
            "building_info_id가 시그니처에서 제거되지 않았습니다"

    def test_required_params_exist(self):
        """필수 파라미터가 모두 존재하는지 확인"""
        import inspect
        from app.services.result_service import save_prediction_result

        sig = inspect.signature(save_prediction_result)
        param_names = list(sig.parameters.keys())

        expected = [
            "pnu", "deposit_manwon", "market_price_manwon",
            "features", "risk_level", "risk_score", "ai_prob"
        ]
        for param in expected:
            assert param in param_names, f"필수 파라미터 '{param}'이 누락되었습니다"

    @patch('app.services.result_service.get_engine')
    @patch('app.services.result_service.create_address_key', return_value="11680-11200-0600-0000")
    def test_insert_sql_excludes_building_info_id(self, mock_addr, mock_engine):
        """INSERT SQL에 building_info_id가 포함되지 않는지 확인"""
        from app.services.result_service import save_prediction_result

        # Mock DB 연결
        mock_conn = MagicMock()
        mock_engine.return_value.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.return_value.connect.return_value.__exit__ = MagicMock(return_value=False)
        mock_conn.begin.return_value.__enter__ = MagicMock()
        mock_conn.begin.return_value.__exit__ = MagicMock(return_value=False)

        save_prediction_result(
            pnu="1168011200",
            deposit_manwon=30000,
            market_price_manwon=50000,
            features={
                "jeonse_ratio": 0.6,
                "hug_risk_ratio": 0.8,
                "total_risk_ratio": 0.7,
                "real_debt_manwon": 0
            },
            risk_level="CAUTION",
            risk_score=65,
            ai_prob=0.7
        )

        # execute 호출된 SQL 문자열에서 building_info_id 확인
        execute_calls = mock_conn.execute.call_args_list
        for c in execute_calls:
            sql_text = str(c[0][0])
            if "INSERT" in sql_text:
                assert "building_info_id" not in sql_text, \
                    "INSERT SQL에 building_info_id가 여전히 포함되어 있습니다"


# =============================================================================
# get_previous_analysis 컬럼명 검증
# =============================================================================
class TestGetPreviousAnalysisSchema:
    """analyzed_at 컬럼명 통일 검증"""

    @patch('app.services.result_service.get_engine')
    def test_query_uses_analyzed_at(self, mock_engine):
        """조회 쿼리가 analyzed_at을 사용하는지 확인"""
        from app.services.result_service import get_previous_analysis

        mock_conn = MagicMock()
        mock_engine.return_value.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.return_value.connect.return_value.__exit__ = MagicMock(return_value=False)
        mock_conn.execute.return_value.mappings.return_value.fetchone.return_value = None

        get_previous_analysis("11680-11200-0600-0000")

        sql_text = str(mock_conn.execute.call_args[0][0])
        assert "analyzed_at" in sql_text, \
            "조회 쿼리에 analyzed_at이 사용되지 않았습니다"
        assert "created_at" not in sql_text, \
            "조회 쿼리에 created_at이 여전히 사용되고 있습니다"


# =============================================================================
# 실행
# =============================================================================
if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])