package hanshin.home_risk_check.safetyscore.domain.accident.repository;

import hanshin.home_risk_check.safetyscore.domain.accident.entity.TrafficAccident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface TrafficRepository extends JpaRepository<TrafficAccident, Long> {

    @Query("SELECT t.rawAddress FROM TrafficAccident t")
    Set<String> findAllRawAddresses();
}
