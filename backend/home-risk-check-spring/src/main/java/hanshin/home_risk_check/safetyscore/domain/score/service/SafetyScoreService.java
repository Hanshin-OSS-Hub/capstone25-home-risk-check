package hanshin.home_risk_check.safetyscore.domain.score.service;

import hanshin.home_risk_check.safetyscore.domain.accident.repository.TrafficRepository;
import hanshin.home_risk_check.safetyscore.domain.cctv.repository.CctvRepository;
import hanshin.home_risk_check.safetyscore.domain.fire.repository.FireStationRepository;
import hanshin.home_risk_check.safetyscore.domain.police.repository.PoliceStationRepository;
import hanshin.home_risk_check.safetyscore.domain.region.entity.Region;
import hanshin.home_risk_check.safetyscore.domain.region.repository.RegionRepository;
import hanshin.home_risk_check.safetyscore.domain.score.dto.SafetyScoreResponse;
import hanshin.home_risk_check.safetyscore.infra.api.KakaoApiCaller;
import hanshin.home_risk_check.safetyscore.infra.dto.KakaoApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SafetyScoreService {

    private final KakaoApiCaller kakaoApiCaller;
    private final RegionRepository regionRepository;
    private final CctvRepository cctvRepository;
    private final PoliceStationRepository policeStationRepository;
    private final FireStationRepository fireStationRepository;
    private final TrafficRepository trafficRepository;

    /**
     * 교통사고 다발지역(핫스팟) 밀집도 가중치 = 4.5
     * - DB 통계상 다발지역 1곳 지정 시 평균 4.47건의 사고가 응집되어 발생.
     *  - 다발지역이 존재한다는 것 자체로 평균 4.5건의 집중된 구조적 위험이
     *   있다는 논리적 근거에 기반하여 페널티 부여.
     **/
    private static final double HOTSPOT_STRUCTURAL_WEIGHT = 4.5;

    public SafetyScoreResponse calculateSafetyScore(String address) {

        //카카오 API를 통해 주소 -> 좌표 및 행정동 코드 반환
        KakaoApiResponse.KakaoDocument document = kakaoApiCaller.searchAddress(address);

        if (document == null || document.getAddress() == null) {
            throw new IllegalArgumentException("주소를 좌표로 변환할 수 없거나 행정동 정보가 없습니다: " + address);
        }

        double lon = Double.parseDouble(document.getX());
        double lat = Double.parseDouble(document.getY());
        String admNm = document.getAddress().getRegion_3depth_h_name();

        //카카오 api를 통해 받은 좌표로 Region을 찾아옴
        Region region = regionRepository.findByLocation(lon,lat)
                .orElseThrow(() -> new IllegalArgumentException("해당 위치(" + admNm + ")의 공간 데이터를 찾을 수 없습니다."));

        // 찾은 Region에서 Sgis 코드를 찾아옴
        String sgisCode = region.getSgisCode();

        // 집 주변 500m 인프라 수집
        int localCctv = cctvRepository.sumCameraCountWithinRadius(lat, lon, 500.0);
        int localPolice = policeStationRepository.countPoliceWithinRadius(lat, lon, 500.0);
        int localFire = fireStationRepository.countFireStationsWithinRadius(lat, lon, 500.0);
        int hotspotCount = trafficRepository.countAccidentAreaWithinRadius(lat, lon, 500.0);

        // 동네 기본 점수 가져오기
        double score = region.getSafetyScore();

        //행정동 면적 대비 cctv 개수 비율
        Double regionArea = regionRepository.getAreaBySgisCode(sgisCode); // ST_Area(ST_Transform(geometry, 3857))
        Integer totalCctvInRegion = cctvRepository.sumCameraCountBySgisCode(sgisCode);

        double densityRatio = 0.0;

        if (regionArea != null && totalCctvInRegion != null && totalCctvInRegion > 0) {
            double regionDensity = totalCctvInRegion / regionArea; // 동네 평균 밀도
            double targetArea = Math.PI * Math.pow(500, 2);       // 반경 500m 면적
            double targetDensity = (double) localCctv / targetArea; // 내 주변 밀도

            densityRatio = targetDensity / regionDensity; // 내 주변 CCTV 밀도 / 동네 평균 밀도
            double densityScore = 0.0;
            // 평균보다 밀도가 높으면 가점, 낮으면 감점 (최대 +-10점 범위)
            if (densityRatio == 0) {
                densityScore = -10.0; // 아예 없으면 확실하게 -10점 타격
            } else {
                // 0.1 더하는 꼼수 제거! 순수하게 1배일 때 딱 0점이 됨 (log(1) = 0)
                densityScore = Math.log(densityRatio) * 5.0; // 가중치를 4.0에서 5.0으로 살짝 올림
            }
            // 캡핑 씌우기 (-10 ~ +10)
            score += Math.max(-10.0, Math.min(10.0, densityScore));
        }

        // 경찰서 / 소방서가 500m 안에 있으면 가점
        if (localPolice > 0) score += 5.0;
        if (localFire > 0) score += 2.0;

        // 교통사고 다발지역이 근처에 있으면 감점
        if (hotspotCount > 0) {
            score -= (hotspotCount * HOTSPOT_STRUCTURAL_WEIGHT);
        }

        // 점수가 0 미만이거나 100을 초과하지 않도록 보정
        int finalScore = (int) Math.max(0, Math.min(100, Math.round(score)));

        SafetyScoreResponse.Data responseData = SafetyScoreResponse.Data.builder()
                .finalSafetyScore(finalScore)
                .regionName(admNm)
                .regionBaseScore(region.getSafetyScore())
                .nearbyCctvCount(localCctv)
                .nearbyPoliceCount(localPolice)
                .nearbyFireCount(localFire)
                .accidentHotspotCount(hotspotCount)
                .isAccidentHotspot(hotspotCount > 0)
                .cctvDensityRatio(densityRatio)
                .build();

        SafetyScoreResponse.Meta responseMeta = SafetyScoreResponse.Meta.builder()
                .code(200)
                .message("success")
                .build();

        return SafetyScoreResponse.builder()
                .meta(responseMeta)
                .data(responseData)
                .build();
    }
}
