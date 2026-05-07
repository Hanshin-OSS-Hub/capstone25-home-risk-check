package hanshin.home_risk_check.safetyscore.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncScheduler {
    private final DataSyncCoordinator dataSyncCoordinator;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String SYNC_LOCK_KEY = "lock:data-sync";
    private static final String DATA_READY_KEY = "system:is_data_ready";

    /**
     * 매일 새벽 4시에 실행
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void scheduledSync() {
        log.info("새벽 4시 정기 안전 점수 데이터 동기화 배치를 시도합니다.");
        executeSyncWithLock();
    }

    /**
     * Admin API 등에서 수동으로 트리거할 때 사용하는 메서드
     */
    public boolean triggerManualSync() {
        log.info("Admin 요청으로 수동 데이터 동기화를 시도합니다.");
        return executeSyncWithLock();
    }

    private boolean executeSyncWithLock() {
        // Redisson 기반 분산 락 객체 획득
        RLock lock = redissonClient.getLock(SYNC_LOCK_KEY);

        try {
            /*
             * waitTime(0): 락을 획득할 수 없으면 즉시 포기
             * leaseTime(-1): Redisson Watchdog 활성화
             * -> 서버가 살아있는 한 10초마다 락 유효시간을 30초로 자동 연장함
             */
            boolean isLocked = lock.tryLock(0, -1, TimeUnit.MILLISECONDS);

            if (!isLocked) {
                log.warn("다른 서버 인스턴스에서 이미 동기화를 진행 중입니다. 배치를 스킵합니다.");
                return false;
            }

            log.info("분산 락 획득 성공. 데이터 동기화를 시작합니다.");

            // 1. 실제 무거운 데이터 동기화 로직 실행
            dataSyncCoordinator.syncAndRecalculate();

            // 2. 동기화 완료 후 Redis에 준비 완료 플래그(Flag) 활성화
            redisTemplate.opsForValue().set(DATA_READY_KEY, "true");
            log.info("데이터 동기화 완료 및 서비스 준비 상태(Ready) 활성화");

            return true;

        } catch (InterruptedException e) {
            log.error("동기화 락 획득 중 스레드 인터럽트 발생", e);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            // 현재 스레드가 락을 가지고 있을 때만 안전하게 해제
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("데이터 동기화 분산 락 해제 완료");
            }
        }
    }
}
