package hanshin.home_risk_check.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/*
 * 전세사기 위험도 분석 결과.
 * FastAPI predict 결과(result)와 매핑.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JeonseFraudResult(
    String address,
    long deposit,
    long marketPrice,
    String priceSource,
    int riskScore,
    String riskLevel,
    List<JeonseRiskFactor> majorJeonseRiskFactors,
    JeonseHugResult jeonseHugResult,
    JeonseRiskDetails details,
    List<String> recommendations,
    JeonseScoringDetail jeonseScoringDetail
) {}