package hanshin.home_risk_check.riskanalysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/*
 * 전세사기 위험도 분석 결과.
 * FastAPI predict 결과(result)와 매핑.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RiskAnalysisResult(
    String address,
    long deposit,
    long marketPrice,
    String priceSource,
    int riskScore,
    String riskLevel,
    List<RiskFactor> majorRiskFactors,
    HugResult hugResult,
    RiskDetails details,
    List<String> recommendations,
    ScoringDetail scoringDetail
) {}