package hanshin.home_risk_check.safetyscore.config.loader;

import hanshin.home_risk_check.safetyscore.domain.cctv.repository.CctvRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CctvDataLoader {

    private final JdbcTemplate jdbcTemplate; // JDBC를 사용하여 속도 향상
    private final FileSyncService fileSyncService;

    @Transactional
    public void loadData(){
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/cctvs/*.csv");

            if (resources.length == 0) {
                log.warn("CSV 파일을 찾을 수 없습니다.");
                return;
            }

            Resource resource = resources[0];
            String filename = resource.getFilename();

            String fileHash = fileSyncService.calculateHash(resource);
            boolean isChanged = fileSyncService.isChanged(filename, fileHash);

            if (!isChanged) {
                log.info("CCTV CSV 파일 내용이 동일하여 데이터 적재를 건너뜁니다.");
                return; // 파일이 안 변했으면 바로 종료
            }

            log.info("CCTV CSV 파일 변경 감지, 데이터 동기화를 시작합니다.");

            // ST_GeomFromText: 텍스트 형식의 좌표를 DB가 인식하는 공간 객체로 변환하는 함수
            String sql = "INSERT INTO cctvs (manage_no, address, purpose, camera_count, geometry) " +
                    "VALUES (?, ?, ?, ?, ST_GeomFromText(?, 4326, 'axis-order=long-lat')) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "address = VALUES(address), purpose = VALUES(purpose), " +
                    "camera_count = VALUES(camera_count), geometry = VALUES(geometry)";


            List<Object[]> batchArgs = new ArrayList<>();
            int totalCount = 0;


            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                br.readLine(); // 헤더 스킵
                String line;

                while ((line = br.readLine()) != null) {
                    try {
                        // 쉼표 무시 정규식
                        String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                        if (data.length < 14) continue;

                        String manageNo = data[1].trim();

                        // 주소 처리 (도로명주소 data[3] 우선, 없으면 지번주소 data[4])
                        String address = (data[3] == null || data[3].trim().isEmpty()) ? data[4] : data[3];
                        address = address.replaceAll("\"", "").trim();

                        int cameraCount = 0;
                        try {
                            cameraCount = Integer.parseInt(data[6].trim());
                        } catch (Exception e) {

                        }

                        // 좌표 파싱 및 유효성 검사
                        String latStr = data[12].replaceAll("[^0-9.-]", ""); // 숫자, 마침표, 마이너스 빼고 다 제거
                        String lonStr = data[13].replaceAll("[^0-9.-]", "");

                        double lat = Double.parseDouble(latStr);
                        double lon = Double.parseDouble(lonStr);
                        if (lat < 33 || lat > 39 || lon < 124 || lon > 132) continue;

                        // MySQL 표준: 위도 경도 WKT 생성
                        String pointWkt = "POINT(" + lon + " " + lat + ")";

                        batchArgs.add(new Object[]{ manageNo, address, data[5].trim(), cameraCount, pointWkt });
                        totalCount++;

                        //  1000개 단위로 save
                        if (batchArgs.size() == 1000) {
                            jdbcTemplate.batchUpdate(sql, batchArgs);
                            batchArgs.clear();

                            if (totalCount % 30000 == 0) {
                                log.info("{}개 CCTV 처리 완료...", totalCount);
                            }
                        }

                    } catch (Exception e) {
                        log.warn("CCTV CSV 파싱 중 개별 행 오류 발생.", e);
                    }
                }
            } catch (IOException e) {
                log.error("파일 읽기 실패: {}", resource.getFilename());
            }

            // 남은 데이터 처리
            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
            }

            fileSyncService.updateSyncHistory(filename, fileHash);

            log.info("CCTV 데이터 동기화 완료. 총 {} 건이 추가 되었습니다.", totalCount);

        } catch (Exception e) {
            log.error("CCTV 데이터 로드 중 치명적 오류 발생", e);
        }
    }
}