package hanshin.home_risk_check.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PostPollRequest(

    @NotNull(message = "복수 선택 허용 여부는 필수입니다.")
    Boolean allowMultiple,

    @NotEmpty(message = "옵션은 최소 1개 필요합니다.")
    @Size(min = 1, max = 5, message = "옵션은 1~5개여야 합니다.")
    List<
         @NotBlank(message = "옵션 내용은 비어 있을 수 없습니다.")
         @Size(max = 100, message = "옵션 내용은 100자까지 가능합니다.")
         String
    > optionNames
) {}