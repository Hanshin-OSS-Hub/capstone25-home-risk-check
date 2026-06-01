package hanshin.home_risk_check.riskanalysis.infra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
 * FastAPI 공통 응답 래퍼 { meta, data }.
 * meta는 사용하지 않으므로 data만 추출. 미정의 필드는 무시.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiEnvelope<T>(T data) {}
