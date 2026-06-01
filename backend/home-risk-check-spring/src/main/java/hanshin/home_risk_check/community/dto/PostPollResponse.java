package hanshin.home_risk_check.community.dto;

import lombok.Builder;

@Builder
public record PostPollResponse(
    Long id,
    boolean allowMultiple,
    long totalVotes
) {}