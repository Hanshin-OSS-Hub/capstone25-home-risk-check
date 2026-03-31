package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/*
 * 댓글 응답 DTO
 *
 * 엔티티 내부는 JPA 연관관계로 맵핑되어 있어도,
 * 프론트/API 응답은 ID 기반으로 유지한다.
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

    /*
     * Entity -> DTO 변환
     *
     * 연관관계 객체에서 필요한 ID만 꺼내서 응답에 담는다.
     */
    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPost().getPostId())
                .authorId(comment.getAuthorId())
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