package hanshin.home_risk_check.community.dto;

import lombok.Builder;

@Builder
public record PollOptionResponse(
    Long id,
    String optionName,
    long voteCount,
    boolean isSelectedByMe
) {}
