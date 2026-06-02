package hanshin.home_risk_check.user.service;

import hanshin.home_risk_check.file.entity.ImageFile;
import hanshin.home_risk_check.file.service.ImageFileService;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import hanshin.home_risk_check.user.dto.NicknameUpdateRequest;
import hanshin.home_risk_check.user.dto.PasswordUpdateRequest;
import hanshin.home_risk_check.user.dto.UserResponse;
import hanshin.home_risk_check.user.dto.UserUpdateRequest;
import hanshin.home_risk_check.user.entity.User;
import hanshin.home_risk_check.user.mapper.UserResponseMapper;
import hanshin.home_risk_check.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageFileService imageFileService;
    private final UserResponseMapper userResponseMapper;

    public UserResponse get(Long userId) {
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return userResponseMapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        updateNickname(user, request.nicknameUpdateRequest());
        updatePassword(user, request.passwordUpdateRequest());

        log.info("회원 정보 수정 - userId={}", userId);
        return userResponseMapper.toResponse(user);
    }

    private void updateNickname(User user, NicknameUpdateRequest request) {
        String newNickname = request.newNickname();
        if (user.getNickname().equals(newNickname)) {
            return;
        }
        if (userRepository.existsByNickname(newNickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        user.updateNickname(newNickname);
    }

    private void updatePassword(User user, PasswordUpdateRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        user.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public UserResponse updateProfileImageFile(Long userId, MultipartFile image) {
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ImageFile oldImageFile = user.getProfileImageFile();
        ImageFile newImageFile = imageFileService.create(image);

        user.updateProfileImageFile(newImageFile);
        userRepository.flush(); //flush로 user FK 변경 먼저 DB 반영
        imageFileService.delete(oldImageFile);

        log.info("프로필 이미지 변경 - userId={}", userId);
        return userResponseMapper.toResponse(user);
    }

    @Transactional
    public UserResponse deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ImageFile oldImageFile = user.getProfileImageFile();

        if (oldImageFile == null) {
            return userResponseMapper.toResponse(user);
        }

        user.deleteProfileImageFile();
        userRepository.flush(); //flush로 user FK 변경 먼저 DB 반영
        imageFileService.delete(oldImageFile);

        log.info("프로필 이미지 삭제 - userId={}", userId);
        return userResponseMapper.toResponse(user);
    }

    @Transactional
    public void delete(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(userId);
        log.info("회원 탈퇴 - userId={}", userId);
    }
}