package hanshin.home_risk_check.safetyscore.domain.accident.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "traffic_accidents")
public class TrafficAccident {

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;

    private String rawAddress; //CSV 원본 주소
    private String standardAddress; // API로 정제된 표준 주소
    private String admCd; // 행정동 코드

    // 사고 건수
    private Integer accidentCount;  //사고 건수
    private Integer deathCount; // 사망자 수

    @Column(nullable = false)
    private Point geometry;


    @Builder
    public TrafficAccident(String rawAddress, String standardAddress, String admCd,
                           Integer accidentCount, Integer deathCount, Point geometry){
        this.rawAddress = rawAddress;
        this.standardAddress = standardAddress;
        this.admCd = admCd;
        this.accidentCount = accidentCount;
        this.deathCount = deathCount;
        this.geometry = geometry;
    }
}
