package hanshin.home_risk_check.riskanalysis.dto;

/*
 * 위험 요인 한 건.
 * FastAPI major_risk_factors[] 의 각 항목과 매핑.
 */
public record RiskFactor(
    String type,
    String severity,
    String message
) {}