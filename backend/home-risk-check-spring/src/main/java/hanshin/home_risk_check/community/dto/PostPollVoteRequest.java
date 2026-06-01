package hanshin.home_risk_check.community.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PostPollVoteRequest(
    @NotEmpty(message = "투표 옵션은 최소 1개 선택해야 합니다.")
    List<@NotNull(message = "투표 옵션 ID는 필수입니다.") Long> pollOptionIds
) {}
