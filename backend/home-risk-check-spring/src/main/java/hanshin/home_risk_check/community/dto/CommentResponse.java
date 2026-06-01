package hanshin.home_risk_check.community.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record CommentResponse(
    Long id,
    AuthorResponse author,
    String content,
    LocalDateTime createdAt,
    boolean isWrittenByMe,
    long replyCount
) {}