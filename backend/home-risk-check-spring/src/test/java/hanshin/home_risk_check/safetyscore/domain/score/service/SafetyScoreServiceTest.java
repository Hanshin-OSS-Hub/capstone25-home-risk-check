package hanshin.home_risk_check.safetyscore.domain.score.service;

import hanshin.home_risk_check.safetyscore.config.SafetyScoreProperties;
import hanshin.home_risk_check.safetyscore.domain.accident.repository.TrafficRepository;
import hanshin.home_risk_check.safetyscore.domain.cctv.repository.CctvRepository;
import hanshin.home_risk_check.safetyscore.domain.fire.repository.FireStationRepository;
import hanshin.home_risk_check.safetyscore.domain.police.repository.PoliceStationRepository;
import hanshin.home_risk_check.safetyscore.domain.region.entity.Region;
import hanshin.home_risk_check.safetyscore.domain.region.repository.RegionRepository;
import hanshin.home_risk_check.safetyscore.domain.score.dto.SafetyScoreResponse;
import hanshin.home_risk_check.safetyscore.infra.api.KakaoApiCaller;
import hanshin.home_risk_check.safetyscore.infra.dto.KakaoApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@EnableConfigurationProperties(SafetyScoreProperties.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
class SafetyScoreServiceTest {

    @Autowired
    private SafetyScoreProperties properties;

    //  Mock 객체 선언
    @Mock private KakaoApiCaller kakaoApiCaller;
    @Mock private RegionRepository regionRepository;
    @Mock private CctvRepository cctvRepository;
    @Mock private PoliceStationRepository policeStationRepository;
    @Mock private FireStationRepository fireStationRepository;
    @Mock private TrafficRepository trafficRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @InjectMocks
    private SafetyScoreService safetyScoreService;

    @BeforeEach
    void setUp() {
        SafetyScoreProperties.MacroWeights macroWeights = new SafetyScoreProperties.MacroWeights(0.2, 0.5, 0.3);

        properties = new SafetyScoreProperties(
                500.0,        // radius
                4.5,          // hotspotWeight
                5.0,          // densityWeight
                5.0,          // policeScore
                2.0,          // fireScore
                macroWeights  // 거시용 macroWeights 객체 주입
        );

        ReflectionTestUtils.setField(safetyScoreService, "properties", properties);
    }
    @Test
    @DisplayName("정상적인 주소(신갈동)가 주어지면, 가중치가 반영된 정확한 안전 점수(50점)를 계산하여 반환한다.")
    void calculateSafetyScore_Success() {

        // properties 수정여부 체크 (수정시 여기도 수정)
        assertThat(properties.macroWeights().crime()).isEqualTo(0.5);
        assertThat(properties.macroWeights().accident()).isEqualTo(0.3);
        assertThat(properties.macroWeights().infra()).isEqualTo(0.2);

        assertThat(properties.radius()).isEqualTo(500.0);
        assertThat(properties.hotspotWeight()).isEqualTo(4.5);
        assertThat(properties.densityWeight()).isEqualTo(5.0);

        // ---알려진 입력값 ---
        String targetAddress = "경기도 용인시 기흥구 신갈동";
        double mockLat = 37.275;
        double mockLon = 127.108;
        String sgisCode = "41463102"; // 신갈동 행정동 코드

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("system:is_data_ready")).thenReturn("true");
        // [Mock] 카카오 API 가짜 응답
        KakaoApiResponse.KakaoDocument mockDoc = mock(KakaoApiResponse.KakaoDocument.class);
        KakaoApiResponse.AddressInfo mockAddressInfo = mock(KakaoApiResponse.AddressInfo.class);

        when(mockDoc.getX()).thenReturn(String.valueOf(mockLon));
        when(mockDoc.getY()).thenReturn(String.valueOf(mockLat));
        when(mockDoc.getAddress()).thenReturn(mockAddressInfo);
        when(mockAddressInfo.getRegion_3depth_h_name()).thenReturn("신갈동");
        when(kakaoApiCaller.searchAddress(targetAddress)).thenReturn(mockDoc);

        // [Mock] Region Repository 가짜 응답
        Region mockRegion = mock(Region.class);
        when(mockRegion.getSgisCode()).thenReturn(sgisCode);
        when(mockRegion.getSafetyScore()).thenReturn(45.44); // 알려진 기본 점수
        when(regionRepository.findByLocation(mockLon, mockLat)).thenReturn(Optional.of(mockRegion));

        // [Mock] 주변 인프라 개수 가짜 응답
        when(cctvRepository.sumCameraCountWithinRadius(anyDouble(), anyDouble(), anyDouble())).thenReturn(148);
        when(policeStationRepository.countPoliceWithinRadius(anyDouble(), anyDouble(), anyDouble())).thenReturn(0);
        when(fireStationRepository.countFireStationsWithinRadius(anyDouble(), anyDouble(), anyDouble())).thenReturn(0);
        when(trafficRepository.countAccidentAreaWithinRadius(anyDouble(), anyDouble(), anyDouble())).thenReturn(0);

        // [Mock] 비교 수치(밀도) 계산용 데이터 설정
        when(regionRepository.getAreaBySgisCode(sgisCode)).thenReturn(3_000_000.0); // 동네 면적
        when(cctvRepository.sumCameraCountBySgisCode(sgisCode)).thenReturn(210); // 동네 전체 CCTV 수

        // --- 테스트 메서드 실행 ---
        SafetyScoreResponse response = safetyScoreService.calculateSafetyScore(targetAddress);

        // --- 결과 검증 (알려진 출력값 검사) ---
        // 기본 정보 / 성공 여부 , 동네 이름
        assertThat(response.getMeta().getCode()).isEqualTo(200);
        assertThat(response.getData().getRegionName()).isEqualTo("신갈동");

        //점수 테스트
        assertThat(response.getData().getFinalSafetyScore()).isEqualTo(50);

        assertThat(response.getData().getNearbyCctvCount()).isEqualTo(148);
        assertThat(response.getData().getAccidentHotspotCount()).isEqualTo(0);
    }

}