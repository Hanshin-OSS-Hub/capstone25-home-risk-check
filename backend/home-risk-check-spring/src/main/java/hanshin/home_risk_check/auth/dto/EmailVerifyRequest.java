package hanshin.home_risk_check.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerifyRequest(

    @NotBlank(message = "이메일은 비어 있을 수 없습니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    String email,

    @NotBlank(message = "인증 코드는 비어 있을 수 없습니다.")
    @Pattern(regexp = "\\d{6}", message = "인증 코드는 6자리 숫자입니다.")
    String code
) {}