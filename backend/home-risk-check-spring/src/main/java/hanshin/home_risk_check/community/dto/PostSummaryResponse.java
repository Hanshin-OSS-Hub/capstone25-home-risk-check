package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.PostCategory;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record PostSummaryResponse(
    Long id,
    AuthorResponse author,
    PostCategory postCategory,
    String title,
    String content,
    String thumbnailUrl,
    long likeCount,
    long commentCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}