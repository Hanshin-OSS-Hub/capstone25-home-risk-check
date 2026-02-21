package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.config.loader;


import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.region.entity.Region;
import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.GeometryCombiner;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
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

    private final RegionRepository regionRepository;

    @Transactional
    public void loadData(){
        try {

            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/regions/*.csv");

            if(resources.length == 0) {
                log.warn("CSV 파일을 찾을수 없습니다.");
                return;
            }

            // 중복 확인용 기존 코드 조회
            Set<String> existingCodes = regionRepository.findAllAdmCodes();
            log.info("현재 DB에 저장된 행정동: {}개", existingCodes.size());

            Map<String, String> nameMap = new HashMap<>(); //코드별 이름 저장
            Map<String, List<Geometry>> geometryMap = new HashMap<>(); // 코드별 지형 리스트 저장

            int totalCount = 0;
            // JTS 공간 데이터 변환용 객체
            WKTReader wktReader = new WKTReader();
            GeometryFactory geometryFactory = new GeometryFactory();

            for (Resource resource : resources){
                try (BufferedReader br= new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))){
                    String line;
                    boolean isFirstLine = true; //Header 건너띄기

                    while((line = br.readLine()) != null){
                        if (isFirstLine) {
                            isFirstLine = false;
                            continue;
                        }

                        totalCount++;

                        String[] data = line.split(";");

                        if(data.length >= 4) {
                            // ""를 빈 문자열로 변환
                            String wktString = data[0].replace("\"", ""); // 공간 데이터
                            String admCode = data[1].replace("\"", ""); // 행정동 코드
                            String admNm = data[3].replace("\"", ""); // 행정동 이름

                            if (existingCodes.contains(admCode)) {
                                continue;
                            }
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
            }

            List<Region> newRegions = new ArrayList<>();

            for (String admCode: geometryMap.keySet()){
                List<Geometry> geometries = geometryMap.get(admCode);
                //GeometryCombiner 행정동 경계 중복 -> UnaryUnionOp로 변경
                Geometry combined = UnaryUnionOp.union(geometries);


                // MultiPolygon 객체로 통일
                MultiPolygon multiPolygon = convertToMultiPolygon(combined, geometryFactory);

                if (multiPolygon != null) {

                    multiPolygon.setSRID(5186); //공간 데이터의 좌표계를 5186(대한민국 중부원점)으로 지정

                    newRegions.add(Region.builder()
                            .admCode(admCode)
                            .admNm(nameMap.get(admCode))
                            .geometry(multiPolygon)
                            .build());
                }
            }

            if (!newRegions.isEmpty()){
                regionRepository.saveAll(newRegions);
                log.info("CSV 총 행수: {}, 신규 저장된 행정동 : {} 개", totalCount, newRegions.size());
            } else {
                log.info("추가할 신규 행정동 데이터가 없습니다.");
            }

        } catch (Exception e){
            log.error("행정동 데이터 로드 중 치명적 오류 발생", e);
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

}