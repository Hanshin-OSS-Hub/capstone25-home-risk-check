package hanshin.home_risk_check.community.entity;

import hanshin.home_risk_check.user.entity.User; // [변경] 작성자를 User 엔티티와 연관관계로 매핑하기 위해 추가
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * 게시글 Entity
 */
@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    /*
     * [변경]
     * 기존 Long authorId 대신 User 엔티티와 FK 연관관계 매핑
     * post.author_id -> user.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User user;

    @Column(name = "category_label", nullable = false, length = 50)
    private String categoryLabel;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /*
     * 게시글 1개 -> 댓글 여러 개
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    /*
     * 게시글 1개 -> 이미지 여러 장
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("imageOrder ASC")
    private List<PostImage> images = new ArrayList<>();

    @Builder
    public Post(User user, String categoryLabel, String title, String content) { // [변경] Long authorId -> User user
        this.user = user; // [변경]
        this.categoryLabel = categoryLabel;
        this.title = title;
        this.content = content;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String categoryLabel, String title, String content) {
        this.categoryLabel = categoryLabel;
        this.title = title;
        this.content = content;
    }

    public void addImage(PostImage image) {
        this.images.add(image);
    }
}