package hanshin.home_risk_check.safetyscore.infra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class TaasApiResponse {

    private String resultCode;
    private String resultMsg;
    private Items items;


    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        private List<Item> item;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        @JsonProperty("sido_sgg_nm")
        private String sidoSggNm;   // 시군구명 (공간 DB의 지역 데이터와 매핑할 때 필수)

        @JsonProperty("acc_cl_nm")
        private String accClNm;     // 사고분류명 ("전체사고" 데이터만 필터링할 때 사용)

        @JsonProperty("acc_cnt")
        private String accCnt;      // 단순 사고건수

        @JsonProperty("pop_100k")
        private String pop100k;     // 인구 10만명당 사고건수 (인구 대비 사고 건수)

        @JsonProperty("dth_dnv_cnt")
        private String dthDnvCnt;
    }
}
