package hanshin.home_risk_check.safetyscore.domain.sync.repository;

import hanshin.home_risk_check.safetyscore.domain.sync.entity.FileSyncHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileSyncHistoryRepository extends JpaRepository<FileSyncHistory, Long> {
    Optional<FileSyncHistory> findByFileName(String fileName);
}
