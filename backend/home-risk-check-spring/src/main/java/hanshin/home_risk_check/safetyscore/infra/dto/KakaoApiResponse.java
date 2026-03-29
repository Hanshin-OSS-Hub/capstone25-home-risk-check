package hanshin.home_risk_check.safetyscore.infra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true) // 지정한 값만 받아오게 하기
public class KakaoApiResponse {

    @JsonProperty("documents")
    private List<KakaoDocument> documentList;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoDocument {

        private String place_name; // 장소명
        private String address_name; // 지번 주소
        private String road_address_name; // 도로명 주소
        private String x; // 경도
        private String y; // 위도

        @JsonProperty("address")
        private AddressInfo address;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddressInfo {
        private String region_1depth_name; // 시/도
        private String region_2depth_name; // 구/군
        private String region_3depth_name; // 법정동

        private String region_3depth_h_name; // 행정동 명칭

        private String h_code; //행정동 코드
        private String b_code; //법정동 코드
    }
}
