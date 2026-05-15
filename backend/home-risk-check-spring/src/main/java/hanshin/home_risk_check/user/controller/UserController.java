package hanshin.home_risk_check.user.controller;

import hanshin.home_risk_check.global.dto.ApiResponse;
import hanshin.home_risk_check.user.dto.*;
import hanshin.home_risk_check.user.entity.CustomUserDetails;
import hanshin.home_risk_check.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal CustomUserDetails currentUser) {
        UserResponse response = userService.getUserById(currentUser.getUserId());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "내 정보 조회 성공", response));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<UserResponse>> updatePassword(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                                    @Valid @RequestBody PasswordUpdateRequest passwordUpdateRequest) {
        UserResponse updatedUser = userService.updatePassword(currentUser.getUserId(), passwordUpdateRequest.currentPassword(), passwordUpdateRequest.newPassword());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "비밀번호 변경 성공", updatedUser));
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<UserResponse>> updateNickname(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                                    @Valid @RequestBody NicknameUpdateRequest nicknameUpdateRequest) {
        UserResponse updatedUser = userService.updateNickname(currentUser.getUserId(), nicknameUpdateRequest.newNickname());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "닉네임 변경 성공", updatedUser));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe(@AuthenticationPrincipal CustomUserDetails currentUser) {
        userService.deleteUser(currentUser.getUserId());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "회원 탈퇴 성공", null));
    }
}