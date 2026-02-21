package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.police.repository;


import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.police.entity.PoliceStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface PoliceStationRepository extends JpaRepository<PoliceStation, Long> {

    @Query("SELECT p.name FROM PoliceStation p")
    Set<String> findAllNames();
}
