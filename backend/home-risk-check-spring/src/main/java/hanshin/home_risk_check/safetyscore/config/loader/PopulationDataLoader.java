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
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopulationDataLoader {

    private final JdbcTemplate jdbcTemplate;
    private final FileSyncService fileSyncService;

    @Transactional
    public boolean loadData() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/population/*.csv");

            if (resources.length == 0) {
                log.warn("인구 데이터 CSV 파일을 찾을 수 없습니다.");
                return false;
            }

            Resource resource = resources[0];
            String filename = resource.getFilename();

            String fileHash = fileSyncService.calculateHash(resource);
            boolean isChanged = fileSyncService.isChanged(filename, fileHash);
            if (!isChanged) {
                log.info("인구 데이터 CSV 파일 내용이 동일하여 데이터 적재를 건너뜁니다.");
                return false;
            }

            log.info("인구 데이터 CSV 파일 변경 감지, 데이터 동기화를 시작합니다.");

            // 인구 업데이트 SQL (행정동 코드가 일치하는 행의 인구수와 기준년월 업데이트)
            String sql = "UPDATE safety_regions SET population = ?, population_stats_ym = ? WHERE adm_code = ?";

            List<Object[]> batchArgs = new ArrayList<>();
            int processedCount = 0;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                boolean isFirstLine = true;

                while ((line = br.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false; // 헤더 건너뛰기
                        continue;
                    }

                    String[] data = line.split(",");

                    // 데이터 구조: [0]년도, [1]행정동코드, [2]항목코드, [3]인구수
                    if (data.length >= 4) {
                        String year = data[0].replace("\"", "");
                        String admCode = data[1].replace("\"", "");
                        String itemCode = data[2].replace("\"", "");
                        String popValue = data[3].replace("\"", "");

                        // 필터링: '총인구(to_in_001)' 이면서 '8자리 행정동'인 데이터만 수집
                        if ("to_in_001".equals(itemCode) && admCode.length() == 8) {
                            int population = Integer.parseInt(popValue);
                            String statsYm = year + "12"; // 예: 202412

                            batchArgs.add(new Object[]{population, statsYm, admCode});
                            processedCount++;

                            // 1000건씩 배치 업데이트 실행 (성능 최적화)
                            if (batchArgs.size() == 1000) {
                                jdbcTemplate.batchUpdate(sql, batchArgs);
                                batchArgs.clear();
                            }
                        }
                    }
                }
            }

            // 남은 데이터 처리
            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
            }

            fileSyncService.updateSyncHistory(filename, fileHash);

            log.info("인구 데이터 동기화 완료. 총 {} 건이 추가 되었습니다.", processedCount);
            return true;

        } catch (Exception e) {
            log.error("인구 데이터 로드 중 치명적 오류 발생", e);
            return false;
        }
    }

}