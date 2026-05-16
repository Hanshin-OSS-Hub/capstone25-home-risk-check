package hanshin.home_risk_check.auth.service;

import hanshin.home_risk_check.auth.dto.TokenResponse;
import hanshin.home_risk_check.user.dto.LoginRequest;
import hanshin.home_risk_check.user.dto.LoginResponse;
import hanshin.home_risk_check.user.dto.SignupRequest;
import hanshin.home_risk_check.user.dto.UserResponse;

public interface AuthService {
    UserResponse signup(SignupRequest signupRequest);
    LoginResponse login(LoginRequest loginRequest);
    void logout(Long userId);
    TokenResponse reissue(String refreshToken);
}