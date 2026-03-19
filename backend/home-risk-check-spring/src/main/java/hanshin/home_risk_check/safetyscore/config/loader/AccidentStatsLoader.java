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

            if (sidoResources.length == 0 || gugunResources.length == 0) {
                log.warn("TAAS 관련 CSV 파일을 찾을 수 없습니다.");
                return false;
            }

            Resource sidoRes = sidoResources[0];
            Resource gugunRes = gugunResources[0];

            // 지문 계산
            String sidoHash = fileSyncService.calculateHash(sidoRes);
            String gugunHash = fileSyncService.calculateHash(gugunRes);

            // csv파일 변경 여부 확인
            boolean isSidoChanged = fileSyncService.isChanged(sidoRes.getFilename(), sidoHash);
            boolean isGugunChanged = fileSyncService.isChanged(gugunRes.getFilename(), gugunHash);

            if (!isSidoChanged && !isGugunChanged) {
                log.info("교통사고 CSV 파일 내용이 동일하여 데이터 적재를 건너뜁니다.");
                return false;
            }

            log.info("교통사고 CSV 파일 변경 감지, 데이터 동기화를 시작합니다.");

            Map<String, String> sidoMap = loadSidoData(sidoRes);

            if (sidoMap.isEmpty()) {
                log.warn("TAAS 시도(Sido) 데이터를 불러올 수 없습니다.");
                return false;
            }

            // Upsert 쿼리 (없다면 생성, 있다면 덮어쓰기)
            String insertSql = "INSERT INTO sgg_safety_stats (sido_nm, sgg_nm, sido_code, sgg_code) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE sido_code = VALUES(sido_code), sgg_code = VALUES(sgg_code)";

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

                        //없어진 행정구역
                        if (sggNm.equals("청원군") || sggNm.equals("연기군")) {
                            continue;
                        }

                        if (sidoCode == null) {
                            continue;
                        }

                        batchArgs.add(new Object[]{currentSidoNm, sggNm, sidoCode, sggCode});
                        totalCount++;

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
    }

