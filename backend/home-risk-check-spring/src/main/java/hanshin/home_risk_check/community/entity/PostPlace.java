package hanshin.home_risk_check.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "post_place",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_place_post", columnNames = {"post_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "address", nullable = false, length = 100)
    private String address;

    @Column(name = "place_name", length = 100)
    private String placeName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Builder
    public PostPlace(double latitude, double longitude, String address, String placeName, Post post) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.placeName = placeName;
        this.post = post;
    }

    public void update(double latitude, double longitude, String address, String placeName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.placeName = placeName;
    }
}