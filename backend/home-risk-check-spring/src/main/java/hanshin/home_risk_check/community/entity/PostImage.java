package hanshin.home_risk_check.community.entity;

import hanshin.home_risk_check.file.entity.ImageFile;
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
                @UniqueConstraint(name = "uk_image", columnNames = {"image_file_id"})
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_file_id", nullable = false)
    private ImageFile imageFile;

    @Builder
    public PostImage(Post post, ImageFile imageFile) {
        this.post = post;
        this.imageFile = imageFile;
    }
}