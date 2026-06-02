package hanshin.home_risk_check.auth.service;

import hanshin.home_risk_check.auth.infra.EmailSender;
import hanshin.home_risk_check.auth.storage.EmailVerificationStore;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import hanshin.home_risk_check.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_BOUND = 1_000_000; // 000000 ~ 999999

    private final EmailVerificationStore verificationStore;
    private final EmailSender emailSender;
    private final UserRepository userRepository;

    public void sendCode(String email) {
        // 이미 가입된 이메일이면 발송하지 않음
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String code = generateCode();
        verificationStore.saveCode(email, code);
        emailSender.send(email, buildSubject(), buildBody(code));
    }

    public void verifyCode(String email, String code) {
        String savedCode = verificationStore.findCode(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_CODE_NOT_FOUND));

        if (!savedCode.equals(code)) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        verificationStore.deleteCode(email);
        verificationStore.markVerified(email);
    }

    /* 회원가입 시 인증 완료 여부 확인 */
    public void ensureVerified(String email) {
        if (!verificationStore.isVerified(email)) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    /* 회원가입 완료 후 인증 마킹 정리 */
    public void clearVerified(String email) {
        verificationStore.clearVerified(email);
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(CODE_BOUND));
    }

    private String buildSubject() {
            return "[HomeRiskCheck] 이메일 인증 코드";
    }

    private String buildBody(String code) {
        return """
               아래 6자리 인증 코드를 입력해주세요.

               인증 코드: %s
               """.formatted(code);
    }
}