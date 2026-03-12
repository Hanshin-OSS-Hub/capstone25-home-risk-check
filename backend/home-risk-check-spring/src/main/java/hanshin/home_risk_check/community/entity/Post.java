package hanshin.home_risk_check.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * 게시글 Entity
 * DB의 post 테이블과 매핑되는 클래스
 */
@Entity  // JPA가 이 클래스를 DB 테이블과 매핑되는 엔티티로 인식
@Table(name = "post") // 실제 DB 테이블 이름
@Getter // Lombok: 모든 필드의 getter 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// JPA는 기본 생성자가 필요함. 외부에서 무분별하게 new 못 하도록 protected 설정

public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // DB에서 AUTO_INCREMENT로 PK 생성
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "author_id", nullable = false)
    // 작성자 ID (User 엔티티와 연관관계 대신 Long 값으로 저장)
    private Long authorId;

    @Column(name = "category_label", nullable = false, length = 50)
    // 게시글 카테고리 (예: "서울시 성동구")
    private String categoryLabel;

    @Column(name = "title", nullable = false, length = 200)
    // 게시글 제목
    private String title;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    // 게시글 본문
    // 글이 길어질 수 있으므로 MEDIUMTEXT 사용
    private String content;

    @Column(name = "created_at", nullable = false)
    // 게시글 생성 시간
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    // 게시글 수정 시간
    private LocalDateTime updatedAt;

    /*
     * Builder 패턴
     * 게시글 생성 시 사용하는 생성자
     */
    @Builder
    public Post(Long authorId, String categoryLabel, String title, String content) {
        this.authorId = authorId;
        this.categoryLabel = categoryLabel;
        this.title = title;
        this.content = content;
    }

    /*
     * DB INSERT 전에 자동 실행
     * createdAt, updatedAt을 현재 시간으로 설정
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /*
     * DB UPDATE 전에 자동 실행
     * 수정 시간 갱신
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /*
     * 게시글 수정 메서드
     * 서비스 계층에서 게시글 수정 시 사용
     */
    public void update(String categoryLabel, String title, String content) {
        this.categoryLabel = categoryLabel;
        this.title = title;
        this.content = content;
    }
}