package hanshin.home_risk_check.safetyscore.domain.population.service;

import hanshin.home_risk_check.safetyscore.domain.population.service.PopulationService;
import hanshin.home_risk_check.safetyscore.infra.dto.PopulationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/population")
@RequiredArgsConstructor
@Slf4j
public class PopulationController {

    private final PopulationService populationService;

    /**
     * 1. 누락된 전체 인구 데이터 수집 트리거
     * Postman: POST http://localhost:8080/api/population/sync
     */
    @PostMapping("/sync")
    public ResponseEntity<String> syncMissingPopulation() {
        log.info("수동 인구 데이터 동기화 API 호출됨");
        // 쓰레드를 분리해서 응답을 먼저 주고 백그라운드에서 돌게 할 수도 있지만,
        // 지금은 로그를 보기 위해 동기로 실행합니다.
        populationService.updateAllMissingPopulation();
        return ResponseEntity.ok("인구 데이터 수집 프로세스가 완료되었습니다. 로그를 확인해주세요!");
    }

    /**
     * 2. 특정 법정동 코드로 단건 테스트 (군내면 등 테스트용)
     * Postman: GET http://localhost:8080/api/population/test/4148038000
     */
    @GetMapping("/test/{admCode}")
    public ResponseEntity<PopulationResponse.PopulationItem> testGetPopulation(@PathVariable String admCode) {
        log.info("특정 지역 단건 조회 테스트: {}", admCode);

        // 서비스 로직을 직접 태워서 결과를 반환합니다.
        PopulationResponse.PopulationItem result = populationService.getPopulationByDongCode(admCode);

        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.noContent().build(); // 204 No Content (데이터 없음)
        }
    }
}