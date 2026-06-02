package hanshin.home_risk_check.analysis.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * 위험도 분석 요청 (파일 제외 본문).
 */
public record AnalysisRequest(

    @Min(value = 0, message = "보증금은 0 이상이어야 합니다.")
    long deposit,

    @NotBlank(message = "주소는 비어 있을 수 없습니다.")
    @Size(min = 5, max = 200, message = "주소는 5~200자여야 합니다.")
    String address
) {}