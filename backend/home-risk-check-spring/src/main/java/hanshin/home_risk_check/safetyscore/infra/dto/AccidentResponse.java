package hanshin.home_risk_check.safetyscore.infra.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AccidentResponse {
    private String resultCode;
    private String resultMsg;
    private AccidentItems items;

    @Getter
    @Setter
    public static class AccidentItems {
        private List<AccidentItem> item;
    }

    @Getter
    @Setter
    public static class AccidentItem {
        @JsonProperty("std_year")
        private String stdYear; // 기준 년도

        @JsonProperty("sido_sgg_nm")
        private String sidoSggNm;         // 지역명 (예: 서울특별시 중구)

        @JsonProperty("acc_cl_nm")
        private String accClNm;           // 사고유형 (예: 전체사고)

        @JsonProperty("pop_100k")
        private String pop100k;           // 인구 10만명당 사고건수

        @JsonProperty("ftlt_rate")
        private String ftltRate;          // 치사율

    }
}
