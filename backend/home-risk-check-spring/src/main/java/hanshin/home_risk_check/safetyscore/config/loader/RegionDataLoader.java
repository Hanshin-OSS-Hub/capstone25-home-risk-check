package hanshin.home_risk_check.safetyscore.config.loader;


import hanshin.home_risk_check.safetyscore.domain.region.entity.Region;
import hanshin.home_risk_check.safetyscore.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.union.UnaryUnionOp;
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
public class RegionDataLoader {

    private final JdbcTemplate jdbcTemplate;
    private final FileSyncService fileSyncService;

    @Transactional
    public boolean loadData(){
        try {

            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/regions/*.csv");

            if(resources.length == 0) {
                log.warn("CSV 파일을 찾을수 없습니다.");
                return false;
            }

            Resource resource = resources[0];
            String filename = resource.getFilename();

            String fileHash = fileSyncService.calculateHash(resource);
            boolean isChanged = fileSyncService.isChanged(filename, fileHash);

            if (!isChanged) {
                log.info("지역 데이터 CSV 파일 내용이 동일하여 데이터 적재를 건너뜁니다.");
                return false; // 파일이 안 변했으면 바로 종료
            }

            log.info("지역 데이터 CSV 파일 변경 감지, 데이터 동기화를 시작합니다.");

            Map<String, String> nameMap = new HashMap<>(); //코드별 이름 저장
            Map<String, List<Geometry>> geometryMap = new HashMap<>(); // 코드별 지형 리스트 저장
            int totalCount = 0;

            // JTS 공간 데이터 변환용 객체
            WKTReader wktReader = new WKTReader();
            GeometryFactory geometryFactory = new GeometryFactory();

            try (BufferedReader br= new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))){
                String line;
                boolean isFirstLine = true; //Header 건너띄기

                while((line = br.readLine()) != null){
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    totalCount++;

                    String[] data = parseCsvLine(line);

                    if(data.length >= 4) {
                        // ""를 빈 문자열로 변환
                        String wktString = data[0].replace("\"", ""); // 공간 데이터
                        String admCode = data[2].replace("\"", ""); // 행정동 코드
                        String admNm = data[3].replace("\"", ""); // 행정동 이름

                        nameMap.put(admCode, admNm);

                        try {
                            //WKT 문자열 -> JTS geometry 객체 변환
                            Geometry geometry = wktReader.read(wktString);
                            geometryMap.computeIfAbsent(admCode, k -> new ArrayList<>()).add(geometry);

                        } catch (ParseException e) {
                            log.error("WKT 공간 데이터 파싱 오류 (행정동 코드: {}): {}", admCode, e.getMessage());
                        }
                    }
                }
            }

            String sql = "INSERT INTO safety_regions (adm_code, adm_nm, geometry) " +
                    "VALUES (?, ?, ST_GeomFromText(?, 4326, 'axis-order=long-lat')) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "adm_nm = VALUES(adm_nm), geometry = VALUES(geometry)";

            List<Object[]> batchArgs = new ArrayList<>();
            int processedCount = 0;


            for (String admCode: geometryMap.keySet()){
                List<Geometry> geometries = geometryMap.get(admCode);
                //GeometryCombiner 행정동 경계 중복 -> UnaryUnionOp로 변경
                Geometry combined = UnaryUnionOp.union(geometries);

                // MultiPolygon 객체로 통일
                MultiPolygon multiPolygon = convertToMultiPolygon(combined, geometryFactory);

                if (multiPolygon != null) {
                    String combinedWkt = multiPolygon.toText();

                    batchArgs.add(new Object[]{admCode, nameMap.get(admCode), combinedWkt});
                    processedCount++;

                    if (batchArgs.size() == 1000) {
                        jdbcTemplate.batchUpdate(sql, batchArgs);
                        batchArgs.clear();
                    }
                }
            }

            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
            }

            fileSyncService.updateSyncHistory(filename, fileHash);

            log.info("지역 데이터 동기화 완료. 총 {} 건이 추가 되었습니다.", processedCount);
            return true;

        } catch (Exception e){
            log.error("행정동 데이터 로드 중 치명적 오류 발생", e);
            return false;
        }
    }

    private MultiPolygon convertToMultiPolygon(Geometry geometry, GeometryFactory factory) {
        if (geometry instanceof MultiPolygon) {
            return (MultiPolygon) geometry;
        }
        if (geometry instanceof Polygon) {
            return factory.createMultiPolygon(new Polygon[]{(Polygon) geometry});
        }
        return null;
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\"') {
                inQuotes = !inQuotes; // 따옴표 안/밖 상태 변경
            } else if (c == ',' && !inQuotes) {
                // 따옴표 밖에서 쉼표를 만나면 단어의 끝이므로 리스트에 추가
                tokens.add(sb.toString().trim());
                sb.setLength(0); // StringBuilder 초기화
            } else {
                // 그 외의 문자는 모두 저장
                sb.append(c);
            }
        }
        // 마지막 남은 단어 추가
        tokens.add(sb.toString().trim());

        return tokens.toArray(new String[0]);
    }

}