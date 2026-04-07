package hanshin.home_risk_check.safetyscore.domain.cctv.repository;

import hanshin.home_risk_check.safetyscore.domain.cctv.entity.Cctv;
import io.micrometer.common.KeyValues;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface CctvRepository extends JpaRepository<Cctv, Long> {

    @Query("SELECT c.manageNo FROM Cctv c")
    Set<String> findAllManageNos();

    // 단건 조회용 (반경 radius 안에 있는 cctv의 개수 반환)
    @Query(value = "SELECT COALESCE(SUM(camera_count), 0) FROM cctvs " +
            "WHERE ST_Distance_Sphere(geometry, " +
            "ST_GeomFromText(CONCAT('POINT(', :lon, ' ', :lat, ')'), 4326, 'axis-order=long-lat')) <= :radius",
            nativeQuery = true)
    Integer sumCameraCountWithinRadius(@Param("lat") double lat,
                                       @Param("lon") double lon,
                                       @Param("radius") double radius);

    // 특정 행정동(sgisCode)의 전체 CCTV 개수 단건 조회
    @Query("SELECT COALESCE(SUM(c.cameraCount), 0) FROM Cctv c WHERE c.sgisCode = :sgisCode")
    Integer sumCameraCountBySgisCode(@Param("sgisCode") String sgisCode);

    // 행정동별로 설치된 카메라의 개수를 합산
    @Query("SELECT c.sgisCode, SUM(c.cameraCount) " +
            "FROM Cctv c " +
            "WHERE c.sgisCode IS NOT NULL " +
            "GROUP BY c.sgisCode")
    List<Object[]> sumCameraCountGroupedBySgisCode();
}
