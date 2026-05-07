package hanshin.home_risk_check.safetyscore.domain.score.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.jpa.repository.Meta;

@Getter
@Builder
public class SafetyScoreResponse {

    private Meta meta;
    private Data data;

    @Getter
    @Builder
    public static class Meta {
        private int code;
        private String message;
    }

    @Getter
    @Builder
    public static class Data {// 최종 점수
        private int finalSafetyScore;

        // 동네 기본 점수
        private String regionName; // 행정동 이름
        private double regionBaseScore; //해당 행정동의 기본 안전 점수

        private int nearbyCctvCount; //반경 500m 내 CCTV 카메라 총 대수
        private int nearbyPoliceCount; //반경 500m 내 경찰관서 개수
        private int nearbyFireCount; //반경 500m 내 소방서 개수
        private boolean isAccidentHotspot; // 반경 500m 내 교통사고 다발 구역 포함 여부
        private int accidentHotspotCount; // 반경 500m 내 교통사고 다발 구역 개수

        private double cctvDensityRatio; // 우리 동네 평균 대비 CCTV 밀집도 비율}
    }
}
