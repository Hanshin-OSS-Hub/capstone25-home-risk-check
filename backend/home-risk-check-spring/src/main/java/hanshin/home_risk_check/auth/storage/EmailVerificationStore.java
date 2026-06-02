package hanshin.home_risk_check.auth.storage;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Optional;

/*
 * - 인증 코드:   "emailVerify:code:{email}"     (TTL = code-ttl-minutes)
 * - 인증 완료:   "emailVerify:verified:{email}" (TTL = verified-ttl-minutes)
 */
@Component
@RequiredArgsConstructor
public class EmailVerificationStore {

    private static final String CODE_PREFIX = "emailVerify:code:";
    private static final String VERIFIED_PREFIX = "emailVerify:verified:";
    private final RedissonClient redissonClient;

    @Value("${mail.verification.code-ttl-minutes}")
    private long codeTtlMinutes;

    @Value("${mail.verification.verified-ttl-minutes}")
    private long verifiedTtlMinutes;

    public void saveCode(String email, String code) {
        codeBucket(email).set(code, Duration.ofMinutes(codeTtlMinutes));
    }

    public Optional<String> findCode(String email) {
        return Optional.ofNullable(codeBucket(email).get());
    }

    public void deleteCode(String email) {
        codeBucket(email).delete();
    }

    public void markVerified(String email) {
        verifiedBucket(email).set("true", Duration.ofMinutes(verifiedTtlMinutes));
    }

    public boolean isVerified(String email) {
        return "true".equals(verifiedBucket(email).get());
    }

    public void clearVerified(String email) {
        verifiedBucket(email).delete();
    }

    private RBucket<String> codeBucket(String email) {
        return redissonClient.getBucket(CODE_PREFIX + email, StringCodec.INSTANCE);
    }

    private RBucket<String> verifiedBucket(String email) {
        return redissonClient.getBucket(VERIFIED_PREFIX + email, StringCodec.INSTANCE);
    }
}