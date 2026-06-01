package hanshin.home_risk_check.riskanalysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * HUG 전세보증금 반환보증 심사 결과.
 * FastAPI hug_result 와 매핑. reason은 가입 불가일 때만 존재.
 */
public record HugResult(
    @JsonProperty("is_eligible")
    boolean eligible,

    long safeLimit,
    double coverageRatio,
    String message,
    String reason
) {}