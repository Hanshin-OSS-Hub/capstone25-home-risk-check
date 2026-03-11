package hanshin.home_risk_check.safetyscore.domain.accident.repository;

import hanshin.home_risk_check.safetyscore.domain.accident.entity.TrafficAccident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface TrafficRepository extends JpaRepository<TrafficAccident, Long> {

    @Query("SELECT t.rawAddress FROM TrafficAccident t")
    Set<String> findAllRawAddresses();

    // 행정동 코드가 일치하는 교통사고 건수 합산
    @Query("SELECT SUM(t.accidentCount) FROM TrafficAccident t WHERE t.admCd = :admCode")
    Integer sumAccidentCountByAdmCode(@Param("admCode") String admCode);
}
