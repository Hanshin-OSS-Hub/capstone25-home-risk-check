package hanshin.home_risk_check.safetyscore.config.loader;

import hanshin.home_risk_check.safetyscore.domain.region.entity.SggSafetyStats;
import hanshin.home_risk_check.safetyscore.domain.region.repository.SggSafetyStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
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
public class CrimeStatsLoader {

    private final SggSafetyStatsRepository sggSafetyStatsRepository;
    private final FileSyncService fileSyncService;

    @Transactional
    public void loadData() {
        try {
            //  CSV 파일 찾기
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:safetyscore/crime/crime_stats.csv");

            if (resources.length == 0) {
                log.warn("범죄 통계 CSV 파일을 찾을 수 없습니다.");
                return;
            }

            Resource resource = resources[0];
            String filename = resource.getFilename();

            String fileHash = fileSyncService.calculateHash(resource);
            boolean isChanged = fileSyncService.isChanged(filename, fileHash);

            if (!isChanged) {
                log.info("범죄 통계 CSV 파일 내용이 동일하여 데이터 적재를 건너뜁니다.");
                return; // 파일이 안 변했으면 바로 종료
            }

            log.info("범죄 통계 CSV 파일 변경 감지, 데이터 동기화를 시작합니다.");

            //DB에 저장된 모든 SggSafetyStats 가져오기
            List<SggSafetyStats> existingStats = sggSafetyStatsRepository.findAll();

            // "시도이름_시군구이름"을 키로 하는 Map 생성
            Map<String, SggSafetyStats> statsMap = new HashMap<>();
            for (SggSafetyStats stat : existingStats) {
                String key = stat.getSidoNm() + "_" + stat.getSggNm();
                statsMap.put(key, stat);
            }
            log.info("현재 DB에 저장된 통계 지역 수: {}개", statsMap.size());

            int updateCount = 0;
            // 업데이트할 객체들을 담아둘 리스트
            List<SggSafetyStats> statsToUpdate = new ArrayList<>();

            // 3. 파일 읽기 및 처리

            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                br.readLine(); // 첫 줄(헤더) 스킵
                String line;

                while ((line = br.readLine()) != null) {
                    try {
                        String[] parts = line.split(",");
                        if (parts.length < 6) continue;

                        String[] regionParts = parts[0].trim().split(" ");
                        String sidoNm;
                        String sggNm;

                        //  세종시 예외처리 (DB: 세종특별자치시_세종특별자치시)
                        if (parts[0].trim().equals("세종시")) {
                            sidoNm = "세종특별자치시";
                            sggNm = "세종특별자치시";
                        } else {
                            sidoNm = convertSidoName(regionParts[0]);
                            sggNm = regionParts[1];
                        }

                        // 부산광역시 진구 예외처리
                        if (sidoNm.equals("부산광역시") && sggNm.equals("부산진구")) {
                            sggNm = "진구";
                        }

                        // 파싱
                        int robbery = Integer.parseInt(parts[1].trim());
                        int theft = Integer.parseInt(parts[2].trim());
                        int murder = Integer.parseInt(parts[3].trim());
                        int sexual = Integer.parseInt(parts[4].trim());
                        int violence = Integer.parseInt(parts[5].trim());

                        // Map에서 찾아서 업데이트
                        String key = sidoNm + "_" + sggNm;
                        SggSafetyStats targetStat = statsMap.get(key);

                        if (targetStat != null) {
                            targetStat.updateCrimeStats(robbery, theft, murder, sexual, violence);
                            statsToUpdate.add(targetStat);
                            updateCount++;
                        } else {
                            log.warn("DB에 매칭되는 지역이 없어 건너뜁니다: {}", key);
                        }

                    } catch (Exception e) {
                        log.error("행 파싱 중 에러 발생: {}", line, e);
                    }
                }
            }
            //  일괄 저장
            if (!statsToUpdate.isEmpty()) {
                sggSafetyStatsRepository.saveAll(statsToUpdate);
                log.info("범죄 데이터 동기화 완료. 총 {}건이 추가 되었습니다.", statsToUpdate.size());
            } else {
                log.info("업데이트할 범죄 데이터가 없습니다.");
            }

            fileSyncService.updateSyncHistory(filename, fileHash);

        } catch (Exception e) {
            log.error("범죄 데이터 로드 중 치명적 오류 발생", e);
        }
    }

    // 약어를 공식 행정구역 명칭으로 변환 ("서울" -> "서울특별시")
    private String convertSidoName(String shortNm) {
        return switch (shortNm) {
            case "서울" -> "서울특별시";
            case "부산" -> "부산광역시";
            case "대구" -> "대구광역시";
            case "인천" -> "인천광역시";
            case "광주" -> "광주광역시";
            case "대전" -> "대전광역시";
            case "울산" -> "울산광역시";
            case "경기도" -> "경기도";
            case "강원도" -> "강원특별자치도";
            case "충북" -> "충청북도";
            case "충남" -> "충청남도";
            case "전북" -> "전북특별자치도";
            case "전남" -> "전라남도";
            case "경북" -> "경상북도";
            case "경남" -> "경상남도";
            case "제주" -> "제주특별자치도";
            default -> shortNm;
        };
    }
}
