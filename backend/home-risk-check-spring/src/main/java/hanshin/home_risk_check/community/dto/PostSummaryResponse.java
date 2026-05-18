package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.PostCategory;
import java.time.LocalDateTime;

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