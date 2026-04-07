package hanshin.home_risk_check.safetyscore.domain.police.repository;

import hanshin.home_risk_check.safetyscore.domain.police.entity.PoliceStation;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface PoliceStationRepository extends JpaRepository<PoliceStation, Long> {

    @Query("SELECT p.name FROM PoliceStation p")
    Set<String> findAllNames();

    // 행정동 내 경찰서 개수 조회
    @Query("SELECT COUNT(p) FROM PoliceStation p WHERE p.sgisCode = :sgisCode")
    Integer countPoliceStationsInRegion(@Param("sgisCode") String sgisCode);

    // 주소 기반 주변 경찰서 개수 조회
    @Query(value = "SELECT COUNT(*) FROM police_stations " +
            "WHERE ST_Distance_Sphere(geometry, " +
            "ST_GeomFromText(CONCAT('POINT(', :lon, ' ', :lat, ')'), 4326, 'axis-order=long-lat')) <= :radius",
            nativeQuery = true)
    Integer countPoliceWithinRadius(@Param("lat") double lat,
                                    @Param("lon") double lon,
                                    @Param("radius") double radius);

    // 전체 조회용
    @Query("SELECT p.sgisCode, COUNT(p) " +
       "FROM PoliceStation p " +
       "WHERE p.sgisCode IS NOT NULL AND p.sgisCode != '' " +
       "GROUP BY p.sgisCode")
    List<Object[]> countAllGroupedBySgisCode();

}
