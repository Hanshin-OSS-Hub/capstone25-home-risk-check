package hanshin.home_risk_check.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * 댓글 Entity
 * DB의 comment 테이블과 매핑되는 클래스
 */
@Entity  // JPA가 이 클래스를 DB 테이블과 연결된 엔티티로 인식
@Table(name = "comment") // 실제 DB 테이블 이름
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// JPA는 기본 생성자가 필요함
// 외부에서 무분별하게 생성하지 못하도록 protected로 제한

public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // DB에서 AUTO_INCREMENT 방식으로 PK 생성
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "post_id", nullable = false)
    // 어떤 게시글에 달린 댓글인지
    private Long postId;

    @Column(name = "author_id", nullable = false)
    // 댓글 작성자 ID
    private Long authorId;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    // 댓글 내용 (길이 제한을 크게 두기 위해 TEXT 사용)
    private String content;

    @Column(name = "parent_comment_id")
    // 부모 댓글 ID
    // 일반 댓글이면 null
    // 대댓글이면 부모 댓글 ID 저장
    private Long parentCommentId;

    @Column(name = "root_comment_id")
    // 댓글 트리의 최상위 댓글 ID
    // depth 1 구조에서 댓글 묶음을 정렬할 때 사용
    private Long rootCommentId;

    @Column(name = "depth", nullable = false)
    // 댓글 깊이
    // 0 = 일반 댓글
    // 1 = 대댓글
    private Integer depth;

    @Column(name = "created_at", nullable = false)
    // 댓글 생성 시간
    private LocalDateTime createdAt;

    /*
     * Builder 패턴
     * 댓글 생성 시 사용하는 생성자
     */
    @Builder
    public Comment(Long postId, Long authorId, String content,
                   Long parentCommentId, Long rootCommentId, Integer depth) {
        this.postId = postId;
        this.authorId = authorId;
        this.content = content;
        this.parentCommentId = parentCommentId;
        this.rootCommentId = rootCommentId;
        this.depth = depth;
    }

    /*
     * DB INSERT 전에 자동 실행
     * 댓글 생성 시간 기록
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /*
     * root 댓글 ID 설정
     * 댓글 저장 후 rootCommentId를 자기 자신 ID로 설정할 때 사용
     */
    public void setRootCommentId(Long rootCommentId) {
        this.rootCommentId = rootCommentId;
    }
}