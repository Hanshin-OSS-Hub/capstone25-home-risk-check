package hanshin.home_risk_check.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "post_image",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_image", columnNames = {"image_file_id"}),
                @UniqueConstraint(name = "uk_post_order", columnNames = {"post_id", "image_order"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "image_file_id", nullable = false)
    private ImageFile imageFile;

    @Column(name = "image_order", nullable = false)
    private int imageOrder;

    @Builder
    public PostImage(Post post, ImageFile imageFile, int imageOrder) {
        this.post = post;
        this.imageFile = imageFile;
        this.imageOrder = imageOrder;
    }
}