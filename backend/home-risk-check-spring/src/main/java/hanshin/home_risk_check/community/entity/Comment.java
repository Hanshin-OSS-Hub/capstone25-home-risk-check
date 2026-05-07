package hanshin.home_risk_check.community.entity;

import hanshin.home_risk_check.user.entity.User; // [변경] 댓글 작성자를 User 엔티티와 연관관계로 매핑하기 위해 추가
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
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /*
     * [변경]
     * 기존 Long authorId 대신 User 엔티티와 FK 연관관계 매핑
     * comment.author_id -> user.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User user;

    /*
     * 댓글 내용
     */
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /*
     * 부모 댓글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    /*
     * 루트 댓글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_comment_id")
    private Comment rootComment;

    /*
     * 현재 댓글의 자식 댓글 목록
     */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>();

    /*
     * 댓글 깊이
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
                   User user, // [변경] Long authorId -> User user
                   String content,
                   Comment parentComment,
                   Comment rootComment,
                   Integer depth) {
        this.post = post;
        this.user = user; // [변경]
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
     */
    public void setRootComment(Comment rootComment) {
        this.rootComment = rootComment;
    }
}