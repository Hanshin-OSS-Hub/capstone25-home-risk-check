package hanshin.home_risk_check.community.dto;

import lombok.Builder;

@Builder
public record PostLikeResponse(
    Long postId,
    boolean liked,
    long likeCount
) {}