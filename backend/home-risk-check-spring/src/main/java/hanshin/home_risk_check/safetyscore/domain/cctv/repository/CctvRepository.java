package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.cctv.repository;


import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.cctv.entity.Cctv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface CctvRepository extends JpaRepository<Cctv, Long> {

    @Query("SELECT c.manageNo FROM Cctv c")
    Set<String> findAllManageNos();
}
