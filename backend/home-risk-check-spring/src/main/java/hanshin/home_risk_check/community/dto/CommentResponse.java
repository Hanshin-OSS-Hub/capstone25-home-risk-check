package hanshin.home_risk_check.community.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
    Long id,
    AuthorResponse author,
    String content,
    LocalDateTime createdAt,
    boolean isWrittenByMe,
    List<CommentResponse> childComments
){}