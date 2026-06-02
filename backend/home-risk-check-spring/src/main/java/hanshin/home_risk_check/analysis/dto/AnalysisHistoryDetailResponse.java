package hanshin.home_risk_check.analysis.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record AnalysisHistoryDetailResponse(
    Long id,
    LocalDateTime createdAt,
    JeonseFraudResult jeonseFraud,
    SafetyScoreSnapshot safetyScore
) {}