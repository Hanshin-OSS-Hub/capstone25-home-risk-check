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
 * 댓글 Entity
 * DB의 comment 테이블과 매핑되는 클래스
 *
 * ERD 상으로는
 * - post_id
 * - parent_comment_id
 * - root_comment_id
 * 가 숫자(FK) 컬럼으로 존재하지만,
 * JPA에서는 이를 객체 연관관계로 매핑해서 더 편하게 사용한다.
 *
 * 단, 프론트로 응답할 때는 DTO에서 다시 ID만 꺼내서 내려준다.
 */
@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    /*
     * 댓글이 속한 게시글
     *
     * comment.post_id -> post.post_id
     *
     * LAZY:
     * 댓글만 조회할 때 게시글 전체를 즉시 다 끌고오지 않도록 설정
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /*
     * 댓글 작성자 ID
     *
     * 아직 User 엔티티와 연관관계를 맺지 않았으므로
     * 작성자 정보는 Long 값으로만 관리한다.
     */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /*
     * 댓글 내용
     */
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /*
     * 부모 댓글
     *
     * - 일반 댓글이면 null
     * - 대댓글이면 자신이 달린 부모 댓글을 참조
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    /*
     * 루트 댓글
     *
     * 댓글 트리의 최상위 댓글을 가리킨다.
     *
     * 예:
     * 루트 댓글 A
     *   ├ 대댓글 B
     *   └ 대댓글 C
     *
     * B, C의 rootComment는 모두 A가 된다.
     *
     * 댓글 목록을 묶어서 정렬할 때 사용한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_comment_id")
    private Comment rootComment;

    /*
     * 현재 댓글의 자식 댓글 목록
     *
     * parentComment를 기준으로 역방향 매핑
     * 현재 정책상 depth 1까지만 허용하므로
     * 실질적으로는 "루트 댓글 -> 대댓글들" 구조로 사용된다.
     */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>();

    /*
     * 댓글 깊이
     *
     * 0 = 일반 댓글
     * 1 = 대댓글
     */
    @Column(name = "depth", nullable = false)
    private Integer depth;

    /*
     * 댓글 생성 시간
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Comment(Post post,
                   Long authorId,
                   String content,
                   Comment parentComment,
                   Comment rootComment,
                   Integer depth) {
        this.post = post;
        this.authorId = authorId;
        this.content = content;
        this.parentComment = parentComment;
        this.rootComment = rootComment;
        this.depth = depth;
    }

    /*
     * DB INSERT 직전에 자동 실행
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /*
     * 루트 댓글 설정 메서드
     *
     * 루트 댓글은 저장 직후 자기 자신을 rootComment로 넣어준다.
     */
    public void setRootComment(Comment rootComment) {
        this.rootComment = rootComment;
    }
}