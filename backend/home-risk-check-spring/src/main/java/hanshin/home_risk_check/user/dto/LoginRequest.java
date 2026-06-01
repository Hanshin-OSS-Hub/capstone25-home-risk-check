package hanshin.home_risk_check.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest (

    @NotBlank(message = "이메일은 비어 있을 수 없습니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 50, message = "이메일은 최대 50자까지 가능합니다.")
    String email,

    @NotBlank(message = "비밀번호는 비어 있을 수 없습니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
    @Pattern(regexp = PASSWORD_PATTERN, message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.")
    String password
) {
    private static final String PASSWORD_PATTERN =
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[~!@#$%^&*()-_+=|{}\\[\\]:;<>.,?'\"])[A-Za-z\\d~!@#$%^&*()-_+=|{}\\[\\]:;<>.,?'\"]{8,20}$";
}