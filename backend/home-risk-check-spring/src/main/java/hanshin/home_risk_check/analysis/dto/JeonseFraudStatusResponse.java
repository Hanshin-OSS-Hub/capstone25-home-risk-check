package hanshin.home_risk_check.analysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/*
 * 클라이언트에 내려주는 위험도 분석 상태/결과 응답.
 *
 * - PENDING/PROCESSING: status, progress 만 채워짐
 * - COMPLETED: jeonseFraud + safetyScore
 * - FAILED: error 채워짐
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JeonseFraudStatusResponse(
    String taskId,
    TaskStatus status,
    Integer progress,
    JeonseFraudResult jeonseFraud,
    SafetyScoreSnapshot safetyScore,
    String error
) {}