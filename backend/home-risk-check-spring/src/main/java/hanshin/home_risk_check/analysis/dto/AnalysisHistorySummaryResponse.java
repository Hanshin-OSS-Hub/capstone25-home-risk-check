package hanshin.home_risk_check.analysis.dto;

import lombok.Builder;
import java.time.LocalDateTime;

/*
 * 마이페이지 위험도 분석 이력 목록 항목 (요약).
 */
@Builder
public record AnalysisHistorySummaryResponse(
    Long id,
    String address,
    long deposit,
    int riskScore,
    String riskLevel,
    LocalDateTime createdAt
) {}