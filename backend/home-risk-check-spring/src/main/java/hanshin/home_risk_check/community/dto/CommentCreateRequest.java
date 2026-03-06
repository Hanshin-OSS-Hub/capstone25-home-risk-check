package hanshin.home_risk_check.community.dto;

import lombok.Getter;

@Getter
public class CommentCreateRequest {
    private String content;
    private Long parentCommentId;
}