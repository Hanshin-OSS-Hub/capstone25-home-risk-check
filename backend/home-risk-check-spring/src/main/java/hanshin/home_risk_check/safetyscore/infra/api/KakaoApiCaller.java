package hanshin.home_risk_check.safetyscore.infra.api;

import hanshin.home_risk_check.safetyscore.infra.dto.KakaoApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoApiCaller {

    @Value("${kakao.rest-api-key}")
    private String kakaoApiKey;

    // 교통사고 다발지역 확인용 장소/키워드 검색
    private static final String KAKAO_LOCAL_KEYWORD_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    // 정확한 주소 검색용 (유저 입력 주소-> 행정동 추출용)
    private static final String KAKAO_LOCAL_ADDRESS_URL = "https://dapi.kakao.com/v2/local/search/address.json";

    private final RestTemplate restTemplate;

    public KakaoApiResponse.KakaoDocument searchPlace(String keyWord){
        return requestKakaoApi(KAKAO_LOCAL_KEYWORD_URL, keyWord);
    }

    public KakaoApiResponse.KakaoDocument searchAddress(String address){
        return requestKakaoApi(KAKAO_LOCAL_ADDRESS_URL, address);
    }

    public KakaoApiResponse.KakaoDocument requestKakaoApi(String url, String query) {
        try {
            //Kakao API : KakaoAK {REST_API_KEY} 들어가야함
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // URL 생성
            String uriString = UriComponentsBuilder.fromUriString(url)
                    .queryParam("query", query)
                    .queryParam("size", 1)
                    .build()
                    .toUriString();

            // API 통신
            ResponseEntity<KakaoApiResponse> response = restTemplate.exchange(
                    uriString,
                    HttpMethod.GET,
                    entity,
                    KakaoApiResponse.class
            );

            // 결과 꺼내기
            if (response.getBody() != null &&
                    response.getBody().getDocumentList() != null &&
                    !response.getBody().getDocumentList().isEmpty()) {

                return response.getBody().getDocumentList().get(0);
            }

        } catch (Exception e) {
            log.error("카카오 로컬 API 호출 중 오류 발생. 검색어: {}", query, e);
        }

        return null;
    }

}
