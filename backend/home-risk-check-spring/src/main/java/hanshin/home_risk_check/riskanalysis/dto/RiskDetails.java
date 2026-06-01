package hanshin.home_risk_check.riskanalysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * 위험도 분석 세부 지표.
 * FastAPI details 와 매핑.
 */
public record RiskDetails(
    double jeonseRatio,
    long seniorDebt,

    @JsonProperty("is_illegal_building")
    boolean illegalBuilding,

    @JsonProperty("is_trust")
    boolean trust,

    double buildingAge,
    Integer ownershipDurationMonths
) {}