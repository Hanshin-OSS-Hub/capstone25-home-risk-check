package hanshin.home_risk_check.riskanalysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import hanshin.home_risk_check.safetyscore.domain.score.dto.SafetyScoreResponse;
import lombok.Builder;

/*
 * 클라이언트에 내려주는 위험도 분석 상태/결과 응답.
 *
 * - PENDING/PROCESSING: status, progress 만 채워짐
 * - COMPLETED: riskAnalysis + safetyScore
 * - FAILED: error 채워짐
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RiskAnalysisStatusResponse(
    String taskId,
    TaskStatus status,
    Integer progress,
    RiskAnalysisResult riskAnalysis,
    SafetyScoreResponse.Data safetyScore,
    String error
) {}