package hanshin.home_risk_check.safetyscore.domain.region.repository;

import hanshin.home_risk_check.safetyscore.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RegionRepository extends JpaRepository<Region, Long> {

    //sgisCode(String)으로 지역 정보 조회
    Optional<Region> findBySgisCode(String sgisCode);

    @Query("SELECT r.sgisCode FROM Region r")
    Set<String> findAllSgisCode();

    // DB에 점수 없는지 조회용 ( 기초 데이터 넣을때 사용)
    boolean existsBySafetyScoreIsNull();

    //  특정 행정동(sgisCode)의 면적  계산
    @Query(value = "SELECT ST_Area(ST_Transform(geometry, 3857)) " +
            "FROM safety_regions " +
            "WHERE sgis_code = :sgisCode",
            nativeQuery = true)
    Double getAreaBySgisCode(@Param("sgisCode") String sgisCode);

    // 카카오에서 주는 행정동 코드와 sgis 코드가 다르므로 카카오에서 주는 좌표로 속한 행정동 찾아옴
    @Query(value = "SELECT * FROM safety_regions " +
            "WHERE ST_Contains(geometry, ST_GeomFromText(CONCAT('POINT(', :lon, ' ', :lat, ')'), 4326, 'axis-order=long-lat')) " +
            "LIMIT 1", nativeQuery = true)
    Optional<Region> findByLocation(@Param("lon") double lon, @Param("lat") double lat);

}
