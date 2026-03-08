package hanshin.home_risk_check.safetyscore.config;



import hanshin.home_risk_check.safetyscore.config.loader.*;
import hanshin.home_risk_check.safetyscore.domain.accident.service.AccidentScoreService;
import hanshin.home_risk_check.safetyscore.domain.population.service.PopulationService;
import hanshin.home_risk_check.safetyscore.domain.region.repository.RegionRepository;
import hanshin.home_risk_check.safetyscore.domain.region.repository.SggSafetyStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalDataInitializer implements CommandLineRunner {

    private final RegionDataLoader regionDataLoader;
    private final PoliceDataLoader policeDataLoader;
    private final CctvDataLoader cctvDataLoader;
    private final AccidentPointLoader accidentPointLoader;
    private final AccidentStatsLoader accidentStatsLoader;
    private final CrimeStatsLoader crimeStatsLoader;
    private final FireStationDataLoader fireStationDataLoader;

    private final AccidentScoreService accidentScoreService;
    private final PopulationService populationService;

    private final SggSafetyStatsRepository sggSafetyStatsRepository;
    private final RegionRepository regionRepository;



    @Override
    public void run(String... args) throws Exception {

        // 행정동 데이터 로드
        regionDataLoader.loadData();

        // 경찰관서 데이터 로드
        policeDataLoader.loadData();

        // 소방서 데이터 로드
        fireStationDataLoader.loadData();;

        // CCTV 데이터 로드
        cctvDataLoader.loadData();

        //교통사고 다발지역 데이터 로드
        accidentPointLoader.loadData();

        // 기초 지역코드
        boolean isAccidentFileChanged = accidentStatsLoader.loadData();

        // 기초 지역 교통사고 통계
        if (isAccidentFileChanged || sggSafetyStatsRepository.existsByAccCntIsNull()) {
            log.info("교통사고 통계 CSV 파일이 변경되었거나, 비어있는 데이터가 있어 TAAS API 호출을 시작합니다.");
            accidentScoreService.updateAllAccidentScores();
        } else {
            log.info("모든 지역의 사고 데이터가 존재합니다. TAAS API 호출을 건너뜁니다.");
        }

        // 인구 통계 데이터 수집
        if (regionRepository.existsByPopulationIsNull()) {
            log.info("비어있는 인구 데이터가 감지되어 공공데이터 API 호출을 시작합니다.");
            populationService.updateAllMissingPopulation();
        } else {
            log.info("모든 지역의 인구 데이터가 이미 존재합니다. API 호출을 건너뜁니다.");
        }

        // 범죄 통계 데이터 로드
        crimeStatsLoader.loadData();
    }
}
