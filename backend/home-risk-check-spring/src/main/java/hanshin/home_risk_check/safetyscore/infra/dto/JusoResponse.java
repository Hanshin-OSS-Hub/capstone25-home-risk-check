package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.infra.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JusoResponse {
    private JusoResults results;

    @Getter
    @Setter
    public static class JusoResults {
        private JusoCommon common;
        private List<JusoDetail> juso;
    }

    @Getter
    @Setter
    public static class JusoCommon {
        private String errorCode;
        private String errorMessage;
        private String totalCount;
    }

    @Getter
    @Setter
    public static class JusoDetail {
        private String roadAddr; //도로명 주소
        private String jibunAddr; //지번 주소
        private String admCd; // 행정구역코드
        private String rnMgtSn; // 도로명코드
        private String bdMgtSn; // 건물관리코드
        private String udrtYn; //지하 여부(0:지상, 1:지하)
        private String buldMnnm; //건물 본번
        private String buldSlno; //건물 부번
    }
}


