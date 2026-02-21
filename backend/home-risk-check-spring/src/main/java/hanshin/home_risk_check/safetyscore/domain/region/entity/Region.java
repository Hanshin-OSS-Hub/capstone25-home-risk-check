package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.region.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "regions")
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "adm_code", unique = true)
    private String admCode; // 행정동 코드

    @Column(name = "adm_nm")
    private String admNm; //행정동 이름

    @Column(nullable = false)
    private MultiPolygon geometry;

    @Column(name = "population")
    private Integer population; //총 인구수 //null 처리를 위해 Integer 사용

    @Column(name = "population_stats_ym", length = 6)
    private String populationStatsYm; // 인구 통계 기준 연월 (예: "202312")

    @Builder
    public Region(String admCode, String admNm, MultiPolygon geometry){
        this.admCode = admCode;
        this.admNm = admNm;
        this.geometry = geometry;
    }

    public void updatePopulation(Integer population, String populationStatsYm) {
        this.population = population;
        this.populationStatsYm = populationStatsYm;
    }
}
