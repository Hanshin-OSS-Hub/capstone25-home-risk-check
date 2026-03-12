package hanshin.home_risk_check.safetyscore.domain.region.controller;

import hanshin.home_risk_check.safetyscore.domain.region.service.RegionSafetyScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/safety-score")
public class TestScoreController {

    private final RegionSafetyScoreService regionSafetyScoreService;

    // 특정 지역 코드(admCode)를 넣으면 해당 지역의 계산 과정을 로그로 출력
    // 브라우저 주소창에: http://localhost:8080/api/test/safety-score/check/1111051500 입력
    @GetMapping("/check/{admCode}")
    public Map<String, Object> checkSingleRegion(@PathVariable String admCode) {
        return regionSafetyScoreService.debugSingleRegionScore(admCode);
    }
}