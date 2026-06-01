package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.PostCategory;
import hanshin.home_risk_check.community.entity.PostSortType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record PostSearchRequest(
    PostCategory postCategory,

    String keyword,

    PostSortType postSortType,

    @PositiveOrZero(message = "페이지 번호는 0 이상이어야 합니다.")
    int page,

    @Positive(message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하이어야 합니다.")
    int size
) {}