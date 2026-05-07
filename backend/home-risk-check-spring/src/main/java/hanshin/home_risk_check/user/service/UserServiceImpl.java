package hanshin.home_risk_check.user.service;

import hanshin.home_risk_check.global.security.JwtUtil;
import hanshin.home_risk_check.mapper.UserMapper;
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

@Service
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
        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setProfileImageUrl(null);
        user.setRole(Role.USER);
        User saved = userRepository.save(user);
        return userMapper.from(saved);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String accessToken  = jwtUtil.generateAccessToken(
                userDetails.getUsername(), userDetails.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());

        //리프레시 토큰 저장 로직 필요

        return new LoginResponse(accessToken, refreshToken);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository
                    .findById(id)
                    .orElseThrow(
                            () -> new IllegalArgumentException("회원 정보 없음 : " + id)
                    );
        return userMapper.from(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository
                    .findByEmail(email)
                    .orElseThrow(
                            () -> new IllegalArgumentException("회원 정보 없음 : " + email)
                    );
        return userMapper.from(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("회원 정보 없음 : " + id);
        }
        userRepository.deleteById(id);
    }
}
