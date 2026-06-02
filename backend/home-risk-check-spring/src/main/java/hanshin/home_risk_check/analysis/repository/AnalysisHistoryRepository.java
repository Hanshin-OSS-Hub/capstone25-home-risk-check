package hanshin.home_risk_check.analysis.repository;

import hanshin.home_risk_check.analysis.entity.AnalysisHistory;
import hanshin.home_risk_check.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AnalysisHistoryRepository extends JpaRepository<AnalysisHistory, Long> {

    boolean existsByTaskId(String taskId);

    Slice<AnalysisHistory> findAllByUserOrderByIdDesc(User user, Pageable pageable);

    // 상세 조회 — 본인 이력만 접근 가능하도록 user 조건 포함
    Optional<AnalysisHistory> findByIdAndUser(Long id, User user);
}