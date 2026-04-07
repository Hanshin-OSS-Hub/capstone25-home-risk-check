package hanshin.home_risk_check.safetyscore.domain.fire.repository;

import hanshin.home_risk_check.safetyscore.domain.fire.entity.FireStation;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FireStationRepository extends JpaRepository<FireStation, Long> {

    // 행정동 내 소방서 개수 조회
    @Query("SELECT COUNT(f) FROM FireStation f WHERE f.sgisCode = :sgisCode")
    Integer countFireStationsInRegion(@Param("sgisCode") String sgisCode);

    // 주소 근처 소방서 개수 조회
    @Query(value = "SELECT COUNT(*) FROM fire_stations " +
            "WHERE ST_Distance_Sphere(geometry, " +
            "ST_GeomFromText(CONCAT('POINT(', :lon, ' ', :lat, ')'), 4326, 'axis-order=long-lat')) <= :radius",
            nativeQuery = true)
    Integer countFireStationsWithinRadius(@Param("lat") double lat,
                                          @Param("lon") double lon,
                                          @Param("radius") double radius);

    // 전체 조회용
    @Query("SELECT f.sgisCode, COUNT(f) " +
       "FROM FireStation f " +
       "WHERE f.sgisCode IS NOT NULL AND f.sgisCode != '' " +
       "GROUP BY f.sgisCode")
    List<Object[]> countAllGroupedBySgisCode();

}
