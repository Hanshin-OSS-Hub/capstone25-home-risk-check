package hanshin.home_risk_check.user.service;

import hanshin.home_risk_check.user.dto.LoginRequest;
import hanshin.home_risk_check.user.dto.LoginResponse;
import hanshin.home_risk_check.user.dto.SignupRequest;
import hanshin.home_risk_check.user.dto.UserResponse;

public interface UserService {
    UserResponse signup(SignupRequest request);
    LoginResponse login(LoginRequest request);
    UserResponse getUserById(Long id);
    UserResponse getUserByEmail(String email);
    void deleteUser(Long id);
}
