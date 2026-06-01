package hanshin.home_risk_check.riskanalysis.repository;

import hanshin.home_risk_check.riskanalysis.entity.RiskAnalysisHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAnalysisHistoryRepository extends JpaRepository<RiskAnalysisHistory, Long> {
    boolean existsByTaskId(String taskId);
}