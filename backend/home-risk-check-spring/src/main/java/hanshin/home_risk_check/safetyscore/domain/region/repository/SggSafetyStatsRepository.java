package hanshin.home_risk_check.safetyscore.domain.region.repository;

import hanshin.home_risk_check.safetyscore.domain.region.entity.SggSafetyStats;
import org.aspectj.apache.bcel.classfile.Code;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SggSafetyStatsRepository extends JpaRepository<SggSafetyStats, Long> {

    //데이터 존재 여부 확인
    boolean existsBySidoNmAndSggNm(String sidoNm, String sggNm);

    Optional<SggSafetyStats> findBySidoNmAndSggNm(String sidoNm, String sggNm);

    //교통 사고 건수가 채워지지 않은 지역 여부 확인
    boolean existsByAccCntIsNull();
}
