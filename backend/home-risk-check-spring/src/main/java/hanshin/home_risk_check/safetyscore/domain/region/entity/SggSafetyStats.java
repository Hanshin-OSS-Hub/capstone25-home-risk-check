package hanshin.home_risk_check.safetyscore.domain.region.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "sgg_safety_stats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sido_sgg",
                        columnNames = {"sido_nm", "sgg_nm"}
                )
    }
)
public class SggSafetyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sido_nm", nullable = false)
    private String sidoNm;

    @Column(name = "sgg_nm", nullable = false)
    private String sggNm;

    @Column(name = "sido_code", nullable = false)
    private String sidoCode;

    @Column(name = "sgg_code", nullable = false)
    private String sggCode;

    @Column(name = "adm_code", length = 5)
    private String admCode; //SGIS 행정코드(5자리)

    // 사고 통계 정보 (API 콜러가 나중에 채워줌)
    @Column(name = "acc_cnt")
    private Integer accCnt; // 단순 사고 건수

    @Column(name = "dth_dnv_cnt")
    private Integer dthDnvCnt; // 사망자 수

    @Column(name = "pop_100k")
    private Double pop100k; // 인구 10만 명당 사고 건수

    // 범죄 통계 정보
    @Column(name = "robbery_cnt")
    private Integer robberyCnt; // 강도

    @Column(name = "theft_cnt")
    private Integer theftCnt;   // 절도

    @Column(name = "murder_cnt")
    private Integer murderCnt;  // 살인

    @Column(name = "sexual_crime_cnt")
    private Integer sexualCrimeCnt; // 성범죄

    @Column(name = "violence_cnt")
    private Integer violenceCnt; // 폭력

    @Builder
    public SggSafetyStats(String sidoNm, String sggNm, String sidoCode, String sggCode) {
        this.sidoNm = sidoNm;
        this.sggNm = sggNm;
        this.sidoCode = sidoCode;
        this.sggCode = sggCode;
    }

    // API를 통해 가져온 통계 데이터를 업데이트하는 메서드
    public void updateStatistics(Integer accCnt, Integer dthDnvCnt, Double pop100k) {
        this.accCnt = accCnt;
        this.dthDnvCnt = dthDnvCnt;
        this.pop100k = pop100k;
    }

    // 범죄 데이터를 업데이트하는 메서드
    public void updateCrimeStats(Integer robberyCnt, Integer theftCnt, Integer murderCnt, Integer sexualCrimeCnt, Integer violenceCnt) {
        this.robberyCnt = robberyCnt;
        this.theftCnt = theftCnt;
        this.murderCnt = murderCnt;
        this.sexualCrimeCnt = sexualCrimeCnt;
        this.violenceCnt = violenceCnt;
    }

    public void updateAdmCode(String admCode) {
        this.admCode = admCode;
    }
}
