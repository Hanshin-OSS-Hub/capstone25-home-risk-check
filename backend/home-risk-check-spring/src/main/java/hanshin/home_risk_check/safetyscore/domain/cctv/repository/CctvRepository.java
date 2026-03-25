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

    // 단건 조회용
    @Query("SELECT COALESCE(SUM(c.cameraCount), 0) FROM Cctv c WHERE c.admCode = :admCode")
    Integer sumCameraCountInRegion(@Param("admCode") String admCode);

    // 행정동별로 설치된 카메라의 개수를 합산
    @Query("SELECT c.admCode, SUM(c.cameraCount) " +
            "FROM Cctv c " +
            "WHERE c.admCode IS NOT NULL " +
            "GROUP BY c.admCode")
    List<Object[]> sumCameraCountGroupedByAdmCode();
}
