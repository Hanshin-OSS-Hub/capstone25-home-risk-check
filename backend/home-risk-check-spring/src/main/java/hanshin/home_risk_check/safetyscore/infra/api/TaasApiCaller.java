package hanshin.home_risk_check.safetyscore.infra.api;

import hanshin.home_risk_check.safetyscore.infra.dto.TaasApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class TaasApiCaller {

    @Value("${taas.api-key}")
    private String taasApiKey;

    private static final String TAAS_API_URL = "https://opendata.koroad.or.kr/data/rest/stt";


    /**
     * 특정 지역의 교통사고 위험도 데이터 조회
     */
    public TaasApiResponse fetchTrafficRiskData(String sidoCode, String sggCode){
        // API 키 2중 암호화 방지용
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(TAAS_API_URL);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(factory);

        URI uri = UriComponentsBuilder.fromUriString(TAAS_API_URL)
                .queryParam("authKey", taasApiKey)
                .queryParam("searchYearCd", "2024") //기준 연도
                .queryParam("sido", sidoCode)
                .queryParam("gugun", sggCode)
                .queryParam("type", "json")
                .build(true)
                .toUri();

        try {
            // 외부 API 호출 및 DTO 변환
            return restTemplate.getForObject(uri, TaasApiResponse.class);

        } catch (Exception e) {
            throw new RuntimeException("TAAS API 연동 중 오류가 발생했습니다. (sido: " + sidoCode + ", sgg: " + sggCode + ")", e);
        }
    }


}
