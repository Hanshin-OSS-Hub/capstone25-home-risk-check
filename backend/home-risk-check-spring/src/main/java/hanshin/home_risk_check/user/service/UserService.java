package hanshin.home_risk_check.user.service;

import hanshin.home_risk_check.user.dto.UserResponse;

public interface UserService {
    UserResponse getUserById(Long userId);
    UserResponse getUserByEmail(String email);
    UserResponse updatePassword(Long userId, String currentPassword, String newPassword);
    UserResponse updateNickname(Long userId, String newNickname);
    UserResponse updateProfileImage(Long userId, String newProfileImageUrl);
    void deleteUser(Long userId);
}
