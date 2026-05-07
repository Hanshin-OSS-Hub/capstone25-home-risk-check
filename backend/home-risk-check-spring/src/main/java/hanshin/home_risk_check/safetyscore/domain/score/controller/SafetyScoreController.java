package hanshin.home_risk_check.safetyscore.domain.score.controller;

import hanshin.home_risk_check.safetyscore.domain.score.dto.SafetyScoreResponse;
import hanshin.home_risk_check.safetyscore.domain.score.service.SafetyScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/safetyScore")
public class SafetyScoreController {

    private final SafetyScoreService safetyScoreService;

    /**
     * 주소를 기반으로 반경 500m 내의 치안/안전 점수를 측정합니다.
     * API 호출 예시: GET /api/v1/safetyScore/get-score?address=서울 강남구 역삼동 123
     */
    @GetMapping("/get-score")
    public ResponseEntity<SafetyScoreResponse> getSafetyScore(
            @RequestParam(name = "address") String address) {

        log.info("안전 점수 측정 요청 수신 - 주소: {}", address);

        SafetyScoreResponse response = safetyScoreService.calculateSafetyScore(address);

        // 결과 반환 (HTTP 상태코드 200 OK)
        return ResponseEntity.ok(response);
    }
}
