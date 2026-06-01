package hanshin.home_risk_check.auth.storage;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "refreshToken:";
    private final RedissonClient redissonClient;

    @Override
    public void save(Long userId, String tokenHash, Duration ttl) {
        bucket(userId).set(tokenHash, ttl);
    }

    @Override
    public Optional<String> find(Long userId) {
        return Optional.ofNullable(bucket(userId).get());
    }

    @Override
    public void delete(Long userId) {
        bucket(userId).delete();
    }

    private RBucket<String> bucket(Long userId) {
        return redissonClient.getBucket(KEY_PREFIX + userId, StringCodec.INSTANCE);
    }
}
