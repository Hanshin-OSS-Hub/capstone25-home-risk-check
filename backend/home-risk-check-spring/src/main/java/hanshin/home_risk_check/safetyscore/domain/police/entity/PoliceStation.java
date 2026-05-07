package hanshin.home_risk_check.safetyscore.domain.police.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "police_stations")
public class PoliceStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 관사명

    private String type; // 구분(지구대 / 파출소 / 경찰서)

    private String address; // 주소

    @Column(name = "sgisCode")
    private String sgisCode;

    @Column(nullable = false)
    private Point geometry;


    @Builder
    public PoliceStation(String name, String type, String address, Point geometry){
        this.name = name;
        this.type = type;
        this.address = address;
        this.geometry = geometry;
    }

    //위도
    public Double getLatitude() {
        return this.geometry != null ? this.geometry.getY() : null;
    }

    //경도
    public Double getLongitude() {
        return this.geometry != null ? this.geometry.getX() : null;
    }
}
