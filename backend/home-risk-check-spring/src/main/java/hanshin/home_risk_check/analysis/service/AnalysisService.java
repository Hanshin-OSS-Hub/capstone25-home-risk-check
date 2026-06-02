package hanshin.home_risk_check.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hanshin.home_risk_check.analysis.dto.*;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import hanshin.home_risk_check.analysis.entity.AnalysisHistory;
import hanshin.home_risk_check.analysis.infra.JeonseFraudApiClient;
import hanshin.home_risk_check.analysis.infra.JeonseFraudApiClient.PredictSubmission;
import hanshin.home_risk_check.analysis.infra.dto.PredictPollData;
import hanshin.home_risk_check.analysis.mapper.AnalysisHistoryMapper;
import hanshin.home_risk_check.analysis.repository.AnalysisHistoryRepository;
import hanshin.home_risk_check.safetyscore.domain.score.dto.SafetyScoreResponse;
import hanshin.home_risk_check.safetyscore.domain.score.service.SafetyScoreService;
import hanshin.home_risk_check.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/*
 * 전세사기 위험도 분석 흐름 조정.
 *
 * - submit: FastAPI에 분석 요청 전달. 캐시히트면 즉시 합본 결과, 아니면 taskId 발급.
 * - getStatus: FastAPI 폴링. 완료 시 안전등급을 함께 계산해 합본 + 이력 저장.
 *
 * 전세사기 위험도와 안전등급 결과를 하나의 응답으로 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final JeonseFraudApiClient apiClient;
    private final SafetyScoreService safetyScoreService;
    private final AnalysisHistoryRepository historyRepository;
    private final AnalysisHistoryMapper historyMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JeonseFraudStatusResponse submit(
            User user,
            AnalysisRequest request,
            List<MultipartFile> ledgerFiles,
            List<MultipartFile> registryFiles
    ) {
        log.info("위험도 분석 요청 - userId={}", user.getId());
        PredictSubmission submission = apiClient.submitPredict(
                request.deposit(), request.address(), ledgerFiles, registryFiles
        );

        // 캐시히트: 이미 완료된 결과 → 안전등급 합쳐 즉시 반환 (taskId 없음 → 이력 저장 안 함)
        if (submission.cacheHit()) {
            log.info("위험도 분석 캐시 히트 - userId={}", user.getId());
            return buildCompleted(null, submission.cachedResult(), user);
        }

        // 접수: 폴링용 taskId 반환
        log.info("위험도 분석 접수 - taskId={}, userId={}", submission.taskId(), user.getId());
        return JeonseFraudStatusResponse.builder()
                .taskId(submission.taskId())
                .status(TaskStatus.PENDING)
                .progress(0)
                .build();
    }

    public JeonseFraudStatusResponse getStatus(User user, String taskId) {
        PredictPollData poll = apiClient.pollPredict(taskId);

        return switch (poll.status()) {
            case COMPLETED -> buildCompleted(taskId, poll.result(), user);
            case FAILED -> JeonseFraudStatusResponse.builder()
                    .taskId(taskId)
                    .status(TaskStatus.FAILED)
                    .progress(poll.progress())
                    .error(poll.error())
                    .build();
            default -> JeonseFraudStatusResponse.builder()
                    .taskId(taskId)
                    .status(poll.status())
                    .progress(poll.progress())
                    .build();
        };
    }

    /* ─────────────────────────── 분석 이력 조회 ─────────────────────────── */

    public Slice<AnalysisHistorySummaryResponse> getHistory(User user, Pageable pageable) {
        return historyRepository.findAllByUserOrderByIdDesc(user, pageable)
                .map(history -> historyMapper.toHistoryResponse(history, deserialize(history.getResultJson())));
    }

    public AnalysisHistoryDetailResponse getHistoryDetail(User user, Long historyId) {
        AnalysisHistory history = historyRepository.findByIdAndUser(historyId, user)
                                                   .orElseThrow(() -> new BusinessException(ErrorCode.RISK_ANALYSIS_HISTORY_NOT_FOUND));

        AnalysisSnapshot snapshot = deserialize(history.getResultJson());
        return historyMapper.toDetailResponse(history, snapshot);
    }

    /* 위험도 결과 + 안전등급 합본 구성. taskId가 있으면 스냅샷 이력 저장. */
    private JeonseFraudStatusResponse buildCompleted(String taskId, JeonseFraudResult result, User user) {
        SafetyScoreSnapshot safetyScore = resolveSafetyScore(result.address());
        saveHistoryIfAbsent(user, taskId, result, safetyScore);

        return JeonseFraudStatusResponse.builder()
                .taskId(taskId)
                .status(TaskStatus.COMPLETED)
                .progress(100)
                .jeonseFraud(result)
                .safetyScore(safetyScore)
                .build();
    }

    /* 안전등급 계산 실패는 위험도 결과 반환을 막지 않는다 (부분 실패 허용). */
    private SafetyScoreSnapshot resolveSafetyScore(String address) {
        try {
            SafetyScoreResponse.Data data = safetyScoreService.calculateSafetyScore(address).getData();
            return toSnapshot(data);
        } catch (Exception e) {
            log.warn("안전등급 계산 실패 (위험도 결과만 반환) - 주소: {}, 원인: {}", address, e.getMessage());
            return null;
        }
    }

    /* safetyscore 도메인의 응답 Data → jeonsefraud 소유 스냅샷 DTO로 복사 (도메인 간 경계 변환) */
    private SafetyScoreSnapshot toSnapshot(SafetyScoreResponse.Data data) {
        if (data == null) {
            return null;
        }
        return new SafetyScoreSnapshot(
                data.getFinalSafetyScore(),
                data.getRegionName(),
                data.getRegionBaseScore(),
                data.getNearbyCctvCount(),
                data.getNearbyPoliceCount(),
                data.getNearbyFireCount(),
                data.isAccidentHotspot(),
                data.getAccidentHotspotCount(),
                data.getCctvDensityRatio()
        );
    }

    private void saveHistoryIfAbsent(User user, String taskId, JeonseFraudResult result, SafetyScoreSnapshot safetyScore) {
        if (taskId == null || historyRepository.existsByTaskId(taskId)) {
            return;
        }
        String resultJson = serialize(new AnalysisSnapshot(result, safetyScore));
        try {
            historyRepository.save(AnalysisHistory.builder()
                                                  .user(user)
                                                  .taskId(taskId)
                                                  .resultJson(resultJson)
                                                  .build());
            log.info("위험도 분석 완료 및 이력 저장 - taskId={}, userId={}, riskLevel={}",
                    taskId, user.getId(), result.riskLevel());
        } catch (DataIntegrityViolationException e) {
            // 동시 폴링으로 이미 저장됨 — 무시
            log.debug("이미 저장된 분석 이력: taskId={}", taskId);
        }
    }

    private String serialize(AnalysisSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.RISK_ANALYSIS_SNAPSHOT_INVALID, e);
        }
    }

    private AnalysisSnapshot deserialize(String json) {
        try {
            return objectMapper.readValue(json, AnalysisSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.RISK_ANALYSIS_SNAPSHOT_INVALID, e);
        }
    }
}
