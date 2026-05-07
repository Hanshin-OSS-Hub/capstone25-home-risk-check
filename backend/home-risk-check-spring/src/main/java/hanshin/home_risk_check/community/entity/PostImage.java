package hanshin.home_risk_check.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 게시글 이미지 Entity
 *
 * 파일은 로컬에 저장하고,
 * DB에는 파일 메타데이터만 저장한다.
 */
@Entity
@Table(name = "post_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_image_id")
    private Long postImageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /*
     * 사용자가 업로드한 원본 파일명
     */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /*
     * 서버에 저장되는 파일명
     */
    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    /*
     * 파일 확장자
     */
    @Column(name = "extension", nullable = false, length = 20)
    private String extension;

    /*
     * 파일 크기(byte)
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /*
     * 파일 접근 경로
     * 예: /uploads/community/posts/1/uuid.png
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /*
     * 업로드 순서
     */
    @Column(name = "image_order", nullable = false)
    private Integer imageOrder;

    @Builder
    public PostImage(Post post,
                     String originalName,
                     String storedName,
                     String extension,
                     Long fileSize,
                     String filePath,
                     Integer imageOrder) {
        this.post = post;
        this.originalName = originalName;
        this.storedName = storedName;
        this.extension = extension;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.imageOrder = imageOrder;
    }
}