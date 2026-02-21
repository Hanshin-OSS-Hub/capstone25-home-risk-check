package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.config.loader;

import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.cctv.repository.CctvRepository;
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
    private final CctvRepository cctvRepository;

    @Transactional
    public void loadData(){
        try {
            Set<String> existingIds = cctvRepository.findAllManageNos();

            log.info("현재 DB에 저장된 CCTV : {}  개", existingIds.size());

            // ST_GeomFromText: 텍스트 형식의 좌표를 DB가 인식하는 공간 객체로 변환하는 함수
            String sql = "INSERT INTO cctvs (manage_no, address, purpose, camera_count, geometry) " +
                    "VALUES (?, ?, ?, ?, ST_GeomFromText(?, 4326))";

            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/cctvs/*.csv");

            if(resources.length == 0) {
                log.warn("CSV 파일을 찾을수 없습니다.");
                return;
            }

            List<Object[]> batchArgs = new ArrayList<>();
            int totalCount = 0;

            for (Resource resource : resources) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                    br.readLine(); // 헤더 스킵
                    String line;
                    while ((line = br.readLine()) != null) {
                        try {
                            // 쉼표 무시 정규식
                            String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                            if (data.length < 14) continue;

                            String manageNo = data[1].trim();
                            if (existingIds.contains(manageNo)) continue;

                            // 주소 처리 (도로명주소 data[3] 우선, 없으면 지번주소 data[4])
                            String address = (data[3] == null || data[3].trim().isEmpty()) ? data[4] : data[3];
                            address = address.replaceAll("\"", "").trim();

                            int cameraCount = 0;
                            try {
                                cameraCount = Integer.parseInt(data[6].trim());
                            } catch (Exception e) {

                            }

                            // 좌표 파싱 및 유효성 검사
                            double lat = Double.parseDouble(data[12]);
                            double lon = Double.parseDouble(data[13]);
                            if (lat < 33 || lat > 39 || lon < 124 || lon > 132) continue;

                            // MySQL 표준: 위도 경도 WKT 생성
                            String pointWkt = String.format("POINT(%f %f)", lat, lon);

                            batchArgs.add(new Object[]{ manageNo, address, data[5].trim(), cameraCount, pointWkt });
                            totalCount++;

                            //  1000개 단위로 save
                            if (batchArgs.size() == 1000) {
                                jdbcTemplate.batchUpdate(sql, batchArgs);
                                batchArgs.clear();
                                log.info("{}개 CCTV 저장 중...", totalCount);
                            }

                        } catch (Exception e) { /* 개별 행 에러 스킵 */ }
                    }
                } catch (IOException e) {
                    log.error("파일 읽기 실패: {}", resource.getFilename());
                }
            }

            // 남은 데이터 처리
            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
            }

            if (totalCount > 0) {
                log.info("총 {}개의 신규 CCTV 데이터가 저장되었습니다", totalCount);
            } else {
                log.info("추가할 신규 CCTV 데이터가 없습니다.");
            }

        } catch (Exception e) {
            log.error("CCTV 데이터 로드 중 치명적 오류 발생", e);
        }
    }
}