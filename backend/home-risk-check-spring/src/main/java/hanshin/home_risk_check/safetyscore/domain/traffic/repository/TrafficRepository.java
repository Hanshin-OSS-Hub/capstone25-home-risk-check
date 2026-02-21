package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.traffic.repository;

import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.traffic.entity.TrafficAccident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface TrafficRepository extends JpaRepository<TrafficAccident, Long> {

    @Query("SELECT t.rawAddress FROM TrafficAccident t")
    Set<String> findAllRawAddresses();
}
