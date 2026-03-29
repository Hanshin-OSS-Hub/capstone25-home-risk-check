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

    // 교통사고 다발 지역 포함 여부 확인용
    @Query(value = "SELECT COUNT(*) FROM traffic_accidents " +
            "WHERE ST_Distance_Sphere(geometry, " +
            "ST_GeomFromText(CONCAT('POINT(', :lon, ' ', :lat, ')'), 4326, 'axis-order=long-lat')) <= :radius",
            nativeQuery = true)
    Integer countAccidentAreaWithinRadius(@Param("lat") double lat,
                                          @Param("lon") double lon,
                                          @Param("radius") double radius);
}
