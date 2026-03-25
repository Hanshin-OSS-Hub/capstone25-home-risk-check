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

    //admCode(String)으로 지역 정보 조회
    Optional<Region> findByAdmCode(String admCode);


    @Query("SELECT r.admCode FROM Region r")
    Set<String> findAllAdmCodes();

    // DB에 점수 없는지 조회용 ( 기초 데이터 넣을때 사용)
    boolean existsBySafetyScoreIsNull();

}
