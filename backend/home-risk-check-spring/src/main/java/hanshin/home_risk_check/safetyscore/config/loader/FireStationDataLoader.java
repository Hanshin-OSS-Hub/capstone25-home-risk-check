package hanshin.home_risk_check.safetyscore.config.loader;

import hanshin.home_risk_check.safetyscore.infra.api.KakaoApiCaller;
import hanshin.home_risk_check.safetyscore.infra.dto.KakaoApiResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FireStationDataLoader {

    private final KakaoApiCaller kakaoApiCaller;
    private final JdbcTemplate jdbcTemplate;
    private final FileSyncService fileSyncService;

    @Transactional
    public void loadData() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/fire_station/*.csv");

            if (resources.length == 0) {
                log.warn("소방서 CSV 파일을 찾을 수 없습니다.");
                return;
            }

            Resource resource = resources[0];
            String filename = resource.getFilename();
            String fileHash = fileSyncService.calculateHash(resource);
            boolean isChanged = fileSyncService.isChanged(filename, fileHash);

            if (!isChanged) {
                log.info("소방서 CSV 파일 내용이 동일하여 데이터 적재를 건너뜁니다.");
                return;
            }

            log.info("소방서 CSV 파일 변경 감지. 데이터 동기화를 시작합니다.");

            String insertSql = "INSERT INTO fire_stations (name, address, geometry) " +
                    "VALUES (?, ?, ST_GeomFromText(?, 4326, 'axis-order=long-lat')) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "address = VALUES(address), geometry = VALUES(geometry)";

            Map<String, String> addressFixMap = getAddressFixMap();
            List<Object[]> insertBatchArgs = new ArrayList<>();
            List<String> failedAddresses = new ArrayList<>();
            int insertCount = 0;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                br.readLine(); // 헤더 스킵 (순번, 본부, 소방서명, 주소, 전화번호...)
                String line;

                while ((line = br.readLine()) != null) {
                    String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                    // 최소 3개 컬럼(이름, 주소)은 있어야 함
                    if (data.length < 3) continue;

                    String name = data[1].trim();
                    String rawAddress = data[2].replaceAll("\"", "").trim();

                    if (addressFixMap.containsKey(name)) {
                        String fixedAddress = addressFixMap.get(name);
                        rawAddress = fixedAddress;
                    }

                    KakaoApiResponse.KakaoDocument kakaoDoc = kakaoApiCaller.searchPlace(rawAddress);

                    // 1차 실패 시 괄호 제거 후 재검색
                    if (kakaoDoc == null && rawAddress.contains("(")) {
                        String baseAddr = rawAddress.split("\\(")[0].trim();
                        kakaoDoc = kakaoApiCaller.searchPlace(baseAddr);
                    }

                    if (kakaoDoc != null) {
                        // 카카오가 찾아준 X(경도), Y(위도)로 POINT 생성
                        String pointWkt = String.format("POINT(%s %s)", kakaoDoc.getX(), kakaoDoc.getY());

                        insertBatchArgs.add(new Object[]{
                                name, // 소방서 이름
                                kakaoDoc.getAddress_name(), // 카카오가 정제해준 깔끔한 도로명/지번 주소
                                pointWkt // 변환된 좌표
                        });
                        insertCount++;

                        if (insertBatchArgs.size() >= 100) {
                            jdbcTemplate.batchUpdate(insertSql, insertBatchArgs);
                            insertBatchArgs.clear();
                        }
                    } else {
                        failedAddresses.add(name + " (" + rawAddress + ")");
                    }
                }

                if (!insertBatchArgs.isEmpty()) {
                    jdbcTemplate.batchUpdate(insertSql, insertBatchArgs);
                }

                if (!failedAddresses.isEmpty()) {
                    log.warn("========================================");
                    log.warn("카카오 API 좌표 변환 실패 목록 (총 {}건):", failedAddresses.size());
                    failedAddresses.forEach(addr -> log.warn(" - {}", addr));
                    log.warn("========================================");
                }

                fileSyncService.updateSyncHistory(filename, fileHash);
                log.info("소방서 데이터 자동 좌표 변환 및 동기화 완료. 총 {}건 추가되었습니다.", insertCount);

            }
        } catch (Exception e) {
            log.error("소방서 데이터 로딩 중 오류 발생", e);
        }
    }

    private Map<String, String> getAddressFixMap() {
        Map<String, String> fixMap = new HashMap<>();

        // [소방서명] -> [정확한 주소]
        fixMap.put("가평소방서", "경기도 가평군 가평읍 가화로 36");
        fixMap.put("파주소방서", "경기도 파주시 파주읍 통일로 1564");
        fixMap.put("구미소방서", "경상북도 구미시 수출대로 112");
        fixMap.put("양산소방서", "경상남도 양산시 물금읍 황산로 719");
        fixMap.put("성산소방서", "경상남도 창원시 성산구 상남로 165");

        return fixMap;
    }
}