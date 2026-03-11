package hanshin.home_risk_check.safetyscore.domain.cctv.repository;

import hanshin.home_risk_check.safetyscore.domain.cctv.entity.Cctv;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface CctvRepository extends JpaRepository<Cctv, Long> {

    @Query("SELECT c.manageNo FROM Cctv c")
    Set<String> findAllManageNos();

    // 특정 행정동 폴리곤 내부에 있는 CCTV 카메라 대수의 총합을 구함
    @Query("SELECT SUM(c.cameraCount) FROM Cctv c WHERE ST_Contains(:regionGeom, c.geometry) = true")
    Integer sumCameraCountInRegion(@Param("regionGeom") MultiPolygon regionGeom);
}
