package hanshin.home_risk_check.safetyscore.domain.region.service;

import hanshin.home_risk_check.safetyscore.domain.cctv.repository.CctvRepository;
import hanshin.home_risk_check.safetyscore.domain.fire.repository.FireStationRepository;
import hanshin.home_risk_check.safetyscore.domain.police.repository.PoliceStationRepository;
import hanshin.home_risk_check.safetyscore.domain.region.entity.Region;
import hanshin.home_risk_check.safetyscore.domain.region.entity.SggSafetyStats;
import hanshin.home_risk_check.safetyscore.domain.region.repository.RegionRepository;
import hanshin.home_risk_check.safetyscore.domain.region.repository.SggSafetyStatsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.locationtech.jts.geom.MultiPolygon;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionSafetyScoreServiceTest {

    @InjectMocks
    private RegionSafetyScoreService regionSafetyScoreService;

    @Mock
    private RegionRepository regionRepository;
    @Mock private SggSafetyStatsRepository sggSafetyStatsRepository;
    @Mock private CctvRepository cctvRepository;
    @Mock private PoliceStationRepository policeStationRepository;
    @Mock private FireStationRepository fireStationRepository;

    @Test
    @DisplayName("전국 점수 계산 시, 가장 안전한 지역은 100점, 가장 위험한 지역은 0점으로 정규화되어 업데이트된다.")
    void calculateAllRegionScores_Success() {
        // 100점 마을
        Region safeRegion = mock(Region.class);
        MultiPolygon safeGeom = mock(MultiPolygon.class);
        when(safeGeom.getArea()).thenReturn(10000.0); // 면적 세팅
        when(safeRegion.getSgisCode()).thenReturn("11111001");
        when(safeRegion.getPopulation()).thenReturn(1000);
        when(safeRegion.getGeometry()).thenReturn(safeGeom);

        // 0점 마을
        Region dangerRegion = mock(Region.class);
        MultiPolygon dangerGeom = mock(MultiPolygon.class);
        when(dangerGeom.getArea()).thenReturn(10000.0);
        when(dangerRegion.getSgisCode()).thenReturn("22222001");
        when(dangerRegion.getPopulation()).thenReturn(1000);
        when(dangerRegion.getGeometry()).thenReturn(dangerGeom);

        // Region Repository 응답 세팅
        when(regionRepository.findAll()).thenReturn(Arrays.asList(safeRegion, dangerRegion));

        // --- 통계 데이터(범죄, 사고) 세팅 ---
        SggSafetyStats safeStats = mock(SggSafetyStats.class);
        when(safeStats.getSgisCode()).thenReturn("11111");
        when(safeStats.getRobberyCnt()).thenReturn(0);
        when(safeStats.getTheftCnt()).thenReturn(0);
        when(safeStats.getMurderCnt()).thenReturn(0);
        when(safeStats.getSexualCrimeCnt()).thenReturn(0);
        when(safeStats.getViolenceCnt()).thenReturn(0);
        when(safeStats.getAccCnt()).thenReturn(0);

        SggSafetyStats dangerStats = mock(SggSafetyStats.class);
        when(dangerStats.getSgisCode()).thenReturn("22222");
        when(dangerStats.getRobberyCnt()).thenReturn(100);
        when(dangerStats.getTheftCnt()).thenReturn(200);
        when(dangerStats.getMurderCnt()).thenReturn(50);
        when(dangerStats.getSexualCrimeCnt()).thenReturn(100);
        when(dangerStats.getViolenceCnt()).thenReturn(300);
        when(dangerStats.getAccCnt()).thenReturn(150); //

        when(sggSafetyStatsRepository.findAll()).thenReturn(Arrays.asList(safeStats, dangerStats));

        // ---  인프라 데이터(CCTV, 경찰서, 소방서) 세팅 ---
        // safeRegion: CCTV 500대, 경찰서 5개, 소방서 5개
        // dangerRegion : CCTV 0대, 경찰서 0개, 소방서 0개
        when(cctvRepository.sumCameraCountGroupedBySgisCode()).thenReturn(Arrays.asList(
                new Object[]{"11111001", 500}, new Object[]{"22222001", 0}
        ));
        when(policeStationRepository.countAllGroupedBySgisCode()).thenReturn(Arrays.asList(
                new Object[]{"11111001", 5}, new Object[]{"22222001", 0}
        ));
        when(fireStationRepository.countAllGroupedBySgisCode()).thenReturn(Arrays.asList(
                new Object[]{"11111001", 5}, new Object[]{"22222001", 0}
        ));

        regionSafetyScoreService.calculateAllRegionScores();

        // Z-Score와 Min-Max 정규화 공식에 의해, 단 두 개의 지역만 존재할 경우
        // 상대적으로 안전한 곳은 무조건 100점, 위험한 곳은 무조건 0점을 받아야 정상 작동
        verify(safeRegion, times(1)).updateScores(anyDouble(), anyDouble(), anyDouble(), eq(100.0));

        verify(dangerRegion, times(1)).updateScores(anyDouble(), anyDouble(), anyDouble(), eq(0.0));
    }
}