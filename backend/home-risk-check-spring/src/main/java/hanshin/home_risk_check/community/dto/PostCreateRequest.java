package hanshin.home_risk_check.community.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record PostCreateRequest (

    @NotNull
    @Valid
    PostBaseRequest postBaseRequest,

    @Valid
    PostPlaceRequest postPlaceRequest,

    @Valid
    PostPollRequest postPollRequest
) {}