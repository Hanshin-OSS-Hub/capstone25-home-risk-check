package hanshin.home_risk_check.safetyscore.domain.region.service;

import hanshin.home_risk_check.safetyscore.domain.accident.repository.TrafficRepository;
import hanshin.home_risk_check.safetyscore.domain.cctv.repository.CctvRepository;
import hanshin.home_risk_check.safetyscore.domain.fire.repository.FireStationRepository;
import hanshin.home_risk_check.safetyscore.domain.police.repository.PoliceStationRepository;
import hanshin.home_risk_check.safetyscore.domain.region.entity.Region;
import hanshin.home_risk_check.safetyscore.domain.region.entity.SggSafetyStats;
import hanshin.home_risk_check.safetyscore.domain.region.repository.RegionRepository;
import hanshin.home_risk_check.safetyscore.domain.region.repository.SggSafetyStatsRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionSafetyScoreService {

    private final RegionRepository regionRepository;
    private final SggSafetyStatsRepository sggSafetyStatsRepository;
    private final CctvRepository cctvRepository;
    private final TrafficRepository trafficRepository;
    private final PoliceStationRepository policeStationRepository;
    private final FireStationRepository fireStationRepository;

    //  인프라 가중치 설정
    private static final double CCTV_WEIGHT = 1.0;     // CCTV 1대 = 1점
    private static final double POLICE_WEIGHT = 100.0; // 경찰서 1개 = 100점의 방어력
    private static final double FIRE_WEIGHT = 50.0;    // 소방서 1개 = 50점의 방어력

    @Transactional
    public void calculateAllRegionScores() {
        List<Region> regions = regionRepository.findAll();

        if (regions.isEmpty()) {
            return;
        }

        // score 계산 (정규화 계산을 위해 리스트로 저장)
        List<Double> crimeRawScores = regions.stream().map(this::calculateCrimeRaw).toList();
        List<Double> infraRawScores = regions.stream().map(this::calculateInfraRaw).toList();
        List<Double> accidentRawScores = regions.stream().map(this::calculateAccidentRaw).toList();

        // 이상치 제어 (하위5%와 상위5%를 임계값으로 설정하여 너무 튀는 점수 제어) 지역간 점수 변별력 사라지는 것 방지
        List<Double> winsorizedCrime = applyWinsorizing(crimeRawScores, 0.05);
        List<Double> winsorizedInfra = applyWinsorizing(infraRawScores, 0.05);
        List<Double> winsorizedAccident = applyWinsorizing(accidentRawScores, 0.05);

        // 통계 산출 (평균, 표준편차)
        double crimeMean = calculateMean(winsorizedCrime);
        double crimeStd = calculateStdDev(winsorizedCrime, crimeMean);

        double infraMean = calculateMean(winsorizedInfra);
        double infraStd = calculateStdDev(winsorizedInfra, infraMean);

        double accidentMean = calculateMean(winsorizedAccident);
        double accidentStd = calculateStdDev(winsorizedAccident, accidentMean);

        // 정규화 및 최종 점수 산출
        for (int i = 0; i < regions.size(); i++) {
            Region region = regions.get(i);

            // Z-SCORE 계산
            double crimeZ = calculateZScore(winsorizedCrime.get(i), crimeMean, crimeStd);
            double infraZ = calculateZScore(winsorizedInfra.get(i), infraMean, infraStd);
            double accidentZ = calculateZScore(winsorizedAccident.get(i), accidentMean, accidentStd);

            // 범죄 5 사고 3 인프라 2 가중치 적용
            double weightedCrimeZ = crimeZ * 0.5;
            double weightedAccidentZ = accidentZ * 0.3;
            double weightedInfraZ = infraZ * 0.2;

            // 안전할수록 점수가 높도록
            double finalScoreZ = weightedInfraZ - (weightedCrimeZ + weightedAccidentZ);

            // 표준점수로 변환(음수 점수 방지) Z-score가 너무 작은것을 생각해 * 30으로 골고루 퍼지게 함
            double finalScore = (finalScoreZ * 30) + 50;

            // Region 엔티티 업데이트 로직
            region.updateScores(crimeZ, accidentZ, infraZ, finalScore);
        }
    }

    //  범죄 지표 (시군구 데이터를 행정동으로 분배)
    private double calculateCrimeRaw(Region region) {
        SggSafetyStats sggStats = sggSafetyStatsRepository.findBySidoNmAndSggNm(region.getSidoNm(), region.getSggNm())
                .orElse(null);

        if (sggStats == null) {
            return 0.0;
        }

        // 시군구 총 5대 범죄 합산
        double totalSggCrime = (sggStats.getRobberyCnt() != null ? sggStats.getRobberyCnt() : 0) +
                (sggStats.getTheftCnt() != null ? sggStats.getTheftCnt() : 0) +
                (sggStats.getMurderCnt() != null ? sggStats.getMurderCnt() : 0) +
                (sggStats.getSexualCrimeCnt() != null ? sggStats.getSexualCrimeCnt() : 0) +
                (sggStats.getViolenceCnt() != null ? sggStats.getViolenceCnt() : 0);

        //시군구 총인구수 조회
        Integer sggPopSum = regionRepository.sumPopulationBySidoNmAndSggNm(region.getSidoNm(), region.getSggNm());
        double sggTotalPopulation = (sggPopSum != null && sggPopSum > 0) ? sggPopSum : 1.0; //ex 분당구 인구수

        // 인구 비율을 바탕으로 행정동 범죄 건수 추정
        double regionPopulation = (region.getPopulation() != null && region.getPopulation() > 0)
                ? region.getPopulation() : 1.0; // ex) 정자동의 인구수
        double estimatedRegionCrime = totalSggCrime * (regionPopulation / sggTotalPopulation); // 성남시 범죄율 * (정자동 인구/ 분당구 인구)
        double area = region.getGeometry().getArea();

        // (건수/인구 * 0.6) + (건수/면적 * 0.4)
        return (estimatedRegionCrime / regionPopulation* 0.6)
                + (estimatedRegionCrime / area * 0.4);
    }

    // 인프라 지표 계산
    private double calculateInfraRaw(Region region) {
        MultiPolygon regionGeom = region.getGeometry();

        if (regionGeom == null) {
            return 0.0;
        }

        // 구역 내 CCTV 카메라 총 대수 (null 처리)
        Integer cctvSum = cctvRepository.sumCameraCountInRegion(regionGeom);
        double cctvCount = cctvSum != null ? cctvSum : 0.0;

        // 구역 내 경찰서 개수 (null 처리)
        Integer policeSum = policeStationRepository.countPoliceStationsInRegion(regionGeom);
        double policeCount = policeSum != null ? policeSum : 0.0;

        // 구역 내 소방서 개수 (null 처리)
        Integer fireSum = fireStationRepository.countFireStationsInRegion(regionGeom);
        double fireCount = fireSum != null ? fireSum : 0.0;

        // 가중치를 적용한 인프라 방어력 합산
        double totalInfraPower = (cctvCount * CCTV_WEIGHT)
                + (policeCount * POLICE_WEIGHT)
                + (fireCount * FIRE_WEIGHT);

        double area = regionGeom.getArea();

        //단위 면적당 인프라가 얼마나 촘촘한가
        return area > 0 ? totalInfraPower / area : 0.0;
    }

    // 사고 지표 계산
    private double calculateAccidentRaw(Region region) {
        //  TrafficRepository를 통해 행정동 코드(admCode)로 사고 건수 합산
        Integer accSum = trafficRepository.sumAccidentCountByAdmCode(region.getAdmCode());
        double accidentCount = accSum != null ? accSum : 0.0;

        //  면적과 인구수 가져오기 (Null 및 0 방어 로직 포함)
        double area = region.getGeometry() != null ? region.getGeometry().getArea() : 0.0;
        double population = (region.getPopulation() != null && region.getPopulation() > 0)
                ? region.getPopulation() : 1.0;

        // 면적이 0이하인 비정상 데이터인 경우 계산 불가 처리
        if (area <= 0) {
            return 0.0;
        }

        // 3. 인구 대비 사고율(60%) + 면적 대비 사고밀도(40%) 계산
        return (accidentCount / population * 0.6) + (accidentCount / area * 0.4);
    }

    // 윈저라이징(이상치 제어)
    private List<Double> applyWinsorizing(List<Double> scores, double limitPercent) {
        if (scores == null || scores.isEmpty()) {
            return scores;
        }

        // 원본 데이터를 보호하기 위해 복사본 생성 후 정렬
        List<Double> sortedScores = new java.util.ArrayList<>(scores);
        java.util.Collections.sort(sortedScores);

        // 상/하위 %에 해당하는 인덱스 계산
        int lowerIndex = (int) Math.max(0, Math.floor(scores.size() * limitPercent));
        int upperIndex = (int) Math.min(scores.size() - 1, Math.ceil(scores.size() * (1 - limitPercent)) - 1);

        // 임계값 추출
        double lowerBound = sortedScores.get(lowerIndex);
        double upperBound = sortedScores.get(upperIndex);

        // 원본 리스트의 순서를 유지하면서 값만 임계값으로 제한
        return scores.stream()
                .map(score -> Math.max(lowerBound, Math.min(upperBound, score)))
                .toList();
    }

    // 평균 산출
    private double calculateMean(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    // 표준편차 산출
    private double calculateStdDev(List<Double> values, double mean) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        double variance = values.stream()
                .mapToDouble(val -> Math.pow(val - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }

    // Z-score계산
    private double calculateZScore(double value, double mean, double stdDev) {
        // 모든 지역의 점수가 똑같아서 표준편차가 0이 될 경우 방어
        return stdDev == 0 ? 0.0 : (value - mean) / stdDev;
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> debugSingleRegionScore(String admCode) {
        Region region = regionRepository.findByAdmCode(admCode)
                .orElseThrow(() -> new RuntimeException("지역을 찾을 수 없습니다: " + admCode));

        // 1. Raw 점수 계산 과정 수동 호출
        double crimeRaw = calculateCrimeRaw(region);
        double infraRaw = calculateInfraRaw(region);
        double accidentRaw = calculateAccidentRaw(region);

        // 2. 결과 조립 (화면에 JSON 형태로 출력됨)
        java.util.Map<String, Object> debugInfo = new java.util.HashMap<>();
        debugInfo.put("regionName", region.getAdmNm());
        debugInfo.put("population", region.getPopulation());
        debugInfo.put("area", region.getGeometry().getArea());

        debugInfo.put("rawScores", java.util.Map.of(
                "crime", crimeRaw,
                "infra", infraRaw,
                "accident", accidentRaw
        ));

        debugInfo.put("message", "이 값은 Z-Score 변환 전의 순수 계산값(Raw Score)입니다.");

        return debugInfo;
    }

}
