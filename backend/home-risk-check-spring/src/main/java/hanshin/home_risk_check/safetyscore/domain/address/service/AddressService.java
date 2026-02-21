package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.address.service;

import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.infra.api.JusoApiCaller;
import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.infra.dto.JusoCoordResponse;
import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.infra.dto.JusoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final JusoApiCaller jusoApiCaller;



    /**
     * 주소 키워드를 통해 표준 주소 정보 검색
     * @param keyword 검색할 주소
     * @return 검색된 주소의 상세 정보, 없으면 null 반환
     */
    public JusoResponse.JusoDetail searchAddress(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }

        Map<String,Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("currentPage", 1);
        params.put("countPerPage", 1);

        try {
            //jusoApiCaller 로 response 받아옴
            JusoResponse response = jusoApiCaller.searchAddress(params, JusoResponse.class);

            if (response != null &&
                    response.getResults() != null &&
                    response.getResults().getJuso() != null &&
                    !response.getResults().getJuso().isEmpty()) {

                return response.getResults().getJuso().get(0);
            }
        } catch (Exception e) {
            log.error("주소 API 호출 중 오류 발생. 검색명 : {}", keyword, e);
        }

        if (keyword.contains("(")) {
            String fallbackKeyword = keyword.replaceAll("\\(.*\\)", "").trim();
            log.info("재검색 시도: [{}] -> [{}]", keyword, fallbackKeyword);
            return searchAddress(fallbackKeyword); // 재귀 호출
        }

        return null;
    }

    /**
     * searchAddress 를 통해 나온 주소를 통해 위,경도 좌표 구함
     */
    public JusoCoordResponse.JusoCoordDetail getCoordinate(JusoResponse.JusoDetail detail){

        Map<String, Object> params = new HashMap<>();

        params.put("admCd", detail.getAdmCd()); // 행정구역코드
        params.put("rnMgtSn", detail.getRnMgtSn()); // 도로명코드
        params.put("udrtYn", detail.getUdrtYn()); //지하 여부(0:지상, 1:지하)
        params.put("buldMnnm", detail.getBuldMnnm()); //건물 본번
        params.put("buldSlno", detail.getBuldSlno()); //건물 부번

        try {
            //jusoApiCaller 로 response 받아옴
            JusoCoordResponse response = jusoApiCaller.searchCoordinate(params, JusoCoordResponse.class);
            if (response != null &&
                    response.getResults() != null &&
                    response.getResults().getJuso() != null &&
                    !response.getResults().getJuso().isEmpty()) {
                return response.getResults().getJuso().get(0);
            }
        } catch (Exception e) {
            log.error("좌표 API 호출 중 오류 발생. 검색명 : {}", detail.getRoadAddr(), e);
        }

        return null;
    }


}
