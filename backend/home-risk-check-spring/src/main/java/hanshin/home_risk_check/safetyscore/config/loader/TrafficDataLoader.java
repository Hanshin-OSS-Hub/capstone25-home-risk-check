package hanshin.homeriskcheck.home_risk_check_backend.safetyscore.config.loader;

import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.address.service.AddressService;
import hanshin.homeriskcheck.home_risk_check_backend.safetyscore.domain.traffic.repository.TrafficRepository;
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
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrafficDataLoader {

    private final AddressService addressService;
    private final JdbcTemplate jdbcTemplate;
    private final TrafficRepository trafficRepository;

    public void loadData() {
        try {

            Set<String> existingAddresses = trafficRepository.findAllRawAddresses();
            log.info("현재 DB에 저장된 교통사고 데이터 : {}개", existingAddresses.size());

            String sql = "INSERT INTO traffic_accidents (raw_address, standard_address, adm_cd, accident_count, death_count, geometry) " +
                    "VALUES (?, ?, ?, ?, ?, ST_GeomFromText(?, 4326))";

            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:safetyscore/accident/*.csv");

            if (resources.length == 0) {
                return;
            }

            List<Object[]> batchArgs = new ArrayList<>();
            int totalCount = 0;

            for (Resource resource : resources) {
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

                        //addressService 를 통해 주소, 위도, 경도 확보
                        // searchAddress에서 행정동코드(admCd, 도로명주소) getCoordinate에서 위경도 얻어옴
                        var addressDetail = addressService.searchAddress(rawAddress);
                        if (addressDetail != null) {
                            var coord = addressService.getCoordinate(addressDetail);

                            if (coord != null) {
                                String pointWkt = String.format("POINT(%s %s)", coord.getEntX(), coord.getEntY());

                                batchArgs.add(new Object[]{
                                        rawAddress, //주소 원문
                                        addressDetail.getRoadAddr(), //searchAddress로 정제된 주소
                                        addressDetail.getAdmCd(), //searchAddress로 찾은 행정동 코드
                                        accidentCount, // 사고 건수
                                        deathCount, // 사망자 수
                                        pointWkt // getCoordinate로 찾은 위경도
                                });
                                totalCount++;
                            }
                        }

                        if (batchArgs.size() >= 100) {
                            jdbcTemplate.batchUpdate(sql, batchArgs);
                            batchArgs.clear();
                            log.info("교통사고 데이터 {}개 정제 및 저장 중...", totalCount);
                        }
                    }
                }
            }
            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
            }

            if (totalCount > 0) {
                log.info(" 총 {}개의 교통사고 데이터 적재 완료!", totalCount);
            } else {
                log.info("추가할 신규 교통사고 데이터가 없습니다.");
            }

        } catch (Exception e) {
            log.error("교통사고 데이터 로딩 중 오류 발생", e);
        }
    }
}
