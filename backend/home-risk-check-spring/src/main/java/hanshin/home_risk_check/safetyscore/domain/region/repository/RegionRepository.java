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

    // 인구수가 NULL인 데이터가 하나라도 존재하는지 확인
    boolean existsByPopulationIsNull();

    // 인구수가 NULL인 모든 지역 목록 가져오기
    List<Region> findAllByPopulationIsNull();

    @Query("SELECT r.admCode FROM Region r")
    Set<String> findAllAdmCodes();

    //sidoNm과 sggNm을 조건으로 매핑하여 해당하는 행정동들의 인구를 모두 합산
    @Query("SELECT SUM(r.population) FROM Region r WHERE r.sidoNm = :sidoNm AND r.sggNm = :sggNm AND r.population > 0")
    Integer sumPopulationBySidoNmAndSggNm(@Param("sidoNm") String sidoNm, @Param("sggNm") String sggNm);

    // 이름 기반으로 특정 구에 속한 동을 한번에 업데이트
    @Transactional
    @Modifying
    @Query("UPDATE Region r SET r.accidentScore = :score WHERE r.admNm LIKE :regionName%")
    int updateAccidentScoreByAdmNm(@Param("regionName") String regionName, @Param("score") Double score);
}
