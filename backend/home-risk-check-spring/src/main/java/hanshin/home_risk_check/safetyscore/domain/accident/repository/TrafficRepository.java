package hanshin.home_risk_check.safetyscore.domain.accident.repository;

import hanshin.home_risk_check.safetyscore.domain.accident.entity.TrafficAccident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface TrafficRepository extends JpaRepository<TrafficAccident, Long> {

    @Query("SELECT t.rawAddress FROM TrafficAccident t")
    Set<String> findAllRawAddresses();

    // 행정동코드(admCode)별로 교통사고 건수를 모두 합산해서 리스트로 한 번에 반환
    @Query("SELECT t.admCode, SUM(t.accidentCount) " +
            "FROM TrafficAccident t " +
            "WHERE t.admCode IS NOT NULL AND t.admCode != '' " +
            "GROUP BY t.admCode")
    List<Object[]> sumAccidentCountGroupedByAdmCode();
}
