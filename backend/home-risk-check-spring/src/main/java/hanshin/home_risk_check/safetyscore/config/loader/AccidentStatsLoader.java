package hanshin.home_risk_check.safetyscore.config.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccidentStatsLoader {

    private final JdbcTemplate jdbcTemplate;
    private final FileSyncService fileSyncService;

    @Transactional
    public boolean loadData() {
        try {
            // 필요한 리소스 확보
            Resource[] sidoResources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/accident/taas_sido.csv");
            Resource[]  gugunResources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/accident/taas_gugun.csv");
            Resource[] sgisResources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/accident/sgis_admcode.csv");

            if (sidoResources.length == 0 || gugunResources.length == 0 || sgisResources.length == 0) {
                log.warn("TAAS 관련 CSV 파일을 찾을 수 없습니다.");
                return false;
            }

            Resource sidoRes = sidoResources[0];
            Resource gugunRes = gugunResources[0];
            Resource sgisRes = sgisResources[0];

            // 지문 계산
            String sidoHash = fileSyncService.calculateHash(sidoRes);
            String gugunHash = fileSyncService.calculateHash(gugunRes);
            String sgisHash = fileSyncService.calculateHash(sgisRes);

            // csv파일 변경 여부 확인
            boolean isSidoChanged = fileSyncService.isChanged(sidoRes.getFilename(), sidoHash);
            boolean isGugunChanged = fileSyncService.isChanged(gugunRes.getFilename(), gugunHash);
            boolean isSgisChanged = fileSyncService.isChanged(sgisRes.getFilename(), sgisHash);

            if (!isSidoChanged && !isGugunChanged && !isSgisChanged) {
                log.info("교통사고 CSV 파일 내용이 동일하여 데이터 적재를 건너뜁니다.");
                return false;
            }

            log.info("교통사고 CSV 파일 변경 감지, 데이터 동기화를 시작합니다.");

            Map<String, String> sidoMap = loadSidoData(sidoRes);
            Map<String, String> sgisMap = loadSgisAdmCodeData(sgisRes);

            if (sidoMap.isEmpty() || sgisMap.isEmpty()) {
                log.warn("데이터를 불러올 수 없습니다.");
                return false;
            }

            // Upsert 쿼리 (없다면 생성, 있다면 덮어쓰기)
            String insertSql = "INSERT INTO sgg_safety_stats (sido_nm, sgg_nm, sido_code, sgg_code, adm_code) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE sido_code = VALUES(sido_code), sgg_code = VALUES(sgg_code), adm_code = VALUES(adm_code)";

            List<Object[]> batchArgs = new ArrayList<>();
            int totalCount = 0;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(gugunRes.getInputStream(), StandardCharsets.UTF_8))) {
                br.readLine(); // 헤더 스킵
                String line;
                String currentSidoNm = "";
                String currentSidoCode = "";

                while ((line = br.readLine()) != null) {
                    try {
                        String[] parts = line.split(",");
                        if (parts.length < 3) continue;

                        if (!parts[0].trim().isEmpty()) {
                            String rawSidoNm = parts[0].trim();
                            currentSidoNm = rawSidoNm.equals("제주도") ? "제주특별자치도" : rawSidoNm;
                            currentSidoCode = sidoMap.get(currentSidoNm);
                        }

                        String sggNm = parts[1].trim();
                        String sggCode = parts[2].trim();
                        String sidoCode = currentSidoCode;

                        if ("부산광역시".equals(currentSidoNm) && "진구".equals(sggNm)) {
                            sggNm = "부산진구";
                        }

                        if (currentSidoNm.contains("세종")) {
                            currentSidoNm = "세종특별자치시";
                            sggNm = "세종시";
                        }

                        //없어진 행정구역
                        if (sggNm.equals("청원군") || sggNm.equals("연기군")) {
                            continue;
                        }

                        if (sidoCode == null) {
                            continue;
                        }

                        //SGIS AdmCode 매칭
                        String searchKey = currentSidoNm + " " + sggNm;
                        String sgisCode = sgisMap.get(searchKey);

                        if (sgisCode != null) {
                            batchArgs.add(new Object[]{currentSidoNm, sggNm, sidoCode, sggCode, sgisCode});
                            totalCount++;
                        } else {
                            boolean found = false;
                                for (Map.Entry<String, String> entry : sgisMap.entrySet()) {
                                    // 띄어쓰기 한 칸 포함해서 검색 ("경기도 수원시 ")
                                    if (entry.getKey().startsWith(searchKey + " ")) {
                                        String expandedSggNm = entry.getKey().replace(currentSidoNm + " ", ""); // "수원시 장안구"
                                        batchArgs.add(new Object[]{currentSidoNm, expandedSggNm, sidoCode, sggCode, entry.getValue()});
                                        totalCount++;
                                        found = true;
                                    }
                                }
                            if (!found) {
                                log.debug("SGIS 마스터에서 매핑 대상을 찾을 수 없습니다: [{}]", searchKey);
                            }
                        }

                        // 100개 단위로 Batch Insert 실행 (데이터가 많지 않아 100단위로 설정)
                        if (batchArgs.size() == 100) {
                            jdbcTemplate.batchUpdate(insertSql, batchArgs);
                            batchArgs.clear();
                        }
                    } catch (Exception e) {
                        log.warn("TAAS 데이터 개별 행 파싱 중 오류 발생. [데이터: {}]", line, e);
                    }
                }
            }


            // 남은 데이터 최종 저장
            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(insertSql, batchArgs);
            }

            // 파일 내용 다시 Hash 저장
            fileSyncService.updateSyncHistory(sidoRes.getFilename(), sidoHash);
            fileSyncService.updateSyncHistory(gugunRes.getFilename(), gugunHash);
            fileSyncService.updateSyncHistory(sgisRes.getFilename(), sgisHash);

            if (totalCount > 0) {
                log.info("총 {}개의 신규 교통사고용 지역 코드가 저장되었습니다.", totalCount);
            }
            return true;
        } catch (Exception e) {
            log.error("TAAS 지역 코드 로드 중 치명적 오류 발생", e);
            return false;
        }
    }



    private Map<String, String> loadSidoData(Resource sidoRes) {
        Map<String, String> sidoMap = new HashMap<>();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(sidoRes.getInputStream(), StandardCharsets.UTF_8))) {
                    br.readLine();
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length >= 2) {
                            sidoMap.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                }
            catch (Exception e) {
            log.error("TAAS 시도(Sido) CSV 파일 읽기 실패", e);
        }
        return sidoMap;
        }

    private Map<String, String> loadSgisAdmCodeData(Resource sgisRes) {
        Map<String, String> sgisMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(sgisRes.getInputStream(), StandardCharsets.UTF_8))) {
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                // 빈 줄이거나 헤더 줄인 경우 건너뛰기
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",");
                // 시도코드, 시도명, 시군구코드, 시군구명 최소 4개는 있어야 함
                if (parts.length >= 4) {
                    try {
                        //  시도코드 추출 및 소수점 제거 (예: "11.0" -> "11", "11" -> "11")
                        String sidoCdRaw = parts[0].trim();
                        if (sidoCdRaw.contains(".")) {
                            sidoCdRaw = sidoCdRaw.split("\\.")[0];
                        }

                        //  시도명칭 추출 (예: "서울특별시")
                        String sidoNm = normalizeSidoName(parts[1].trim());

                        //  시군구코드 추출 및 소수점 제거 (예: "010.0" -> "010", "010" -> "010")
                        String sggCdRaw = parts[2].trim();
                        if (sggCdRaw.contains(".")) {
                            sggCdRaw = sggCdRaw.split("\\.")[0];
                        }

                        //  시군구명칭 추출 (예: "종로구")
                        String sggNm = parts[3].trim();

                        //  5자리 SGIS 코드 조립 (예: "11" + "010" = "11010")
                        String sgis5DigitCode = sidoCdRaw + sggCdRaw;

                        //  매핑 Key 생성 (예: "서울특별시 종로구")
                        String key = sidoNm + " " + sggNm;

                        // 같은 구 안에 여러 동이 있어서 동일한 key가 여러 번 나오므로, 최초 1회만 저장
                        sgisMap.putIfAbsent(key, sgis5DigitCode);

                    } catch (Exception parseEx) {
                        log.warn("SGIS 행 파싱 중 오류 발생. 데이터: {}", line);
                    }
                }
            }
            log.info("SGIS 5자리 행정구역 코드 매핑 데이터 로드 완료 (총 {}개 시군구)", sgisMap.size());

        } catch (Exception e) {
            log.error("SGIS 마스터 CSV 파일 읽기 실패", e);
        }

        return sgisMap;
    }

    /**
     * 과거 행정구역 명칭을 최신 명칭으로 보정
     */
    private String normalizeSidoName(String rawSidoNm) {
        if ("전라북도".equals(rawSidoNm)) return "전북특별자치도";
        if ("강원도".equals(rawSidoNm)) return "강원특별자치도";
        if ("제주도".equals(rawSidoNm)) return "제주특별자치도";
        return rawSidoNm; // 나머지는 그대로 반환
    }
}

