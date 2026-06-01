package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostBaseRequest(

    @NotNull(message = "카테고리는 비어 있을 수 없습니다.")
    PostCategory postCategory,

    @NotBlank(message = "제목은 비어 있을 수 없습니다.")
    @Size(max = 100, message = "제목은 최대 100자까지 가능합니다.")
    String title,

    @NotBlank(message = "내용은 비어 있을 수 없습니다.")
    @Size(max = 10000, message = "내용은 최대 10000자까지 가능합니다.")
    String content
) {}