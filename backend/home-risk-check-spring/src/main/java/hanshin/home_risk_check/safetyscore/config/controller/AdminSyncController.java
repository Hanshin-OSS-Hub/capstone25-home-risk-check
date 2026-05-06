package hanshin.home_risk_check.safetyscore.config.controller;
import hanshin.home_risk_check.safetyscore.config.DataSyncScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sync")
public class AdminSyncController {

    private final DataSyncScheduler dataSyncScheduler;

    /**
     * [관리자 전용] 수동 데이터 동기화 트리거
     * - 서버 부팅 후 데이터가 없는 초기 상태에서 강제로 동기화를 수행할 때 사용합니다.
     * TODO: 시큐리티 적용 시 권한 제한 필요
     * @PreAuthorize("hasRole('ADMIN')")
     */
    @PostMapping("/start")
    public ResponseEntity<String> triggerManualSync() {
        log.info("관리자에 의한 수동 데이터 동기화 요청 수신");

        boolean isStarted = dataSyncScheduler.triggerManualSync();

        if (isStarted) {
            return ResponseEntity.ok("데이터 동기화 프로세스가 시작되었습니다. 잠시 후 Redis 플래그가 true로 변경됩니다.");
        } else {
            return ResponseEntity.status(409).body("이미 동기화 작업이 진행 중이거나 분산 락을 획득할 수 없습니다.");
        }
    }
}