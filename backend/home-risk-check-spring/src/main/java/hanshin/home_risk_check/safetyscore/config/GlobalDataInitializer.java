package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.config;


import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.config.loader.*;
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
    private final TrafficDataLoader trafficDataLoader;


    @Override
    public void run(String... args) throws Exception {

        // 행정동 데이터 로드
        regionDataLoader.loadData();

        // 경찰관서 데이터 로드
        policeDataLoader.loadData();

        // CCTV 데이터 로드
        cctvDataLoader.loadData();

        //교통사고 데이터 로드
        trafficDataLoader.loadData();

    }
}
