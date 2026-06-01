package hanshin.home_risk_check.auth.service;

import hanshin.home_risk_check.auth.dto.TokenResponse;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import hanshin.home_risk_check.global.security.JwtUtil;
import hanshin.home_risk_check.user.dto.LoginRequest;
import hanshin.home_risk_check.user.dto.LoginResponse;
import hanshin.home_risk_check.user.dto.SignupRequest;
import hanshin.home_risk_check.user.dto.UserResponse;
import hanshin.home_risk_check.user.entity.CustomUserDetails;
import hanshin.home_risk_check.user.entity.Role;
import hanshin.home_risk_check.user.entity.User;
import hanshin.home_risk_check.user.repository.UserRepository;
import hanshin.home_risk_check.user.mapper.UserResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserResponseMapper userResponseMapper;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserResponse signup(SignupRequest signupRequest) {
        if (userRepository.existsByEmail(signupRequest.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(signupRequest.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = User.builder()
                        .email(signupRequest.email())
                        .passwordHash(passwordEncoder.encode(signupRequest.password()))
                        .nickname(signupRequest.nickname())
                        .profileImageFile(null)
                        .role(Role.USER)
                        .build();

        User saved = userRepository.save(user);
        return userResponseMapper.toResponse(saved);
    }

    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String accessToken  = jwtUtil.generateAccessToken(Objects.requireNonNull(userDetails).getUserId(), userDetails.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(Objects.requireNonNull(userDetails).getUserId());
        TokenResponse tokenResponse = TokenResponse.builder()
                                                   .accessToken(accessToken)
                                                   .refreshToken(refreshToken)
                                                   .build();

        refreshTokenService.save(userDetails.getUserId(), refreshToken);

        return LoginResponse.builder()
                            .token(tokenResponse)
                            .email(userDetails.getEmail())
                            .nickname(userDetails.getNickname())
                            .profileImageUrl(userDetails.getProfileImageFile().getStorageKey())
                            .build();
    }

    public void logout(Long userId) {
        refreshTokenService.revoke(userId);
    }

    public TokenResponse reissue(String refreshToken){
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        JwtUtil.TokenValidationResult result =  jwtUtil.validate(refreshToken);
        if (result == JwtUtil.TokenValidationResult.EXPIRED) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }
        if (result != JwtUtil.TokenValidationResult.VALID) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        //토큰 타입 확인
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = Long.parseLong(jwtUtil.parseClaims(refreshToken).getSubject());

        if (!refreshTokenService.matches(userId, refreshToken)) {
            refreshTokenService.revoke(userId);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().getValue());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());
        TokenResponse tokenResponse = new TokenResponse(newAccessToken, newRefreshToken);

        refreshTokenService.save(user.getId(), newRefreshToken);

        return tokenResponse;
    }
}