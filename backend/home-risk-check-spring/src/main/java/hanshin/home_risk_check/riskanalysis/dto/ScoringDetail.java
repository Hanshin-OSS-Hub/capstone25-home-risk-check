package hanshin.home_risk_check.riskanalysis.dto;

import java.util.Map;

/*
 * 점수 산출 내역 (룰 베이스 + ML 하이브리드).
 * FastAPI scoring_detail 과 매핑.
 */
public record ScoringDetail(
    Integer ruleScore,
    Double mlScore,
    Map<String, Object> weights
) {}