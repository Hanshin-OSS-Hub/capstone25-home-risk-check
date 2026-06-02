package hanshin.home_risk_check.auth.mapper;

import hanshin.home_risk_check.auth.dto.TokenResponse;
import hanshin.home_risk_check.file.util.FileUrlResolver;
import hanshin.home_risk_check.auth.dto.LoginResponse;
import hanshin.home_risk_check.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginResponseMapper {

    private final FileUrlResolver fileUrlResolver;

    public LoginResponse toLoginResponse(User user, TokenResponse token) {
        return LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(fileUrlResolver.toUrl(user.getProfileImageFile()))
                .build();
    }
}
