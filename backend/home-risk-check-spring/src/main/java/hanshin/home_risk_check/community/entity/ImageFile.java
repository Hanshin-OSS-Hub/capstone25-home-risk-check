package hanshin.home_risk_check.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "image_file",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_storage_key", columnNames = {"storage_key"})}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Builder
    public ImageFile(String originalName, String storageKey, String contentType) {
        this.originalName = originalName;
        this.storageKey = storageKey;
        this.contentType = contentType;
    }
}