package hanshin.home_risk_check.safetyscore.config.loader;

import hanshin.home_risk_check.safetyscore.domain.cctv.repository.CctvRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
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

    // JTS 지오메트리 팩토리 (4326 좌표계 설정)
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public boolean loadData(){
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/cctvs/*.csv");

            if (resources.length == 0) {
                log.warn("CSV 파일을 찾을 수 없습니다.");
                return false;
            }

            Resource resource = resources[0];
            String filename = resource.getFilename();

            String fileHash = fileSyncService.calculateHash(resource);
            boolean isChanged = fileSyncService.isChanged(filename, fileHash);

            if (!isChanged) {
                log.info("CCTV CSV 파일 내용이 동일하여 데이터 적재를 건너뜁니다.");
                return false; // 파일이 안 변했으면 바로 종료
            }

            log.info("CCTV CSV 파일 변경 감지, 데이터 동기화를 시작합니다.");

            //  메모리에 행정동 공간 인덱스(STRtree) 구축하기
            log.info("행정동 공간 데이터를 Java 메모리에 저장");
            STRtree spatialIndex = buildSpatialIndex();
            log.info("행정동 공간 데이터 로드 완료");

            // ST_GeomFromText: 텍스트 형식의 좌표를 DB가 인식하는 공간 객체로 변환하는 함수
            String insertSql = """
                INSERT INTO cctvs (manage_no, address, purpose, camera_count, geometry, adm_code)
                VALUES (?, ?, ?, ?, ST_GeomFromText(?, 4326, 'axis-order=long-lat'), ?)
                ON DUPLICATE KEY UPDATE
                    address = VALUES(address),
                    purpose = VALUES(purpose),
                    camera_count = VALUES(camera_count),
                    geometry = VALUES(geometry),
                    adm_code = VALUES(adm_code)
                """;

            List<Object[]> batchArgs = new ArrayList<>();
            int totalCount = 0;
            int mappedCount = 0; //행정동 매핑 카운트

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

                        // java에서 행정동 코드 찾기
                        // mysql은 SRID 4326에 대해 lat, lon으로 저장, WKTReader는 x,y 순서로 읽어서 lat, lon으로 형식 통일
                        Point cctvPoint = geometryFactory.createPoint(new Coordinate(lat, lon));
                        String admCode = findAdmCodeFromMemory(spatialIndex, cctvPoint);

                        if (admCode != null) {
                            mappedCount++;
                        }

                        batchArgs.add(new Object[]{ manageNo, address, data[5].trim(), cameraCount, pointWkt, admCode });
                        totalCount++;

                        //  1000개 단위로 save
                        if (batchArgs.size() == 1000) {
                            jdbcTemplate.batchUpdate(insertSql, batchArgs);
                            batchArgs.clear();

                            if (totalCount % 30000 == 0) {
                                log.info("{}개 CCTV 처리. {}개 매핑 완료...", totalCount, mappedCount);
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
                jdbcTemplate.batchUpdate(insertSql, batchArgs);
            }

            fileSyncService.updateSyncHistory(filename, fileHash);

            log.info("CCTV 데이터 동기화 완료. 총 {} 건이 추가 되었습니다.", totalCount);
            return true;

        } catch (Exception e) {
            log.error("CCTV 데이터 로드 중 치명적 오류 발생", e);
            return false;
        }
    }

    /**
     * Db에서 행정동 데이터가져와 메모리에 저장
     */
    private STRtree buildSpatialIndex() {
        STRtree index = new STRtree(); // 공간 데이터 전용 index
        WKTReader wktReader = new WKTReader(geometryFactory);

        // ST_AsText로 DB의 공간 데이터를 텍스트(WKT) 가져옴
        String sql = "SELECT adm_code, ST_AsText(geometry) as geom_wkt FROM safety_regions";

        jdbcTemplate.query(sql, rs -> {
            String admCode = rs.getString("adm_code");
            String geomWkt = rs.getString("geom_wkt");

            try {
                // 텍스트를 Geometry로 변환
                Geometry polygon = wktReader.read(geomWkt);
                index.insert(polygon.getEnvelopeInternal(), new RegionData(admCode, polygon));

            } catch (ParseException e) {
                log.warn("행정동 WKT 파싱 오류 : adm_code={}", admCode);
            }
        });

        index.build();
        return index;
    }

    /**
     * 메모리 트리에서 주어진 좌표가 포함된 행정동 코드를 찾음
     */
    private String findAdmCodeFromMemory(STRtree index, Point point){
        // 점 근처에 있는 다각형 후보군을 가져옴(MBR(최소 외곽 사각형) 필터링)
        List<?> candidates = index.query(point.getEnvelopeInternal());

        // 후보군 중에서 실제로 점을 포함하는 다각형 찾기
        for (Object obj : candidates) {
            RegionData region = (RegionData) obj;

            //RegionData의 polygon이 point 좌표를 포함하면 행정동 코드 반환
            if (region.polygon().covers(point)) {
                return region.admCode();
            }
        }

        // 경계, 바다등을 관리해 포함되는 폴리곤이 없을때, 가장 가까운 행정동으로 편입
        String closestAdmCode = null;
        double minDistance = Double.MAX_VALUE;

        // 허용 오차: 약 0.0005도 (실제 거리로 약 50미터 내외)
        double distanceThreshold = 0.0005;

        for (Object obj : candidates) {
            RegionData region = (RegionData) obj;

            // 점과 다각형 사이의 최단 거리 계산
            double distance = region.polygon().distance(point);

            if (distance < minDistance && distance <= distanceThreshold) {
                minDistance = distance;
                closestAdmCode = region.admCode();
            }
        }

        // 가장가까운 동네 코드 반환, 없으면 null
        return closestAdmCode;
    }

    // 트리 내부에 담아둘 임시 DTO
    private record RegionData(String admCode, Geometry polygon){}
}