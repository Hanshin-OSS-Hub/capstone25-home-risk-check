package hanshin.home_risk_check.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
 * 분석 완료 시점의 위험도 + 안전등급 합본 스냅샷.
 * AnalysisHistory.resultJson 에 JSON으로 저장되고, 상세 조회 시 그대로 복원된다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalysisSnapshot(
     JeonseFraudResult jeonseFraud,
     SafetyScoreSnapshot safetyScore
) {}