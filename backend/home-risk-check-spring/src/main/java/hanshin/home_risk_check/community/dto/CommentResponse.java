package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/*
 * 댓글 응답(Response) DTO
 *
 * 서버에서 댓글 데이터를 조회한 후
 * 클라이언트(프론트)에게 전달할 데이터를 담는 객체
 *
 * 예 응답 JSON
 *
 * {
 *   "commentId": 10,
 *   "postId": 3,
 *   "authorId": 2,
 *   "content": "이 매물 위험해 보입니다",
 *   "parentCommentId": null,
 *   "rootCommentId": null,
 *   "depth": 0,
 *   "createdAt": "2026-03-10T12:30:00"
 * }
 */
@Getter
@Builder
public class CommentResponse {

    /*
     * 댓글 ID (PK)
     */
    private Long commentId;

    /*
     * 댓글이 속한 게시글 ID
     */
    private Long postId;

    /*
     * 댓글 작성자 ID
     */
    private Long authorId;

    /*
     * 댓글 내용
     */
    private String content;

    /*
     * 부모 댓글 ID
     *
     * null → 일반 댓글
     * 값 있음 → 대댓글
     */
    private Long parentCommentId;

    /*
     * 최상위 댓글 ID
     *
     * 댓글이면 null
     * 대댓글이면 최상위 댓글 ID
     *
     * 댓글 트리 조회 시 사용
     */
    private Long rootCommentId;

    /*
     * 댓글 깊이
     *
     * 0 → 댓글
     * 1 → 대댓글
     */
    private Integer depth;

    /*
     * 댓글 생성 시간
     */
    private LocalDateTime createdAt;

    /*
     * Entity → DTO 변환 메서드
     *
     * Comment Entity 객체를
     * CommentResponse DTO로 변환한다.
     *
     * Service에서 보통 이렇게 사용한다.
     *
     * CommentResponse response = CommentResponse.from(comment);
     */
    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPostId())
                .authorId(comment.getAuthorId())
                .content(comment.getContent())
                .parentCommentId(comment.getParentCommentId())
                .rootCommentId(comment.getRootCommentId())
                .depth(comment.getDepth())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}