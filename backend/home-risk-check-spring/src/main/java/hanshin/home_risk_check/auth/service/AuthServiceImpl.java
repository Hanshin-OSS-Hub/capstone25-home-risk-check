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
import hanshin.home_risk_check.user.mapper.UserMapper;
import hanshin.home_risk_check.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
    }

    @Override
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
                        .profileImageUrl(null)
                        .role(Role.USER)
                        .build();

        User saved = userRepository.save(user);
        return userMapper.from(saved);
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String accessToken  = jwtUtil.generateAccessToken(Objects.requireNonNull(userDetails).getUserId(), userDetails.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(Objects.requireNonNull(userDetails).getUserId());
        TokenResponse tokenResponse = new TokenResponse(accessToken, refreshToken);

        refreshTokenService.save(userDetails.getUserId(), refreshToken);

        return new LoginResponse(tokenResponse, userDetails.getEmail(), userDetails.getNickname(), userDetails.getProfileImageUrl());
    }

    @Override
    public void logout(Long userId) {
        refreshTokenService.revoke(userId);
    }

    @Override
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