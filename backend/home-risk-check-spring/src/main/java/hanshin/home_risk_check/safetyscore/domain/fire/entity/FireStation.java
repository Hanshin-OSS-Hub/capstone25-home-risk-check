package hanshin.home_risk_check.safetyscore.domain.fire.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fire_stations")
public class FireStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;

    @Column(name = "adm_code")
    private String admCode;

    @Column(nullable = false)
    private Point geometry;

    @Builder
    public FireStation(String name, String address, Point geometry){
        this.name = name;
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
