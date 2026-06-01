package hanshin.home_risk_check.auth.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
public class RefreshTokenCookieFactory {

    private static final String COOKIE_NAME = "refreshToken";
    private final Duration refreshTokenTtl;

    public RefreshTokenCookieFactory(@Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationMillis) {
        this.refreshTokenTtl = Duration.ofMillis(refreshTokenExpirationMillis);
    }

    /* 신규 발급 / 재발급용 — 토큰 담은 쿠키 */
    public ResponseCookie create(String refreshToken) {
        return baseBuilder()
                .value(refreshToken)
                .maxAge(refreshTokenTtl)
                .build();
    }

    /* 로그아웃용 — 빈 값, 즉시 만료 */
    public ResponseCookie revoke() {
        return baseBuilder()
                .value("")
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder() {
        return ResponseCookie.from(COOKIE_NAME, "")
                             .httpOnly(true)
                             .secure(false)
                             .sameSite("Lax")
                             .path("/");
    }
}