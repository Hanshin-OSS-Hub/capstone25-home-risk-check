package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.PostCategory;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record PostBaseResponse(
    Long id,
    AuthorResponse author,
    PostCategory postCategory,
    String title,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
