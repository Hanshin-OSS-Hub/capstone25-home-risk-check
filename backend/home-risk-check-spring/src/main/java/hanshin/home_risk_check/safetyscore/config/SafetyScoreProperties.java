package hanshin.home_risk_check.safetyscore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("safetyscore")
public record SafetyScoreProperties(
        double radius,           // 반경 (500.0)
        double hotspotWeight,    // 핫스팟 가중치 (4.5)
        double densityWeight,    // 밀도 가중치 (5.0)
        double policeScore,      // 경찰서 가점 (5.0)
        double fireScore,        // 소방서 가점 (2.0)
        Weights weights          // 0.2 / 0.5 / 0.3 가중치
) {
    public record Weights(
            double cctv,
            double police,
            double lighting
    ) {}
}
