package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/*
 * 댓글 응답 DTO
 */
@Getter
@Builder
public class CommentResponse {

    private Long commentId;
    private Long postId;
    private Long authorId;
    private String content;
    private Long parentCommentId;
    private Long rootCommentId;
    private Integer depth;
    private LocalDateTime createdAt;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPost().getPostId())
                .authorId(comment.getUser().getId()) // [변경] comment.getAuthorId() -> comment.getUser().getId()
                .content(comment.getContent())
                .parentCommentId(
                        comment.getParentComment() != null
                                ? comment.getParentComment().getCommentId()
                                : null
                )
                .rootCommentId(
                        comment.getRootComment() != null
                                ? comment.getRootComment().getCommentId()
                                : null
                )
                .depth(comment.getDepth())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}