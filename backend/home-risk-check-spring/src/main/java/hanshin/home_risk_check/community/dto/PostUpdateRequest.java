package hanshin.home_risk_check.community.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record PostUpdateRequest (

    @NotNull
    @Valid
    PostBaseRequest postBaseRequest,

    @Valid
    PostPlaceRequest postPlaceRequest,

    @Size(max = 10, message = "삭제할 이미지는 최대 10개까지 가능합니다.")
    List<
         @NotNull(message = "삭제할 이미지 ID는 null이 될 수 없습니다.")
         Long
    > deletePostImageIds
) {}