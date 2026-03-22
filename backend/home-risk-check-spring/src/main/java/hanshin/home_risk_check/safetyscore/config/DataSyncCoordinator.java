package hanshin.home_risk_check.safetyscore.config;

import hanshin.home_risk_check.safetyscore.config.loader.*;
import hanshin.home_risk_check.safetyscore.domain.accident.service.AccidentScoreService;
import hanshin.home_risk_check.safetyscore.domain.region.repository.RegionRepository;
import hanshin.home_risk_check.safetyscore.domain.region.repository.SggSafetyStatsRepository;
import hanshin.home_risk_check.safetyscore.domain.region.service.RegionSafetyScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSyncCoordinator {

    private final RegionDataLoader regionDataLoader;
    private final PoliceDataLoader policeDataLoader;
    private final FireStationDataLoader fireDataLoader;
    private final CctvDataLoader cctvDataLoader;
    private final AccidentPointLoader accidentPointLoader;
    private final AccidentStatsLoader accidentStatsLoader;
    private final CrimeStatsLoader crimeStatsLoader;
    private final PopulationDataLoader populationDataLoader;

    private final AccidentScoreService accidentScoreService;
    private final RegionSafetyScoreService safetyScoreService;

    private final SggSafetyStatsRepository sggSafetyStatsRepository;
    private final RegionRepository regionRepository;

    private final JdbcTemplate jdbcTemplate;


    public void syncAndRecalculate() {
        log.info("전체 데이터 동기화 및 점수 산출 프로세스 시작");

        // 공간 인덱스 체크
        prepareSpatialIndexes();

        // 각 로더 실행 및 변경 사항 확인
        boolean isRegionChanged = regionDataLoader.loadData();
        boolean isPoliceChanged = policeDataLoader.loadData();
        boolean isFireChanged = fireDataLoader.loadData();
        boolean isCctvChanged = cctvDataLoader.loadData();
        boolean isAccidentPointChanged = accidentPointLoader.loadData();
        boolean isAccidentStatsChanged = accidentStatsLoader.loadData();
        boolean isCrimeChanged = crimeStatsLoader.loadData();
        boolean isPopulationChanged = populationDataLoader.loadData();

        // 기초 지역 교통사고 통계
        if (isAccidentStatsChanged || sggSafetyStatsRepository.existsByAccCntIsNull()) {
            log.info("교통사고 통계 CSV 파일이 변경되었거나, 비어있는 데이터가 있어 TAAS API 호출을 시작합니다.");
            accidentScoreService.updateAllAccidentScores();
        } else {
            log.info("모든 지역의 사고 데이터가 존재합니다. TAAS API 호출을 건너뜁니다.");
        }

        log.info("전체 데이터 동기화 및 점수 산출 프로세스 종료");
    }

    private void prepareSpatialIndexes() {
        try {
            jdbcTemplate.execute("CREATE SPATIAL INDEX idx_cctvs_geom ON cctvs(geometry)");
            jdbcTemplate.execute("CREATE SPATIAL INDEX idx_police_geom ON police_stations(geometry)");
            jdbcTemplate.execute("CREATE SPATIAL INDEX idx_fire_geom ON fire_stations(geometry)");
            jdbcTemplate.execute("CREATE SPATIAL INDEX idx_region_geom ON safety_regions(geometry)");
        } catch (Exception e) {
            log.debug("공간 인덱스가 이미 존재합니다.");
        }
    }

}
