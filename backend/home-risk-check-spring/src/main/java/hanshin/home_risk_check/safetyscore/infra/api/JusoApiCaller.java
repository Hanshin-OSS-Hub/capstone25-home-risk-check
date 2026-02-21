package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.infra.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import java.net.URI;
import java.util.Map;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class JusoApiCaller {

    private final WebClient webClient;

    private static final String JUSO_Base_URL = "https://business.juso.go.kr/addrlink/";

    @Value("${juso.api-key}")
    private String jusoKey;

    public JusoApiCaller() {
        this.webClient = WebClient.builder()
                .baseUrl(JUSO_Base_URL)
                .build();
    }

    /**
     * 주소 검색 API 호출 (addrLinkApi.do)
     */
    public <T> T searchAddress(Map<String, Object> params, Class<T> responseType) {
        return callJusoApi("addrLinkApi.do", params, responseType);
    }

    /**
     * 좌표 제공 API 호출 (addrCoordApi.do)
     */
    public <T> T searchCoordinate(Map<String, Object> params, Class<T> responseType) {
        return callJusoApi("addrCoordApi.do", params, responseType);
    }

    public <T> T callJusoApi(String path, Map<String, Object> params, Class<T> responseType){
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(JUSO_Base_URL)
                .path(path)
                .queryParam("confmKey", jusoKey)
                .queryParam("resultType", "json");

        if (params != null) {
            params.forEach(builder::queryParam);
        }

        // 한글, 공백 문제를 해결한 uri
        URI uri = builder.build().encode().toUri();

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

}