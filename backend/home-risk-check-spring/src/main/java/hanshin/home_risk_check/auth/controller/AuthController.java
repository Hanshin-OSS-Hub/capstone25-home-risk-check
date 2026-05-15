package hanshin.home_risk_check.auth.controller;

import hanshin.home_risk_check.auth.dto.TokenResponse;
import hanshin.home_risk_check.auth.service.AuthService;
import hanshin.home_risk_check.global.dto.ApiResponse;
import hanshin.home_risk_check.user.dto.LoginRequest;
import hanshin.home_risk_check.user.dto.LoginResponse;
import hanshin.home_risk_check.user.dto.SignupRequest;
import hanshin.home_risk_check.user.dto.UserResponse;
import hanshin.home_risk_check.user.entity.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        UserResponse response = authService.signup(signupRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "회원 가입 성공", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        String refreshToken = response.token().refreshToken();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                                              .httpOnly(true)
                                              .secure(false)
                                              .sameSite("Lax")
                                              .path("/")
                                              .maxAge(7 * 24 * 60 * 60)
                                              .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(ApiResponse.success(HttpStatus.OK.value(), "로그인 성공", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getUserId());
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                                              .httpOnly(true)
                                              .secure(false)
                                              .sameSite("Lax")
                                              .path("/")
                                              .maxAge(0)
                                              .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(ApiResponse.success(HttpStatus.OK.value(), "로그아웃 성공", null));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        TokenResponse response = authService.reissue(refreshToken);
        String newRefreshToken = response.refreshToken();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefreshToken)
                                              .httpOnly(true)
                                              .secure(false)
                                              .sameSite("Lax")
                                              .path("/")
                                              .maxAge(7 * 24 * 60 * 60)
                                              .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(ApiResponse.success(HttpStatus.OK.value(), "토큰 재발급 성공", response));
    }
}