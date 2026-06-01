package hanshin.home_risk_check.user.dto;

import hanshin.home_risk_check.auth.dto.TokenResponse;
import lombok.Builder;

@Builder
public record LoginResponse  (
    TokenResponse token,
    String email,
    String nickname,
    String profileImageUrl
) {}