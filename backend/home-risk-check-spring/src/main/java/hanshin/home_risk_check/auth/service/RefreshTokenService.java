package hanshin.home_risk_check.auth.service;

import hanshin.home_risk_check.auth.storage.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-token-expiration}")
    private long REFRESH_TOKEN_EXPIRATION;

    private final RefreshTokenStore store;

    /** 발급 — 기존 토큰이 있으면 덮어씀 */
    public void save(Long userId, String refreshToken) {
        store.save(userId, hash(refreshToken), Duration.ofMillis(REFRESH_TOKEN_EXPIRATION));
    }

    /** 매칭 검증 — 클라이언트가 보낸 refresh가 저장된 것과 같은지 */
    public boolean matches(Long userId, String refreshToken) {
        return store.find(userId)
                    .map(saved -> MessageDigest.isEqual(
                            saved.getBytes(StandardCharsets.UTF_8),
                            hash(refreshToken).getBytes(StandardCharsets.UTF_8)
                    ))
                    .orElse(false);
    }

    /** 로그아웃 / 명시적 무효화 */
    public void revoke(Long userId) {
        store.delete(userId);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder()
                         .withoutPadding()
                         .encodeToString(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
