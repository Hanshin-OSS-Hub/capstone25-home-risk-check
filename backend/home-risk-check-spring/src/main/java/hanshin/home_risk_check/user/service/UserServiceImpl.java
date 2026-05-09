package hanshin.home_risk_check.user.service;

import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import hanshin.home_risk_check.global.security.JwtUtil;
import hanshin.home_risk_check.user.mapper.UserMapper;
import hanshin.home_risk_check.user.dto.LoginRequest;
import hanshin.home_risk_check.user.dto.LoginResponse;
import hanshin.home_risk_check.user.dto.SignupRequest;
import hanshin.home_risk_check.user.dto.UserResponse;
import hanshin.home_risk_check.user.entity.CustomUserDetails;
import hanshin.home_risk_check.user.entity.Role;
import hanshin.home_risk_check.user.entity.User;
import hanshin.home_risk_check.user.repository.UserRepository;
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
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .profileImageUrl(null)
                .role(Role.USER)
                .build();
        User saved = userRepository.save(user);
        return userMapper.from(saved);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String accessToken  = jwtUtil.generateAccessToken(Objects.requireNonNull(userDetails).getUsername(), userDetails.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());

        //리프레시 토큰 저장 로직 필요

        return new LoginResponse(accessToken, refreshToken);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository
                    .findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return userMapper.from(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository
                    .findByEmail(email)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return userMapper.from(user);
    }

    @Override
    @Transactional
    public void updatePassword(Long id, String currentPassword, String newPassword) {
        User user = userRepository
                    .findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public void updateNickname(Long id, String newNickname) {
        if(userRepository.existsByNickname(newNickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = userRepository
                    .findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updateNickname(newNickname);
    }

    @Override
    @Transactional
    public void updateProfileImage(Long id, String newProfileImageUrl) {
        User user = userRepository
                    .findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updateProfileImage(newProfileImageUrl);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(id);
    }
}
