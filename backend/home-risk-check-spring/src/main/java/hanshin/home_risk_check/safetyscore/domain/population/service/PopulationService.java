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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopulationService {

    private final OpenApiCaller openApiCaller;
    private final RegionRepository regionRepository;

    private static final String POPULATION_API_URL = "http://apis.data.go.kr/1741000/stdgPpltnHhStus/selectStdgPpltnHhStus";

    /**
     *  DB를 먼저 확인하고, 없으면 API를 호출하여 저장한 뒤 인구수를 반환합니다.
     */
    public Integer getOrUpdatePopulation(Region region) {
        //  DB에 이미 인구 데이터가 있는지 확인
        if (region.getPopulation() != null) {
            log.info(" {} 지역은 이미 데이터가 있습니다. (인구: {}명)", region.getAdmNm(), region.getPopulation());
            return region.getPopulation();
        }

        //  DB에 없다면 실시간 API 호출 진행
        log.info("{} 지역 인구 데이터 실시간 수집 시작...", region.getAdmNm());
        PopulationResponse.PopulationItem apiItem = getPopulationByDongCode(region.getAdmCode());

        if (apiItem != null) {
            String sidoNm = apiItem.getCtpvNm();
            String sggNm = apiItem.getSggNm();

            // 인구까지 다 찾은경우
            if (apiItem.getTotNmprCnt() != null) {
                int population = Integer.parseInt(apiItem.getTotNmprCnt());
                region.updateRegionNames(sidoNm, sggNm);
                region.updatePopulation(population, apiItem.getStatsYm());
                regionRepository.save(region);

                log.info("{} 완료 (인구: {}, 주소: {} {})", region.getAdmNm(), population, sidoNm, sggNm);
                return population;
            }
            //인구를 못찾은경우, 시군구, 시도만 저장
            else {
                String fallbackStatsYm = YearMonth.now().minusYears(1).withMonth(12).format(DateTimeFormatter.ofPattern("yyyyMM"));
                region.updateRegionNames(sidoNm, sggNm);
                region.updatePopulation(-1, fallbackStatsYm);
                regionRepository.save(region);

                log.warn("{} 인구 없음(-1). 주소만 저장 ({} {})", region.getAdmNm(), sidoNm, sggNm);
                return -1;
            }
        }

        log.warn("{} 지역 인구 데이터를 가져오지 못했습니다. DB에 -2명으로 기록합니다.", region.getAdmNm());
        String fallbackStatsYm = YearMonth.now().minusYears(1).withMonth(12).format(DateTimeFormatter.ofPattern("yyyyMM"));

        region.updatePopulation(-2, fallbackStatsYm);
        regionRepository.save(region);

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

        String fullStdgCd = (admCode != null && admCode.length() == 8) ? admCode + "00" : admCode;

        //법정동 코드를 5자리까지만 잘라서 속한 동네를 다 호출(읍,면,동) 전부 호출하기위해 사용
        if (fullStdgCd != null && fullStdgCd.length() >= 5) {
            params.put("stdgCd", fullStdgCd.substring(0, 5) + "00000");
        }
        params.put("srchFrYm", targetYearMonth); //시작년월
        params.put("srchToYm", targetYearMonth); //종료년월
        params.put("lv", 3); // 조회 결과범위 (3: 읍면동 단위)
        params.put("regSeCd", "1");               // 등록구분 (1: 전체)
        params.put("type", "JSON");               // JSON 형식
        params.put("numOfRows", "100");


        try {
            PopulationResponse response = openApiCaller.get(POPULATION_API_URL, params, PopulationResponse.class);

            // 계층별로 null 체크
            if (response != null && response.getResponse() != null) {

                PopulationResponse.ResponseBody body = response.getResponse();

                // items가 객체 형태이고 안에 데이터가 있는지 확인
                if (body.getItems() != null && body.getItems().isObject()) {
                    JsonNode itemNode = body.getItems().get("item");
                    int totalPopulation = 0;
                    String statsYm = "";
                    String sidoNm = "";
                    String sggNm = "";

                    if (itemNode != null) {
                        ObjectMapper mapper = new ObjectMapper();

                        // 해당 읍면동이 속한 시군구의 전체 목록을 줄때 (정자 1동,2동,3동)
                        if (itemNode.isArray() && !itemNode.isEmpty()) {

                            sidoNm = itemNode.get(0).path("ctpvNm").asText("");
                            sggNm = itemNode.get(0).path("sggNm").asText("");

                            for (JsonNode node : itemNode) {
                                PopulationResponse.PopulationItem item = mapper.treeToValue(node, PopulationResponse.PopulationItem.class);

                                if (item.getStdgCd().startsWith(fullStdgCd.substring(0, 8))) {
                                    totalPopulation += Integer.parseInt(item.getTotNmprCnt());
                                    statsYm = item.getStatsYm();

                                }
                            }
                            PopulationResponse.PopulationItem result = new PopulationResponse.PopulationItem();
                            result.setCtpvNm(sidoNm);
                            result.setSggNm(sggNm);

                            if (totalPopulation > 0) {
                                result.setTotNmprCnt(String.valueOf(totalPopulation));
                                result.setStatsYm(statsYm);

                            }
                            return result;
                        }
                        // 법정동 1개로 올때
                        else if (itemNode.isObject()) {
                            PopulationResponse.PopulationItem item = mapper.treeToValue(itemNode, PopulationResponse.PopulationItem.class);

                            PopulationResponse.PopulationItem result = new PopulationResponse.PopulationItem();
                            result.setCtpvNm(item.getCtpvNm());
                            result.setSggNm(item.getSggNm());

                            if (item.getStdgCd() != null && item.getStdgCd().startsWith(fullStdgCd.substring(0, 8))) {
                                result.setTotNmprCnt(item.getTotNmprCnt());
                                result.setStatsYm(item.getStatsYm());

                                return result;
                            }
                        }

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

    /**
     * 인구 데이터가 없는 모든 지역을 찾아 업데이트합니다.
     */
    public void updateAllMissingPopulation() {
        List<Region> missingRegions = regionRepository.findAllByPopulationIsNull();

        log.info("총 {}개의 지역 인구 데이터를 수집합니다.", missingRegions.size());

        for (Region region : missingRegions) {
            try {
                // 기존 로직 호출 (내부에서 API 호출 및 DB 저장 수행)
                this.getOrUpdatePopulation(region);

                // API 서버 과부하 방지를 위한 아주 짧은 휴식 (약 50ms)
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("{} 지역 수집 실패: {}", region.getAdmNm(), e.getMessage());
            }
        }
        log.info("모든 미비 인구 데이터 수집 프로세스가 완료되었습니다.");
    }
}