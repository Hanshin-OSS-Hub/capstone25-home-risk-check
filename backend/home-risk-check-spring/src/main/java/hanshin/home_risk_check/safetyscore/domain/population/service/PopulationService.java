package hanshin.home_risk_check.safetyscore.domain.population.service;

import hanshin.home_risk_check.safetyscore.domain.region.entity.Region;
import hanshin.home_risk_check.safetyscore.domain.region.repository.RegionRepository;
import hanshin.home_risk_check.safetyscore.infra.api.OpenApiCaller;
import hanshin.home_risk_check.safetyscore.infra.dto.PopulationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopulationService {

    private final OpenApiCaller openApiCaller;
    private final RegionRepository regionRepository;

    private static final String POPULATION_API_URL = "http://apis.data.go.kr/1741000/admmPpltnHhStus/selectAdmmPpltnHhStus";

    /**
     *  DB를 먼저 확인하고, 없으면 API를 호출하여 저장한 뒤 인구수를 반환합니다.
     */
    public Integer getOrUpdatePopulation(Region region) {
        //  DB에 이미 인구 데이터가 있는지 확인
        if (region.getPopulation() != null && region.getPopulation() > 0) {
            log.info(" {} 지역은 이미 데이터가 있습니다. (인구: {}명)", region.getAdmNm(), region.getPopulation());
            return region.getPopulation();
        }

        //  DB에 없다면 실시간 API 호출 진행
        log.info("{} 지역 인구 데이터 실시간 수집 시작...", region.getAdmNm());
        PopulationResponse.PopulationItem apiItem = getPopulationByDongCode(region.getAdmCode());

        if (apiItem != null && apiItem.getTotNmprCnt() != null) {
            int population = Integer.parseInt(apiItem.getTotNmprCnt());
            String statsYm = apiItem.getStatsYm();

            // 3. 엔티티 업데이트 및 DB 저장
            region.updatePopulation(population, statsYm);
            regionRepository.save(region);

            log.info("{} 지역 데이터 저장 완료! (인구: {}명)", region.getAdmNm(), population);
            return population;
        }

        log.warn("{} 지역 인구 데이터를 가져오지 못했습니다.", region.getAdmNm());
        return 0; // 혹은 예외 처리
    }


    /**
     * 법정동코드를 받아 해당 동의 작년 12월 인구 통계를 가져옴
     */
    public PopulationResponse.PopulationItem getPopulationByDongCode(String admCode) {
        Map<String, Object> params = new HashMap<>();

        // 조회 기준 날짜 세팅
        String targetYearMonth = YearMonth.now().minusYears(1).withMonth(12).format(DateTimeFormatter.ofPattern("yyyyMM"));

        return getPopulationByDongCode(admCode, targetYearMonth);
    }

    /**
     * 특정 기간의 인구조회
     */
    public PopulationResponse.PopulationItem getPopulationByDongCode(String admCode,String targetYearMonth) {
        Map<String, Object> params = new HashMap<>();

        String fullAdmmCd = (admCode != null && admCode.length() == 8) ? admCode + "00" : admCode;

        params.put("admmCd", fullAdmmCd); //법정동 코드
        params.put("srchFrYm", targetYearMonth); //시작년월
        params.put("srchToYm", targetYearMonth); //종료년월
        params.put("lv", 3); // 조회 결과범위 (4: 읍면동 단위)
        params.put("regSeCd", "1");               // 등록구분 (1: 전체)
        params.put("type", "JSON");               // JSON 형식


        try {
            PopulationResponse response = openApiCaller.get(POPULATION_API_URL, params, PopulationResponse.class);

            // 계층별로 null 체크
            if (response != null && response.getResponse() != null) {

                PopulationResponse.ResponseBody body = response.getResponse();

                // items가 객체 형태이고 안에 데이터가 있는지 확인
                if (body.getItems() != null && body.getItems().isObject()) {
                    JsonNode itemNode = body.getItems().get("item");

                    if (itemNode != null && itemNode.isArray() && !itemNode.isEmpty()) {
                        // 첫 번째 데이터를 PopulationItem 객체로 변환
                        ObjectMapper mapper = new ObjectMapper();
                        return mapper.treeToValue(itemNode.get(0), PopulationResponse.PopulationItem.class);
                    }
                }

                // 데이터가 없거나 에러
                if (body.getHead() != null) {
                    log.warn(" 코드: {}, 메시지: {}", body.getHead().getResultCode(), body.getHead().getResultMsg());
                }
            } else {
                log.warn("API 응답이 비어있거나 구조가 맞지 않습니다. (코드: {})", admCode);
            }
        } catch (Exception e) {
            log.error(" 지역코드: {}, 기준년월: {} 호출 중 에러", admCode, targetYearMonth, e);
        }

        return null;
    }
}