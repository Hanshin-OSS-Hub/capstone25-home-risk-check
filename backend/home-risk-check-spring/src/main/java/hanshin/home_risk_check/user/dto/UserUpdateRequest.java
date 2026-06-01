package hanshin.home_risk_check.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UserUpdateRequest(

    @NotNull
    @Valid
    NicknameUpdateRequest nicknameUpdateRequest,

    @NotNull
    @Valid
    PasswordUpdateRequest passwordUpdateRequest
) {}