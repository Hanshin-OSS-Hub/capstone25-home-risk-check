package hanshin.home_risk_check.safetyscore.config.loader;


import hanshin.home_risk_check.safetyscore.domain.police.entity.PoliceStation;
import hanshin.home_risk_check.safetyscore.domain.police.repository.PoliceStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PoliceDataLoader {

    private final JdbcTemplate jdbcTemplate;
    private final FileSyncService fileSyncService;

    @Transactional
    public void loadData() {
        try {
            // 파일 가져오기
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/police/*.csv");

            if (resources.length == 0){
                log.warn("CSV 파일이 없습니다.");
                return;
            }

            String sql = "INSERT INTO police_stations (name, type, address, geometry) " +
                    "VALUES (?, ?, ?, ST_GeomFromText(?, 4326, 'axis-order=long-lat')) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "type = VALUES(type), address = VALUES(address), geometry = VALUES(geometry)";

            int totalProcessedCount = 0;

            for (Resource resource : resources){
                //파일명에 따라 파싱 방법 결정
                String filename = resource.getFilename();
                String fileHash = fileSyncService.calculateHash(resource);
                boolean isChanged = fileSyncService.isChanged(filename, fileHash);

                if (!isChanged) {
                    log.info("경찰서 CSV 파일 내용이 동일하여 데이터 적재를 건너뜁니다.");
                    continue;// 파일이 안 변했으면 바로 종료
                }
                log.info("{} 파일 변경 감지, 데이터 동기화를 시작합니다.", filename);

                String mode = (filename != null && filename.contains("substation")) ? "SUB" : "MAIN";

                //파일 읽어오기
                int count = processFileContent(resource, mode, sql);
                totalProcessedCount += count;

                fileSyncService.updateSyncHistory(filename, fileHash);
            }

            if (totalProcessedCount > 0) {
                log.info("경찰관서 데이터  동기화 완료. 총 {}건이 추가 되었습니다.", totalProcessedCount);
            }
        } catch (Exception e) {
            log.error("경찰서 데이터 로드 중 오류 발생");
        }

    }

    /**
     * CSV 파일 내용을 읽어 파싱하여 JDBC 로 DB에 넣음
     */
    private int processFileContent(Resource resource, String mode, String sql){
        // 해당 Mode에 맞는 데이터 보정값 가져오기
        Map<String,double[]> fixMap = getFixMap(mode);

        List<Object[]> batchArgs = new ArrayList<>();
        int processedCount = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            br.readLine(); // 헤더 스킵
            String line;

            while ((line = br.readLine()) != null ){
                try{
                    //주소안의 ,를 무시하고 컬럼을 나누는 정규식
                    String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                    String name, type, address;
                    double lat = 0.0, lon = 0.0;

                    if (mode.equals("MAIN")) { //police_converted.csv
                        if (data.length < 7) { //최소 7개 컬럼이 없으면 데이터 누락으로 간주하고 스킵하여 에러 방지
                            continue;
                        }
                        name = data[2].trim();
                        type = "경찰서";
                        address = data[3].replaceAll("\"", "").trim();

                        // fixMap 확인: 보정 대상이라면 CSV의 잘못된 값 대신 정확한 좌표로 덮어씀
                        if (fixMap.containsKey(name)) {
                            lat = fixMap.get(name)[0];
                            lon = fixMap.get(name)[1];
                        } else {
                            lon = Double.parseDouble(data[5].replaceAll("[^0-9.-]", "")); // x_axis
                            lat = Double.parseDouble(data[6].replaceAll("[^0-9.-]", "")); // y_axis}
                        }
                    } else {
                        if (data.length < 10) continue;
                        // 경찰서명과 관서명을 합쳐서 고유 이름 생성
                        name = data[2].trim() + " " + data[3].trim(); // "서울서부 녹번"
                        type = data[4].trim();
                        address = data[6].replaceAll("\"", "").trim();

                        // fixMap 확인: 보정 대상이라면 CSV의 잘못된 값 대신 정확한 좌표로 덮어씀
                        if (fixMap.containsKey(name)) {
                            lat = fixMap.get(name)[0];
                            lon = fixMap.get(name)[1];
                        } else {
                            lon = Double.parseDouble(data[8].replaceAll("[^0-9.-]", "")); // x_axis
                            lat = Double.parseDouble(data[9].replaceAll("[^0-9.-]", "")); // y_axis}
                        }
                    }


                    // 한국 위도 범위를 벗어난 잘못된 데이터 좌표 걸러냄
                    if (lat < 33 || lat > 39) continue;

                    // WKT 문자열 형식으로 변환
                    String pointWkt = "POINT(" + lon + " " + lat + ")";

                    batchArgs.add(new Object[]{name, type, address, pointWkt});
                    processedCount++;

                    if (batchArgs.size() == 1000) {
                        jdbcTemplate.batchUpdate(sql, batchArgs);
                        batchArgs.clear();
                    }

                } catch (Exception e){
                    log.error("라인 처리 중 문제 발생 : {} ", line);
                }
            }
            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
            }
        } catch (Exception e){
            log.error("파일 읽는중 문제 발생 : {} ", resource.getFilename(), e);
        }

        return processedCount;
    }

    private Map<String, double[]> getFixMap(String mode) {
        Map<String, double[]> fixMap = new HashMap<>();

        if (mode.equals("MAIN")) {
            // [police_converted.csv]  데이터 보정 목록
            fixMap.put("서울강서경찰서", new double[]{37.5513, 126.8509});
            fixMap.put("부산남부경찰서", new double[]{35.1365, 129.0843});
            fixMap.put("부산강서경찰서", new double[]{35.1749, 128.9467});
            fixMap.put("부산북부경찰서", new double[]{35.1979, 128.9904});
            fixMap.put("대구남부경찰서", new double[]{35.8427, 128.5834});
            fixMap.put("대구북부경찰서", new double[]{35.8853, 128.5819});
            fixMap.put("대구강북경찰서", new double[]{35.9238, 128.5559});
            fixMap.put("광주남부경찰서", new double[]{35.1329, 126.9034});
            fixMap.put("광주북부경찰서", new double[]{35.1751, 126.9152});
            fixMap.put("울산남부경찰서", new double[]{35.5357, 129.3179});
            fixMap.put("울산북부경찰서", new double[]{35.5823, 129.3562});
            fixMap.put("광주의왕경찰서", new double[]{37.4116, 127.2566}); // 경기도 광주 오타 보정
            fixMap.put("마산동부경찰서", new double[]{35.2223, 128.5913});

        } else {
            // [substation.csv] 지구대 및 파출소 데이터 보정 목록
            fixMap.put("서울서부 녹번", new double[]{37.6080, 126.9294});
            fixMap.put("부산금정 범어", new double[]{35.2588, 129.0915});
            fixMap.put("인천중부 덕적", new double[]{37.2363, 126.1200});
            fixMap.put("인천남동 간석4", new double[]{37.4727, 126.7025});
            fixMap.put("인천서부 서곶", new double[]{37.5584, 126.6769});
            fixMap.put("광주서부 염주", new double[]{35.1384, 126.8795});
            fixMap.put("남양주남부 다산1", new double[]{37.6117, 127.1720});
            fixMap.put("포천 소흘", new double[]{37.7942, 127.1490});
            fixMap.put("아산 신창", new double[]{36.7907, 126.9338});
            fixMap.put("보령 웅천", new double[]{36.2403, 126.5872});
            fixMap.put("예산 신례원신암", new double[]{36.7037, 126.8550});
            fixMap.put("완도 군외", new double[]{34.3689, 126.6731});
            fixMap.put("완도 금일", new double[]{34.3503, 127.0438});
            fixMap.put("완도 보길", new double[]{34.1525, 126.5467});
            fixMap.put("완도 금당", new double[]{34.4368, 127.0524});
            fixMap.put("포항남부 구룡포", new double[]{35.9879, 129.5522});
            fixMap.put("포항남부 동해", new double[]{35.9977, 129.4869});
            fixMap.put("고령 대가야", new double[]{35.7291, 128.2568});
        }

        return fixMap;
    }

}
