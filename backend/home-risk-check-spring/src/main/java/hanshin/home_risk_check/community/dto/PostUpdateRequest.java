package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.PostCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record PostUpdateRequest (

    @NotNull(message = "카테고리는 비어 있을 수 없습니다.")
    PostCategory postCategory,

    @NotBlank(message = "제목은 비어 있을 수 없습니다.")
    @Size(max = 200, message = "제목은 최대 200자까지 가능합니다.")
    String title,

    @NotBlank(message = "내용은 비어 있을 수 없습니다.")
    @Size(max = 10000, message = "내용은 최대 10000자까지 가능합니다.")
    String content,

    @Valid
    Place place
){
    public record Place(

        @NotNull(message = "위도는 비어 있을 수 없습니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90.0 이하이어야 합니다.")
        Double latitude,

        @NotNull(message = "경도는 비어 있을 수 없습니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
        Double longitude,

        @NotBlank(message = "주소는 비어 있을 수 없습니다.")
        @Size(max = 300, message = "주소는 최대 300자까지 가능합니다.")
        String address,

        @Size(max = 100, message = "장소 이름은 최대 100자까지 가능합니다.")
        String placeName
    ) {}
}