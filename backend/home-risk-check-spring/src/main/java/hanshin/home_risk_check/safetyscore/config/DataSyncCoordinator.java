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

    private final SpatialRegionIndex spatialRegionIndex;

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

        log.info("============================================================================");
        log.info("전체 데이터 동기화 및 점수 산출 프로세스 시작");
        log.info("============================================================================");

        // 공간 인덱스 체크
        prepareSpatialIndexes();

        // 각 로더 실행 및 변경 사항 확인
        // 기초 점수 계산용
        log.info("기초 기준 데이터(Region) 적재 및 공간 인덱스 초기화 시작...");
        boolean isRegionChanged = regionDataLoader.loadData();
        log.info("----------------------------------------------------------------------------");
        spatialRegionIndex.init();

        log.info("기초 점수 계산용 인프라 및 통계 데이터 적재 시작...");
        log.info("----------------------------------------------------------------------------");
        boolean isPoliceChanged = policeDataLoader.loadData();
        log.info("----------------------------------------------------------------------------");
        boolean isFireChanged = fireDataLoader.loadData();
        log.info("----------------------------------------------------------------------------");
        boolean isCctvChanged = cctvDataLoader.loadData();
        log.info("----------------------------------------------------------------------------");
        boolean isAccidentStatsChanged = accidentStatsLoader.loadData();
        log.info("----------------------------------------------------------------------------");
        boolean isCrimeChanged = crimeStatsLoader.loadData();
        log.info("----------------------------------------------------------------------------");
        boolean isPopulationChanged = populationDataLoader.loadData();
        log.info("----------------------------------------------------------------------------");

        log.info("실시간 유저 검색용 데이터(사고 다발 지역) 적재 시작...");
        // 실시간 유저 검색용
        boolean isAccidentPointChanged = accidentPointLoader.loadData();
        log.info("----------------------------------------------------------------------------");

        // 기초 지역 교통사고 통계
        if (isAccidentStatsChanged || sggSafetyStatsRepository.existsByAccCntIsNull()) {
            log.info("[TAAS API] 교통사고 통계 데이터 갱신 필요. API 호출을 시작합니다...");
            accidentScoreService.updateAllAccidentScores();
        } else {
            log.info("[TAAS API] 모든 지역의 사고 데이터가 존재합니다. API 호출을 건너뜁니다.");
        }
        log.info("----------------------------------------------------------------------------");

        boolean isBaselineDataChanged = isRegionChanged || isPoliceChanged || isFireChanged ||
                isCctvChanged || isAccidentStatsChanged ||
                isCrimeChanged || isPopulationChanged;

        //최종 계산
        if (isBaselineDataChanged || regionRepository.existsBySafetyScoreIsNull()) {
            log.info("[Base Score] 점수 산출에 필요한 기초 데이터 변경 감지 -> 전체 안전 점수 재계산 시작");
            safetyScoreService.calculateAllRegionScores();
        } else {
            log.info("[Base Score] 점수 산출에 영향을 주는 데이터 변경이 없습니다. 계산을 건너뜁니다.");
        }

        log.info("============================================================================");
        log.info("전체 데이터 동기화 및 점수 산출 프로세스 종료");
        log.info("============================================================================");
    }

    private void prepareSpatialIndexes() {

        // 실행할 쿼리들을 배열로 관리
        String[] queries = {
                "CREATE SPATIAL INDEX idx_cctvs_geom ON cctvs(geometry)",
                "CREATE SPATIAL INDEX idx_police_geom ON police_stations(geometry)",
                "CREATE SPATIAL INDEX idx_fire_geom ON fire_stations(geometry)",
                "CREATE SPATIAL INDEX idx_region_geom ON safety_regions(geometry)"
        };

        // 쿼리를 하나씩 개별 실행하여 에러 발생 지점 특정
        for (String query : queries) {
            try {
                jdbcTemplate.execute(query);
                log.info("공간 인덱스 생성 완료: {}", query);
            } catch (org.springframework.dao.DataAccessException e) {
                String errorMessage = e.getMessage();
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    errorMessage += " | " + e.getCause().getMessage();
                }

                // 인덱스가 이미 만들어져 있는 경우 -> 에러가 아니므로 그냥 스킵
                if (errorMessage != null && errorMessage.contains("Duplicate key name")) {
                    log.debug("공간 인덱스가 이미 존재합니다. 스킵: {}", query);
                } else {
                    // 쿼리 오타나 DB 연결 문제 등 '진짜' DB 에러 -> 에러 로그 남기고 강제 중단
                    log.error("공간 인덱스 생성 중 DB 오류 발생. 쿼리: {}", query, e);
                    throw e;
                }
            } catch (Exception e) {
                // 그 외에 예상치 못한 에러가 묻히는 것을 방지하기 위해 강제 중단
                log.error("공간 인덱스 쿼리 실행 중 알 수 없는 치명적 오류 발생", e);
                throw e;
            }
        }
    }

}
