package hanshin.home_risk_check.user.controller;

import hanshin.home_risk_check.global.dto.ApiResponse;
import hanshin.home_risk_check.user.dto.*;
import hanshin.home_risk_check.user.entity.CustomUserDetails;
import hanshin.home_risk_check.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal CustomUserDetails currentUser) {
        UserResponse response = userService.get(currentUser.getUserId());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "내 정보 조회 성공", response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.update(currentUser.getUserId(), request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "내 정보 수정 성공", response));
    }

    @PutMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateProfileImage(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestPart("image") MultipartFile image
    ) {
        UserResponse response = userService.updateProfileImageFile(currentUser.getUserId(), image);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "프로필 이미지 변경 성공", response));
    }

    @DeleteMapping("/me/profile-image")
    public ResponseEntity<ApiResponse<UserResponse>> deleteProfileImage(@AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UserResponse response = userService.deleteProfileImage(currentUser.getUserId());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "프로필 이미지 삭제 성공", response));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe(@AuthenticationPrincipal CustomUserDetails currentUser) {
        userService.delete(currentUser.getUserId());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "회원 탈퇴 성공", null));
    }
}