package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.region.repository;

import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface RegionRepository extends JpaRepository<Region, Long> {

    boolean existsByAdmCode(String admCode);

    @Query("SELECT r.admCode FROM Region r")
    Set<String> findAllAdmCodes();
}
