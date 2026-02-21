package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.cctv.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cctvs")
public  class Cctv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String manageNo;

    private String address;

    private String purpose;

    private Integer cameraCount;

    @Column(nullable = false)
    private Point geometry;

    @Builder
    public Cctv(String manageNo, String address, String purpose, Integer cameraCount, Point geometry){
        this.manageNo = manageNo;
        this.address = address;
        this.purpose = purpose;
        this.cameraCount = cameraCount;
        this.geometry = geometry;
    }

    //위도 조회
    public Double getLatitude() {
        return this.geometry != null ? this.geometry.getY() : null;
    }

    //경도 조회
    public Double getLongitude() {
        return this.geometry != null ? this.geometry.getX() : null;
    }
}

