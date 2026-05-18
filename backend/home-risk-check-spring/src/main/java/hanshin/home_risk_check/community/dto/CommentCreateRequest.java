package hanshin.home_risk_check.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest (
    @NotBlank(message = "댓글 내용은 비어 있을 수 없습니다.")
    @Size(max = 1000, message = "댓글은 최대 1000자까지 가능합니다.")
    String content,

    Long rootCommentId
){}