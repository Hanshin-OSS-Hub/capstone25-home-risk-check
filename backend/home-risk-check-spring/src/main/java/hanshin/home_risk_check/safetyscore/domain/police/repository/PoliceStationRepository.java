package hanshin.home_risk_check.safetyscore.domain.police.repository;

import hanshin.home_risk_check.safetyscore.domain.police.entity.PoliceStation;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface PoliceStationRepository extends JpaRepository<PoliceStation, Long> {

    @Query("SELECT p.name FROM PoliceStation p")
    Set<String> findAllNames();

    //특정 행정동 구역내 시설의 개수
    @Query("SELECT COUNT(p) FROM PoliceStation p WHERE ST_Contains(:regionGeom, p.geometry) = true")
    Integer countPoliceStationsInRegion(@Param("regionGeom") MultiPolygon regionGeom);
}
