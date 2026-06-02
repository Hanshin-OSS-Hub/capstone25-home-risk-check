package hanshin.home_risk_check.analysis.mapper;

import hanshin.home_risk_check.analysis.dto.AnalysisHistoryDetailResponse;
import hanshin.home_risk_check.analysis.dto.AnalysisHistorySummaryResponse;
import hanshin.home_risk_check.analysis.dto.AnalysisSnapshot;
import hanshin.home_risk_check.analysis.entity.AnalysisHistory;
import org.springframework.stereotype.Component;

@Component
public class AnalysisHistoryMapper {

    /* 목록 항목 (요약) — 요약 필드는 스냅샷의 위험도 결과에서 추출 */
    public AnalysisHistorySummaryResponse toHistoryResponse(AnalysisHistory history, AnalysisSnapshot snapshot) {
        var jeonseFraud = snapshot.jeonseFraud();
        return AnalysisHistorySummaryResponse.builder()
                                             .id(history.getId())
                                             .address(jeonseFraud.address())
                                             .deposit(jeonseFraud.deposit())
                                             .riskScore(jeonseFraud.riskScore())
                                             .riskLevel(jeonseFraud.riskLevel())
                                             .createdAt(history.getCreatedAt())
                                             .build();
    }

    /* 상세 (스냅샷 복원본 합본) — snapshot은 Service에서 resultJson을 역직렬화해 전달 */
    public AnalysisHistoryDetailResponse toDetailResponse(AnalysisHistory history, AnalysisSnapshot snapshot) {
        return AnalysisHistoryDetailResponse.builder()
                                            .id(history.getId())
                                            .createdAt(history.getCreatedAt())
                                            .jeonseFraud(snapshot.jeonseFraud())
                                            .safetyScore(snapshot.safetyScore())
                                            .build();
    }
}