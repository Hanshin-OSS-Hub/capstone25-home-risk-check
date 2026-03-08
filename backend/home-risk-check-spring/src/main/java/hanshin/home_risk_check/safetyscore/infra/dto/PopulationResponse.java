package hanshin.home_risk_check.safetyscore.infra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.ResponseBody;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Getter
@Setter
public class PopulationResponse {

    @JsonProperty("Response")
    private ResponseBody response;


    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseBody {
        private Head head;
        private JsonNode items;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Head {
        private String resultCode;
        private String resultMsg;
        private String totalCount;
        private String numOfRows;
        private String pageNo;
    }


    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true) // 선언한 필드의 값만 받기
    public static class PopulationItem{
        private String statsYm; // 통계년월
        private String stdgCd; // 행정동코드
        private String ctpvNm; //시도 명
        private String sggNm; //시군구 명(은평구)
        private String stdgNm; //법정동명 (불광동)
        private String dongNm; //행정동명 (불광동)
        private String totNmprCnt; //총 인구수
    }
}
