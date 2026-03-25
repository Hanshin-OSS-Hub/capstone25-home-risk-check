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

    // 단건 조회용
    @Query(value = """
    SELECT COUNT(p.id)
    FROM police_stations p
    JOIN safety_regions r
      ON MBRContains(r.geometry, p.geometry)
     AND ST_Contains(r.geometry, p.geometry)
    WHERE r.adm_code = :admCode
    """, nativeQuery = true)
    Integer countPoliceStationsInRegion(@Param("admCode") String admCode);

    // 전체 조회용
    @Query("SELECT p.admCode, COUNT(p) " +
       "FROM PoliceStation p " +
       "WHERE p.admCode IS NOT NULL AND p.admCode != '' " +
       "GROUP BY p.admCode")
    List<Object[]> countAllGroupedByAdmCode();

}
