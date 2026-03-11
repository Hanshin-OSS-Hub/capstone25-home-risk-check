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

    // --- 인프라 가중치 설정 (기획에 따라 자유롭게 조절 가능) ---
    private static final double CCTV_WEIGHT = 1.0;     // CCTV 1대 = 1점
    private static final double POLICE_WEIGHT = 100.0; // 경찰서 1개 = 100점의 방어력
    private static final double FIRE_WEIGHT = 50.0;    // 소방서 1개 = 50점의 방어력

    @Transactional
    public void calculateAllRegionScores() {
        List<Region> regions = regionRepository.findAll();

        if (regions.isEmpty()) {
            return;
        }

        // score 계산
        List<Double> crimeRawScores = regions.stream().map(this::calculateCrimeRaw).toList();
        List<Double> infraRawScores = regions.stream().map(this::calculateInfraRaw).toList();
        List<Double> accidentRawScores = regions.stream().map(this::calculateAccidentRaw).toList();

        // 이상치 제어
        List<Double> winsorizedCrime = applyWinsorizing(crimeRawScores, 0.05);
        List<Double> winsorizedInfra = applyWinsorizing(infraRawScores, 0.05);
        List<Double> winsorizedAccident = applyWinsorizing(accidentRawScores, 0.05);

        // 통계 산출 (평균, 표준편차)
        double crimeMean = calculateMean(crimeRawScores);
        double crimeStd = calculateStdDev(crimeRawScores, crimeMean);

        double infraMean = calculateMean(infraRawScores);
        double infraStd = calculateStdDev(infraRawScores, infraMean);

        double accidentMean = calculateMean(accidentRawScores);
        double accidentStd = calculateStdDev(accidentRawScores, accidentMean);

        //  Z-Score 표준화 및 최종 점수 산출
        for (int i = 0; i < regions.size(); i++) {
            Region region = regions.get(i);

            // Z-SCORE 계산
            double crimeZ = calculateZScore(crimeRawScores.get(i), crimeMean, crimeStd);
            double infraZ = calculateZScore(infraRawScores.get(i), infraMean, infraStd);
            double accidentZ = calculateZScore(accidentRawScores.get(i), accidentMean, accidentStd);

            // 최종 수식: 위해(범죄+사고) - 경감(인프라)
            double finalScore = (crimeZ + accidentZ) - infraZ;

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
        double sggTotalPopulation = (sggPopSum != null && sggPopSum > 0) ? sggPopSum : 1.0;

        // 인구 비율을 바탕으로 행정동 범죄 건수 추정
        double regionPopulation = region.getPopulation() != null ? region.getPopulation() : 0.0;
        double estimatedRegionCrime = totalSggCrime * (regionPopulation / sggTotalPopulation);
        double area = region.getGeometry().getArea();

        // (건수/인구 * 0.6) + (건수/면적 * 0.4)
        return (estimatedRegionCrime / (regionPopulation > 0 ? regionPopulation : 1.0) * 0.6)
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


}
