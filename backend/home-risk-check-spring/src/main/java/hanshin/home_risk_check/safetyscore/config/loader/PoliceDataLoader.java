package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.config.loader;

import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.police.entity.PoliceStation;
import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.police.repository.PoliceStationRepository;
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
    private final PoliceStationRepository policeStationRepository;

    @Transactional
    public void loadData() {
        try {
            // 이미 존재하는 데이터 인지 조회
            Set<String> existingNames = policeStationRepository.findAllNames();
            log.info("현재 DB에 저장된 경찰관서: {}개", existingNames.size());

            // 임시 저장용 Map (중복 파일 제거용)
            Map<String, PoliceStation> stationMap = new HashMap<>();

            // 파일 가져오기
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/police/*.csv");

            if (resources.length == 0){
                log.warn("CSV 파일이 없습니다.");
                return;
            }

            for (Resource resource : resources){
                //파일명에 따라 파싱 방법 결정
                String filename = resource.getFilename();
                String mode = (filename != null && filename.contains("substation")) ? "SUB" : "MAIN";

                //파일 읽어오기
                processFileContent(resource, mode, existingNames, stationMap);
            }

            if (!stationMap.isEmpty()){
                // Map에서 value값만 가져오기
                List<PoliceStation> newStations = new ArrayList<>(stationMap.values());
                policeStationRepository.saveAll(newStations);
                log.info("신규 경찰관서 {}개 저장 완료", newStations.size());
            }
            else {
                log.info("추가할 신규 경찰관서 데이터가 없습니다.");
            }
        } catch (Exception e) {
            log.error("경찰서 데이터 로드 중 오류 발생");
        }

    }

    /**
     * CSV 파일 내용을 읽어 파싱하고, 중복 제거 및 보정 후 임시 맵에 저장하는 메서드
     */
    private void processFileContent(Resource resource, String mode, Set<String> existingNames, Map<String, PoliceStation> stationMap){
        // 해당 Mode에 맞는 데이터 보정값 가져오기
        Map<String,double[]> fixMap = getFixMap(mode);
        GeometryFactory geometryFactory = new GeometryFactory();

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
                            lon = Double.parseDouble(data[5]); // x_axis
                            lat = Double.parseDouble(data[6]); // y_axis}
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
                            lon = Double.parseDouble(data[8]); // x_axis
                            lat = Double.parseDouble(data[9]); // y_axis}
                        }
                    }

                    // DB에 이미 존재하는 이름이면 건너띔
                    if (existingNames.contains(name)) {
                        continue;
                    }


                    // 한국 위도 범위를 벗어난 잘못된 데이터 좌표 걸러냄
                    if (lat < 33 || lat > 39) continue;

                    // JTS 공간 객체 생성 ,MySQL 8.0의 4326 좌표계 표준에 맞춰 POINT(위도 경도) 순서로 생성
                    Point location = geometryFactory.createPoint(new Coordinate(lon, lat));
                    location.setSRID(4326); // WGS84 좌표계 부여

                    // Map에 담아둠 (이미 처리된 이름이 다시 나올 경우 최신 값으로 자동 갱신되어 파일 내 중복 방지)
                    stationMap.put(name, PoliceStation.builder()
                            .name(name)
                            .type(type)
                            .address(address)
                            .geometry(location)
                            .build());
                } catch (Exception e){
                    log.error("라인 처리 중 문제 발생 : {} ", line);
                }

            }
        } catch (Exception e){
            log.error("파일 읽는중 문제 발생 : {} ", resource.getFilename(), e);
        }
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
