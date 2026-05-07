package hanshin.home_risk_check.user.dto;

import jakarta.validation.constraints.Email; // [변경] 이메일 형식 검증을 위해 추가
import jakarta.validation.constraints.NotBlank; // [변경] 빈 문자열 검증을 위해 추가
import jakarta.validation.constraints.Size; // [변경] 길이 제한 검증을 위해 추가
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {

    @NotBlank(message = "이메일은 비어 있을 수 없습니다.") // [변경]
    @Email(message = "올바른 이메일 형식이 아닙니다.") // [변경]
    private String email;

    @NotBlank(message = "비밀번호는 비어 있을 수 없습니다.") // [변경]
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.") // [변경]
    private String password;

    @NotBlank(message = "닉네임은 비어 있을 수 없습니다.") // [변경]
    @Size(max = 20, message = "닉네임은 최대 20자까지 가능합니다.") // [변경]
    private String nickname;
}