package hanshin.home_risk_check.safetyscore.config.loader;

import hanshin.home_risk_check.safetyscore.domain.accident.repository.TrafficRepository;
import hanshin.home_risk_check.safetyscore.infra.api.KakaoApiCaller;
import hanshin.home_risk_check.safetyscore.infra.dto.KakaoApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccidentPointLoader {

    private final KakaoApiCaller kakaoApiCaller;
    private final JdbcTemplate jdbcTemplate;
    private final TrafficRepository trafficRepository;
    private final FileSyncService fileSyncService;

    public void loadData() {
        try {

            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/accident/accident_hotspots.csv");

            if (resources.length == 0) {
                log.warn("CSV 파일을 찾을 수 없습니다.");
                return;
            }

            Resource resource = resources[0];
            String filename = resource.getFilename();

            String fileHash = fileSyncService.calculateHash(resource);
            boolean isChanged = fileSyncService.isChanged(filename, fileHash);

            if (!isChanged) {
                log.info("교통사고 다발지역 CSV 파일 내용이 동일하여 데이터 적재를 건너뜁니다.");
                return; // 파일이 안 변했으면 바로 종료
            }

            log.info("교통사고 다발지역 CSV 파일 변경 감지, 데이터 동기화를 시작합니다.");

            Set<String> existingAddresses = trafficRepository.findAllRawAddresses();

            // API호출을 적게 하기위해 이미 주소를 알고 있는 경우 update만
            String insertSql = "INSERT INTO traffic_accidents (raw_address, standard_address, adm_cd, accident_count, death_count, geometry) " +
                    "VALUES (?, ?, ?, ?, ?, ST_GeomFromText(?, 4326, 'axis-order=long-lat'))";
            String updateSql = "UPDATE traffic_accidents SET accident_count = ?, death_count = ? WHERE raw_address = ?";

            List<Object[]> insertBatchArgs = new ArrayList<>();
            List<Object[]> updateBatchArgs = new ArrayList<>();

            int insertCount = 0;
            int updateCount = 0;
            List<String> failedAddresses = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                br.readLine(); // 헤더 스킵
                String line;

                while ((line = br.readLine()) != null) {
                    String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (data.length < 5) {
                        continue;
                    }

                    String rawAddress = data[1].trim();
                    int accidentCount = Integer.parseInt(data[2].trim());
                    int deathCount = Integer.parseInt(data[4].trim());

                    //이미 DB에 저장된 주소는 API 호출 X
                    if (existingAddresses.contains(rawAddress)) {
                        updateBatchArgs.add(new Object[]{accidentCount, deathCount, rawAddress});
                        updateCount++;

                        if (updateBatchArgs.size() >= 100) {
                            jdbcTemplate.batchUpdate(updateSql, updateBatchArgs);
                            updateBatchArgs.clear();
                        }
                        continue;
                    }

                    // DB에 없는 주소 일시 카카오 API 호출
                    KakaoApiResponse.KakaoDocument kakaoDoc = kakaoApiCaller.searchPlace(rawAddress);

                    //1차 시도에 실패시 동네로만 저장
                    if (kakaoDoc == null && rawAddress.contains("(")) {
                        String baseAddr = rawAddress.split("\\(")[0].trim(); // "서울 성북구 동선동4가"
                        kakaoDoc = kakaoApiCaller.searchPlace(baseAddr);
                    }

                    if (kakaoDoc != null) {
                        String pointWkt = String.format("POINT(%s %s)", kakaoDoc.getX(), kakaoDoc.getY());

                        insertBatchArgs.add(new Object[]{
                                rawAddress, //주소 원문
                                kakaoDoc.getAddress_name(), // 카카오가 찾아준 정제된 주소
                                "", // 카카오 키워드 검색엔 adm_cd가 없으므로 빈값 처리 (필요시 추후 보완)
                                accidentCount, // 사고 건수
                                deathCount, // 사망자 수
                                pointWkt // getCoordinate로 찾은 위경도
                        });
                        insertCount++;

                        if (insertBatchArgs.size() >= 100) {
                            jdbcTemplate.batchUpdate(insertSql, insertBatchArgs);
                            insertBatchArgs.clear();
                            log.info("신규 교통사고 데이터 {}개 정제 및 저장 중...", insertCount);
                        }
                    } else {
                        failedAddresses.add(rawAddress);
                    }
                }

                if (!insertBatchArgs.isEmpty()) {
                    jdbcTemplate.batchUpdate(insertSql, insertBatchArgs);
                }
                if (!updateBatchArgs.isEmpty()) {
                    jdbcTemplate.batchUpdate(updateSql, updateBatchArgs);
                }

                if (!failedAddresses.isEmpty()) {
                    log.warn("========================================");
                    log.warn("카카오 API 검색 실패 목록 (총 {}건):", failedAddresses.size());
                    failedAddresses.forEach(addr -> log.warn(" - {}", addr));
                    log.warn("========================================");
                }

                fileSyncService.updateSyncHistory(filename, fileHash);

                log.info("교통사고 다발지역 데이터 동기화 완료. 총 {} 건이 추가되었습니다. ", insertCount);

            }
        } catch (Exception e) {
            log.error("교통사고 다발지역 데이터 로딩 중 오류 발생", e);
        }
    }
}
