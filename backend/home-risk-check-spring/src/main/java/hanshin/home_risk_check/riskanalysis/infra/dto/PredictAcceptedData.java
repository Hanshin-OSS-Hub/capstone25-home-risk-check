package hanshin.home_risk_check.riskanalysis.infra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import hanshin.home_risk_check.riskanalysis.dto.TaskStatus;

/*
 * FastAPI POST /predict 접수(202) data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PredictAcceptedData(
    String taskId,
    TaskStatus status,
    String cacheKey,
    String pollUrl,
    Integer estimatedSeconds
) {}