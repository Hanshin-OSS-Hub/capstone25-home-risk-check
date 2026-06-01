package hanshin.home_risk_check.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

@Builder
public record TokenResponse(
    String accessToken,

    @JsonIgnore
    String refreshToken
) {}