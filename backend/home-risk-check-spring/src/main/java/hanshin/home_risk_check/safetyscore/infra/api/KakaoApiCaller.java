package hanshin.home_risk_check.safetyscore.infra.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanshin.home_risk_check.safetyscore.infra.dto.KakaoApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


import java.util.concurrent.TimeUnit;


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
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Retry(name = "kakaoApi", fallbackMethod = "fallbackKakaoApi")
    @CircuitBreaker(name = "kakaoApi", fallbackMethod = "fallbackKakaoApi")
    public KakaoApiResponse.KakaoDocument searchPlace(String keyWord){
        String cacheKey = "kakaoPlaceCache:" + keyWord.replaceAll("\\s+", "");

        // Redis 캐시 먼저 확인
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                log.info("Redis 캐시에서 장소 데이터 호출 완료 - Key: {}", cacheKey);
                return objectMapper.readValue(json, KakaoApiResponse.KakaoDocument.class);
            }
        } catch (Exception e) {
            log.warn("캐시 읽기 실패: {}", e.getMessage());
        }

        KakaoApiResponse.KakaoDocument result = requestKakaoApi(KAKAO_LOCAL_KEYWORD_URL, keyWord);

        // API 응답이 정상이면 Redis에 저장 (TTL 24시간)
        if (result != null) {
            try {
                String json = objectMapper.writeValueAsString(result);
                redisTemplate.opsForValue().set(cacheKey, json, 24, TimeUnit.HOURS);
                log.info("Redis에 새로운 장소 데이터 저장 완료 - Key: {}", cacheKey);
            } catch (Exception e) {
                log.warn("캐시 저장 실패: {}", e.getMessage());
            }
        }
        return result;
    }

    @Retry(name = "kakaoApi", fallbackMethod = "fallbackKakaoApi")
    @CircuitBreaker(name = "kakaoApi", fallbackMethod = "fallbackKakaoApi")
    public KakaoApiResponse.KakaoDocument searchAddress(String address){
        String cacheKey = "kakaoAddressCache:" + address.replaceAll("\\s+", "");
        // Redis 캐시 먼저 확인
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json != null)  {
                log.info("Redis 캐시에서 주소 데이터 호출 완료 - Key: {}", cacheKey);
                return objectMapper.readValue(json, KakaoApiResponse.KakaoDocument.class);
            }
        } catch (Exception e) {
            log.warn("캐시 읽기 실패: {}", e.getMessage());
        }

        KakaoApiResponse.KakaoDocument result = requestKakaoApi(KAKAO_LOCAL_ADDRESS_URL, address);

        // API 응답이 정상이면 Redis에 저장 (TTL 24시간)
        if (result != null) {
            try {
                String json = objectMapper.writeValueAsString(result);
                redisTemplate.opsForValue().set(cacheKey, json, 24, TimeUnit.HOURS);
                log.info("Redis에 새로운 주소 데이터 저장 완료 - Key: {}", cacheKey);
            } catch (Exception e) {
                log.warn("캐시 저장 실패: {}", e.getMessage());
            }
        }

        return result;
    }

    /**
     * API 통신 로직
     */
    public KakaoApiResponse.KakaoDocument requestKakaoApi(String url, String query) {
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

        return null;
    }

    /**
     * Fallback 메서드
     * 원본 메서드(searchPlace, searchAddress)의 파라미터 시그니처와 일치해야 하며,
     * 마지막 파라미터로 Throwable 객체를 받아야 Resilience4j가 리플렉션을 통해 정상 호출할 수 있습니다.
     */
    public KakaoApiResponse.KakaoDocument fallbackKakaoApi(String query, Throwable t) {
        log.error("[Fallback] 카카오 API 호출 실패 (재시도 초과 또는 서킷 오픈). 입력값: {}, 원인: {}", query, t.getMessage());

        // 에러 발생 시 시스템 셧다운을 막기 위해 null 또는 빈 객체를 반환합니다.
        return null;
    }

}
