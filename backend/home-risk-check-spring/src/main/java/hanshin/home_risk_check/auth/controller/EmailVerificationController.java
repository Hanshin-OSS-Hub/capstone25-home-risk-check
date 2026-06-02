package hanshin.home_risk_check.auth.controller;

import hanshin.home_risk_check.auth.dto.EmailSendRequest;
import hanshin.home_risk_check.auth.dto.EmailVerifyRequest;
import hanshin.home_risk_check.auth.service.EmailVerificationService;
import hanshin.home_risk_check.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/email")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    /* 인증 코드 발송 */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> send(@Valid @RequestBody EmailSendRequest request) {
        emailVerificationService.sendCode(request.email());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "인증 코드 발송 성공", null));
    }

    /* 인증 코드 검증 */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody EmailVerifyRequest request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "이메일 인증 성공", null));
    }
}