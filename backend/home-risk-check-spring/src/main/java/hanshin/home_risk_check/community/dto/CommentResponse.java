package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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