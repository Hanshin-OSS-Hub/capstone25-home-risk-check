package hanshin.home_risk_check.auth.service;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-token-expiration}")
    private long REFRESH_TOKEN_EXPIRATION;

    private static final String KEY_PREFIX = "refresh:";
    private final RedissonClient redissonClient;

    @Autowired
    public RefreshTokenService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /** 발급 — 기존 토큰이 있으면 덮어씀 */
    public void save(Long userKey, String refreshToken) {
        RBucket<String> bucket = bucket(userKey);
        String tokenHash = hash(refreshToken);
        Duration expiration = Duration.ofMillis(REFRESH_TOKEN_EXPIRATION);
        bucket.set(tokenHash, expiration);
    }

    /** 매칭 검증 — 클라이언트가 보낸 refresh가 저장된 것과 같은지 */
    public boolean matches(Long userKey, String refreshToken) {
        String savedTokenHash = bucket(userKey).get();
        if (savedTokenHash == null) {
            return false;
        }

        byte[] saved = savedTokenHash.getBytes(StandardCharsets.UTF_8);
        byte[] provided = hash(refreshToken).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(saved, provided);
    }

    /** 로그아웃 / 명시적 무효화 */
    public void revoke(Long userKey) {
        bucket(userKey).delete();
    }

    private RBucket<String> bucket(Long userKey) {
        return redissonClient.getBucket(KEY_PREFIX + userKey, StringCodec.INSTANCE);
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