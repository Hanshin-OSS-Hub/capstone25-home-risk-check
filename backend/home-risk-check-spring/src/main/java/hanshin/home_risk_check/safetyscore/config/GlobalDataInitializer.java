package hanshin.home_risk_check.safetyscore.config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalDataInitializer implements CommandLineRunner {

    private final DataSyncCoordinator dataSyncCoordinator;

    @Override
    public void run(String... args) throws Exception {

    dataSyncCoordinator.syncAndRecalculate();

    }
}
