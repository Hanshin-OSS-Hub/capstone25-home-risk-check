package hanshin.home_risk_check.community.dto;

import lombok.Builder;

@Builder
public record PostImageResponse(
    Long id,
    String imageUrl
) {}