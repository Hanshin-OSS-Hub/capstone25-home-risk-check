package hanshin.home_risk_check.safetyscore.domain.fire.repository;

import hanshin.home_risk_check.safetyscore.domain.fire.entity.FireStation;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FireStationRepository extends JpaRepository<FireStation, Long> {

    //특정 행정동 구역내 시설의 개수
    @Query("SELECT COUNT(f) FROM FireStation f WHERE ST_Contains(:regionGeom, f.geometry) = true")
    Integer countFireStationsInRegion(@Param("regionGeom") MultiPolygon regionGeom);
}
