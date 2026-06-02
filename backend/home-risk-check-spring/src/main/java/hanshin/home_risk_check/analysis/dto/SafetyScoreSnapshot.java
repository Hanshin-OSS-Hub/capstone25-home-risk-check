package hanshin.home_risk_check.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
 * 지역 안전등급 스냅샷.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SafetyScoreSnapshot(
    int finalSafetyScore,
    String regionName,
    double regionBaseScore,
    int nearbyCctvCount,
    int nearbyPoliceCount,
    int nearbyFireCount,
    boolean accidentHotspot,
    int accidentHotspotCount,
    double cctvDensityRatio
) {}