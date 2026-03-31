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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegionSafetyScoreService {

    private final RegionRepository regionRepository;
    private final SggSafetyStatsRepository sggSafetyStatsRepository;
    private final CctvRepository cctvRepository;
    private final PoliceStationRepository policeStationRepository;
    private final FireStationRepository fireStationRepository;

    //  인프라 가중치 설정
    private static final double CCTV_WEIGHT = 1.0;     // CCTV 1대 = 1점
    private static final double POLICE_WEIGHT = 100.0; // 경찰서 1개 = 100점의 방어력
    private static final double FIRE_WEIGHT = 50.0;    // 소방서 1개 = 50점의 방어력



    @Transactional
    public void calculateAllRegionScores() {
        List<Region> regions = regionRepository.findAll();
        log.info("regions 조회 완료");

        if (regions.isEmpty()) {
            return;
        }
        // 범죄/사고 및 인구 데이터 캐싱
        Map<String, SggSafetyStats> sggStatsMap = sggSafetyStatsRepository.findAll().stream()
                .filter(s -> s.getSgisCode() != null)
                .collect(Collectors.toMap(SggSafetyStats::getSgisCode, s -> s, (existing, replacement) -> existing));
        log.info("sggStatsMap 조회 완료");

        // 시군구 인구수
        // 8자리 Region 코드를 5자리SGG로 잘라서 조회 -> 시군구 전체 인구/면적 산출
        Map<String, Integer> sggPopulationMap = regions.stream()
                .filter(r -> r.getSgisCode() != null && r.getSgisCode().length() >= 5)
                .collect(Collectors.groupingBy(
                        r -> extractSggCode(r.getSgisCode()),
                        Collectors.summingInt(r -> r.getPopulation() != null && r.getPopulation() > 0 ? r.getPopulation() : 0)));
        log.info("sggPopulationMap 조회 완료");

        Map<String, Double> sggAreaMap = regions.stream()
                .filter(r -> r.getSgisCode() != null && r.getSgisCode().length() >= 5)
                .collect(Collectors.groupingBy(
                        r -> extractSggCode(r.getSgisCode()),
                        Collectors.summingDouble(r -> r.getGeometry() != null ? r.getGeometry().getArea() : 0.0)));
        log.info("sggAreaMap 캐싱 완료");

        //  CCTV 데이터 캐싱
        Map<String, Integer> cctvMap = cctvRepository.sumCameraCountGroupedBySgisCode().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> row[1] != null ? ((Number) row[1]).intValue() : 0));
        log.info("cctvMap 캐싱 완료");

        Map<String, Integer> policeMap = policeStationRepository.countAllGroupedBySgisCode().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).intValue()));
        log.info("policeMap 캐싱 완료");

        Map<String, Integer> fireMap = fireStationRepository.countAllGroupedBySgisCode().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).intValue()));
        log.info("fireMap 캐싱 완료");

        // score 계산 (정규화 계산을 위해 리스트로 저장)
        List<Double> crimeRawScores = new java.util.ArrayList<>();
        List<Double> infraRawScores = new java.util.ArrayList<>();
        List<Double> accidentRawScores = new java.util.ArrayList<>();

        log.info("전국 안전점수 계산 시작");
        for (Region region : regions) {

            crimeRawScores.add(calculateCrimeRaw(region,sggStatsMap,sggPopulationMap, sggAreaMap));
            infraRawScores.add(calculateInfraRaw(region,cctvMap, policeMap, fireMap));
            accidentRawScores.add(calculateAccidentRaw(region, sggStatsMap, sggPopulationMap, sggAreaMap));
        }
        log.info(" 모든 점수 계산 완료. 통계 산출 및 최종 점수 업데이트 중...");

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

        // 지역별 Z-Score 합산 점수
        List<Double> finalCrimeZ = new java.util.ArrayList<>();
        List<Double> finalInfraZ = new java.util.ArrayList<>();
        List<Double> finalAccidentZ = new java.util.ArrayList<>();
        List<Double> combinedZScores = new java.util.ArrayList<>();

        // 정규화 및 최종 점수 산출
        for (int i = 0; i < regions.size(); i++) {
            Region region = regions.get(i);

            // Z-SCORE 계산
            double crimeZ = calculateZScore(winsorizedCrime.get(i), crimeMean, crimeStd);
            double infraZ = calculateZScore(winsorizedInfra.get(i), infraMean, infraStd);
            double accidentZ = calculateZScore(winsorizedAccident.get(i), accidentMean, accidentStd);

            // 범죄 5 사고 3 인프라 2 가중치 적용
            // 안전할수록 점수가 높도록
            double rawScore = (infraZ * 0.2)
                                - (crimeZ * 0.5)
                                -(accidentZ * 0.3);

            // Min-Max 정규화를 위해 저장
            finalCrimeZ.add(crimeZ);
            finalInfraZ.add(infraZ);
            finalAccidentZ.add(accidentZ);
            combinedZScores.add(rawScore);

        }
        // Min-Max 정규화를 위한 최대값, 최소값 추출
        double maxZ = java.util.Collections.max(combinedZScores);
        double minZ = java.util.Collections.min(combinedZScores);

        // 점수 0~100점 사이로 배치
        for (int i = 0; i < regions.size(); i++) {
            Region region = regions.get(i);
            double currentZ = combinedZScores.get(i);


            // Min-Max 정규화
            double finalScore;
            if (maxZ == minZ){
                finalScore = 50.0; // 모든 지역 안전 점수가 같다면 중간값 50점으로 처리
            } else {
                finalScore = ((currentZ - minZ) / (maxZ - minZ)) * 100.0;
            }

            // 소수점 둘째자리까지만 표기
            finalScore = Math.round(finalScore * 100.0) / 100.0;

            // Region 엔티티 업데이트 로직
            region.updateScores(
                    finalCrimeZ.get(i),
                    finalAccidentZ.get(i),
                    finalInfraZ.get(i),
                    finalScore
            );

        }
    }

    //  범죄 지표 (시군구 데이터를 행정동으로 분배)
    private double calculateCrimeRaw(Region region, Map<String, SggSafetyStats> sggStatsMap,
                                     Map<String, Integer> sggPopulationMap, Map<String, Double> sggAreaMap) {

        String sgisCode = region.getSgisCode();

        if (sgisCode == null || sgisCode.length() < 5) {
            return 0.0;
        }

        // Region의 sgisCode의 앞 5자리를 추출하여 시군구 코드로 사용
        String sggCode = extractSggCode(sgisCode);
        SggSafetyStats sggStats = sggStatsMap.get(sggCode);

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
        Integer sggPopSum = sggPopulationMap.getOrDefault(sggCode, 1);
        double sggTotalPopulation = sggPopSum > 0 ? sggPopSum : 1.0;

        Double sggAreaSum = sggAreaMap.getOrDefault(sggCode, 1.0);
        double sggTotalArea = sggAreaSum > 0 ? sggAreaSum : 1.0;

        double crimePerCapita = totalSggCrime / sggTotalPopulation;
        double crimeDensity = totalSggCrime / sggTotalArea;

        return (crimePerCapita * 0.6) + (crimeDensity * 0.4);
    }

    // 인프라 지표 계산
    private double calculateInfraRaw(Region region, Map<String, Integer> cctvMap,
                                     Map<String, Integer> policeMap, Map<String, Integer> fireMap) {

        String sgisCode = region.getSgisCode();
        if (sgisCode == null) return 0.0;

        // 구역 내 경찰서 개수
        double policeCount = policeMap.getOrDefault(sgisCode, 0);
        // 구역 내 소방서 개수
        double fireCount = fireMap.getOrDefault(sgisCode, 0);
        // 구역 내 CCTV 카메라 총 대수
        double cctvCount = cctvMap.getOrDefault(sgisCode, 0);

        // 가중치를 적용한 인프라 방어력 합산
        double totalInfraPower = (cctvCount * CCTV_WEIGHT)
                + (policeCount * POLICE_WEIGHT)
                + (fireCount * FIRE_WEIGHT);

        double area = region.getGeometry() != null ? region.getGeometry().getArea() : 0.0;

        //단위 면적당 인프라가 얼마나 촘촘한가
        return area > 0 ? totalInfraPower / area : 0.0;
    }

    // 사고 지표 계산
    private double calculateAccidentRaw(Region region, Map<String, SggSafetyStats> sggStatsMap,
                                        Map<String, Integer> sggPopulationMap, Map<String, Double> sggAreaMap) {

        String sgisCode = region.getSgisCode();
        if (sgisCode == null) {
            return 0.0;
        }

        String sggCode = extractSggCode(sgisCode);
        SggSafetyStats sggStats = sggStatsMap.get(sggCode);

        if (sggStats == null || sggStats.getAccCnt() == null) {
            return 0.0;
        }

        // 시군구 전체 일반 사고 건수
        double totalSggAccident = sggStats.getAccCnt();

        // 시군구 총 인구
        Integer sggPopSum = sggPopulationMap.getOrDefault(sggCode, 1);
        double sggTotalPopulation = sggPopSum > 0 ? sggPopSum : 1.0;

        // 시군구 총 면적
        Double sggAreaSum = sggAreaMap.getOrDefault(sggCode, 1.0);
        double sggTotalArea = sggAreaSum > 0 ? sggAreaSum : 1.0;

        // 인구 대비 사고율 & 면적 대비 사고밀도 계산
        double accidentPerCapita = totalSggAccident / sggTotalPopulation;
        double accidentDensity = totalSggAccident / sggTotalArea;

        // 3. 인구 대비 사고율(60%) + 면적 대비 사고밀도(40%) 계산
        return (accidentPerCapita * 0.6) + (accidentDensity * 0.4);
    }


    // SGIS 코드에서 시군구 5자리 코드 추출 (예외처리 포함)
    private String extractSggCode(String sgisCode) {
        if (sgisCode == null || sgisCode.length() < 5) {
            return null;
        }

        String sggCode = sgisCode.substring(0, 5);

        //부천시 예외 처리: 31051(원미), 31052(소사), 31053(오정) 등은 모두 31050으로 통일
        if (sggCode.startsWith("3105")) {
            return "31050";
        }

        return sggCode;
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


}
