package hanshin.home_risk_check.safetyscore.config;

import hanshin.home_risk_check.safetyscore.domain.region.entity.Region;
import hanshin.home_risk_check.safetyscore.domain.region.repository.RegionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpatialRegionIndex {

    private final RegionRepository regionRepository;
    private final STRtree index = new STRtree();
    private boolean isInitialized = false;

    // 트리 내부에 담아둘 임시 DTO
    private record RegionData(String sgisCode, Geometry polygon) {}

    @PostConstruct
    public void init() {
        if (isInitialized) {
            return;
        }

        log.info("공간 인덱스(STRtree) 초기화 시작...");
        List<Region> regions = regionRepository.findAll();

        if (regions.isEmpty()) {
            log.warn("행정동 데이터가 비어있어 공간 인덱스를 초기화할 수 없습니다.");
            return;
        }

        for (Region region : regions) {
            if (region.getGeometry() != null) {
                // Envelope(MBR)를 키로 사용하여 저장
                index.insert(region.getGeometry().getEnvelopeInternal(),
                        new RegionData(region.getSgisCode(), region.getGeometry()));
            }
        }
        index.build();
        isInitialized = true;
        log.info("공간 인덱스 초기화 완료 (총 {}개 지역)", regions.size());
    }

    /**
     * 좌표를 입력받아 해당하는 행정동 코드를 반환
     */
    public String findSgisCode(Point point) {
        if (point == null || !isInitialized) return null;

        // 점 근처에 있는 다각형 후보군을 가져옴(MBR(최소 외곽 사각형) 필터링)
        List<?> candidates = index.query(point.getEnvelopeInternal());

        // 후보군 중에서 실제로 점을 포함하는 다각형 찾기
        for (Object obj : candidates) {
            RegionData data = (RegionData) obj;
            if (data.polygon().covers(point)) {
                return data.sgisCode();
            }
        }

        // 경계, 바다등을 관리해 포함되는 폴리곤이 없을때, 가장 가까운 행정동으로 편입
        double minDistance = Double.MAX_VALUE;
        String closestCode = null;

        // 허용 오차: 약 0.0005도 (실제 거리로 약 50미터 내외)
        double threshold = 0.0005;

        for (Object obj : candidates) {
            RegionData data = (RegionData) obj;
            // 점과 다각형 사이의 최단 거리 계산
            double dist = data.polygon().distance(point);
            if (dist < minDistance && dist <= threshold) {
                minDistance = dist;
                closestCode = data.sgisCode();
            }
        }
        // 가장가까운 동네 코드 반환, 없으면 null
        return closestCode;
    }
}
