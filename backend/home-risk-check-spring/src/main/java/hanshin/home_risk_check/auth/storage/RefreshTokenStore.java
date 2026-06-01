package hanshin.home_risk_check.auth.storage;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenStore {
    void save(Long userId, String tokenHash, Duration ttl);
    Optional<String> find(Long userId);
    void delete(Long userId);
}