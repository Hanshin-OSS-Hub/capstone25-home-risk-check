package hanshin.home_risk_check.community.dto;

import lombok.Builder;

@Builder
public record AuthorResponse(
    Long id,
    String nickname,
    String profileImageUrl
) {}