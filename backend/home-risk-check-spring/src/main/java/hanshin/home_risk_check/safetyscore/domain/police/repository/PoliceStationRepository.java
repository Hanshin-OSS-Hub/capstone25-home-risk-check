package hanshin.home_risk_check.safetyscore.domain.police.repository;

import hanshin.home_risk_check.safetyscore.domain.police.entity.PoliceStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface PoliceStationRepository extends JpaRepository<PoliceStation, Long> {

    @Query("SELECT p.name FROM PoliceStation p")
    Set<String> findAllNames();
}
