package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.infra.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JusoCoordResponse {

    private JusoCoordResults results;

    @Getter
    @Setter
    public static class JusoCoordResults {
        private JusoCoordCommon common;
        private List<JusoCoordDetail> juso;
    }

    @Getter
    @Setter
    public static class JusoCoordCommon {
        private String errorCode;
        private String errorMessage;
    }

    @Getter
    @Setter
    public static class JusoCoordDetail {
        private String entX; // 경도
        private String entY; // 위도
        private String bdMgtSn; // 건물관리번호
    }

}
