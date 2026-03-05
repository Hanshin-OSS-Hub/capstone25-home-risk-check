package hanshin.home_risk_check.safetyscore.infra.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
@Slf4j
public class OpenApiCaller {

    private final WebClient webClient;

    @Value("${public-data.service-key}")
    private String serviceKey;

    public OpenApiCaller() {
        // 인코딩되어있는 키를 다시 인코딩 안하도록 설정
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        this.webClient = WebClient.builder()
                .uriBuilderFactory(factory)
                .baseUrl("")
                .build();
    }

    /**
     * 공통 GET 요청 메소드
     */
    public <T> T get(String url, Map<String, Object> params, Class<T> responseType){

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
                .queryParam("serviceKey", serviceKey);

        if (params != null) {
            params.forEach(builder::queryParam);
        }

        //이미 encoding된 serviceKey 보호
        URI uri = builder.build(true).toUri();


        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(responseType)
                .block();


    }

}
