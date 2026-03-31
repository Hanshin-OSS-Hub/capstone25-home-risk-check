package hanshin.home_risk_check.community.entity;

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
 * DB의 post 테이블과 매핑되는 클래스
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
     * 작성자 ID
     *
     * 아직 User 엔티티와 연관관계를 맺지 않았으므로
     * Long 값으로만 저장한다.
     */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /*
     * 게시글 카테고리
     * 예: "서울시 성동구"
     */
    @Column(name = "category_label", nullable = false, length = 50)
    private String categoryLabel;

    /*
     * 게시글 제목
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /*
     * 게시글 본문
     *
     * 글이 길 수 있으므로 MEDIUMTEXT 사용
     */
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    /*
     * 생성 시간
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /*
     * 수정 시간
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /*
     * 게시글 1개 -> 댓글 여러 개
     *
     * mappedBy = "post"
     * : Comment 엔티티의 post 필드가 연관관계의 주인임을 의미
     *
     * cascade = ALL
     * : 게시글 삭제 시 연결된 댓글들도 같이 삭제
     *
     * orphanRemoval = true
     * : 부모 컬렉션에서 제거된 자식 엔티티는 DB에서도 제거
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @Builder
    public Post(Long authorId, String categoryLabel, String title, String content) {
        this.authorId = authorId;
        this.categoryLabel = categoryLabel;
        this.title = title;
        this.content = content;
    }

    /*
     * 저장 직전 시간 자동 설정
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /*
     * 수정 직전 updatedAt 갱신
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /*
     * 게시글 수정 메서드
     *
     * 서비스 계층에서 엔티티 값을 직접 바꾸는 대신
     * 엔티티 내부 메서드로 변경하는 방식
     */
    public void update(String categoryLabel, String title, String content) {
        this.categoryLabel = categoryLabel;
        this.title = title;
        this.content = content;
    }
}