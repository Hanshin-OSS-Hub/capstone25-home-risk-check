package hanshin.home_risk_check.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest (

    @NotBlank(message = "비밀번호는 비어 있을 수 없습니다.")
    String currentPassword,

    @NotBlank(message = "비밀번호는 비어 있을 수 없습니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
    @Pattern(regexp = PASSWORD_PATTERN, message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.")
    String newPassword
) {
    private static final String PASSWORD_PATTERN =
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[~!@#$%^&*()-_+=|{}\\[\\]:;<>.,?'\"])[A-Za-z\\d~!@#$%^&*()-_+=|{}\\[\\]:;<>.,?'\"]{8,20}$";
}