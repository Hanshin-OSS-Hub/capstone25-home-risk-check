package hanshin.home_risk_check.safetyscore.domain.fire.repository;

import hanshin.home_risk_check.safetyscore.domain.fire.entity.FireStation;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FireStationRepository extends JpaRepository<FireStation, Long> {

    // 단건 조회용
    @Query(value = """
    SELECT COUNT(f.id)
    FROM fire_stations f
    JOIN safety_regions r
      ON MBRContains(r.geometry, f.geometry)
     AND ST_Contains(r.geometry, f.geometry)
    WHERE r.adm_code = :admCode
    """, nativeQuery = true)
    Integer countFireStationsInRegion(@Param("admCode") String admCode);

    // 전체 조회용
    @Query("SELECT f.admCode, COUNT(f) " +
       "FROM FireStation f " +
       "WHERE f.admCode IS NOT NULL AND f.admCode != '' " +
       "GROUP BY f.admCode")
    List<Object[]> countAllGroupedByAdmCode();

}
