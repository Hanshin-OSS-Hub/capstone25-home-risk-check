package hanshin.home_risk_check.riskanalysis.infra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import hanshin.home_risk_check.riskanalysis.dto.RiskAnalysisResult;
import hanshin.home_risk_check.riskanalysis.dto.TaskStatus;

/*
 * FastAPI GET /predict/{task_id} data.
 * - COMPLETED: result 채워짐
 * - FAILED: error 채워짐
 * - PENDING/PROCESSING: progress 만
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PredictPollData(
    String taskId,
    TaskStatus status,
    Integer progress,
    RiskAnalysisResult result,
    String error
) {}