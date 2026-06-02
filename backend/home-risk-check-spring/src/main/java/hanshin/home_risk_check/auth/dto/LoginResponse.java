package hanshin.home_risk_check.auth.dto;

import lombok.Builder;

@Builder
public record LoginResponse  (
    TokenResponse token,
    String email,
    String nickname,
    String profileImageUrl
) {}