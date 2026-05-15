package hanshin.home_risk_check.user.dto;

import hanshin.home_risk_check.auth.dto.TokenResponse;

public record LoginResponse  (
    TokenResponse token,
    String email,
    String nickname,
    String profileImageUrl
) {}