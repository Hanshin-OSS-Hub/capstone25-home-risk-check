package hanshin.home_risk_check.safetyscore.domain.accident.service;

import hanshin.home_risk_check.safetyscore.domain.region.entity.SggSafetyStats;
import hanshin.home_risk_check.safetyscore.domain.region.repository.RegionRepository;
import hanshin.home_risk_check.safetyscore.domain.region.repository.SggSafetyStatsRepository;
import hanshin.home_risk_check.safetyscore.infra.api.TaasApiCaller;
import hanshin.home_risk_check.safetyscore.infra.dto.TaasApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccidentScoreService {

    private final SggSafetyStatsRepository taasAreaCodeRepository;
    private final TaasApiCaller taasApiCaller;
    private final RegionRepository regionRepository;

    @Transactional
    public void updateAllAccidentScores() {
        // DB에서 로더로 지역 코드가져옴
        List<SggSafetyStats> areaCodes = taasAreaCodeRepository.findAll();
        int updateCount = 0;

        log.info("TAAS API 통계 데이터 수집 시작 (총 {}개 지역)", areaCodes.size());

        for (SggSafetyStats areaCode : areaCodes) {
            try {// TAAS API 호출
                TaasApiResponse response = taasApiCaller.fetchTrafficRiskData(areaCode.getSidoCode(), areaCode.getSggCode());

                if (response != null && response.getItems() != null && response.getItems().getItem() != null) {
                    response.getItems().getItem().stream()
                            .filter(item -> "전체사고".equals(item.getAccClNm()))
                            .findFirst()
                            .ifPresent(item -> {
                                // 엔티티의 updateStatistics 메서드를 호출하여 데이터 채우기
                                // API 응답은 String이므로 숫자 타입으로 변환
                                areaCode.updateStatistics(
                                        Integer.parseInt(item.getAccCnt()),
                                        Integer.parseInt(item.getDthDnvCnt()),
                                        Double.parseDouble(item.getPop100k())
                                );
                            });

                    updateCount++;
                }

                Thread.sleep(100);
            } catch (Exception e) {
                log.error("{} {} 지역의 통계 수집 중 오류 발생: {}", areaCode.getSidoNm(), areaCode.getSggNm(), e.getMessage());
            }
        }
        log.info("TAAS API 통계 데이터 수집 완료 (성공: {}개 지역)", updateCount);
    }
}

