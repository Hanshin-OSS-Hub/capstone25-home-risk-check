package hanshin.home_risk_check.safetyscore.domain.region.repository;

import hanshin.home_risk_check.safetyscore.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

public interface RegionRepository extends JpaRepository<Region, Long> {

    boolean existsByAdmCode(String admCode);

    @Query("SELECT r.admCode FROM Region r")
    Set<String> findAllAdmCodes();

    // 이름 기반으로 특정 구에 속한 동을 한번에 업데이트
    @Transactional
    @Modifying
    @Query("UPDATE Region r SET r.accidentScore = :score WHERE r.admNm LIKE :regionName%")
    int updateAccidentScoreByAdmNm(@Param("regionName") String regionName, @Param("score") Double score);
}
