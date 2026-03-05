package hanshin.home_risk_check.safetyscore.domain.cctv.repository;

import hanshin.home_risk_check.safetyscore.domain.cctv.entity.Cctv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface CctvRepository extends JpaRepository<Cctv, Long> {

    @Query("SELECT c.manageNo FROM Cctv c")
    Set<String> findAllManageNos();
}
