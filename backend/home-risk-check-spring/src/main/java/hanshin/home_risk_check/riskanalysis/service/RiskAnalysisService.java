package hanshin.home_risk_check.riskanalysis.service;

import hanshin.home_risk_check.riskanalysis.dto.RiskAnalysisRequest;
import hanshin.home_risk_check.riskanalysis.dto.RiskAnalysisResult;
import hanshin.home_risk_check.riskanalysis.dto.RiskAnalysisStatusResponse;
import hanshin.home_risk_check.riskanalysis.dto.TaskStatus;
import hanshin.home_risk_check.riskanalysis.entity.RiskAnalysisHistory;
import hanshin.home_risk_check.riskanalysis.infra.RiskAnalysisApiClient;
import hanshin.home_risk_check.riskanalysis.infra.RiskAnalysisApiClient.PredictSubmission;
import hanshin.home_risk_check.riskanalysis.infra.dto.PredictPollData;
import hanshin.home_risk_check.riskanalysis.repository.RiskAnalysisHistoryRepository;
import hanshin.home_risk_check.safetyscore.domain.score.dto.SafetyScoreResponse;
import hanshin.home_risk_check.safetyscore.domain.score.service.SafetyScoreService;
import hanshin.home_risk_check.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
public class RiskAnalysisService {

    private final RiskAnalysisApiClient apiClient;
    private final SafetyScoreService safetyScoreService;
    private final RiskAnalysisHistoryRepository historyRepository;

    public RiskAnalysisStatusResponse submit(
            User user,
            RiskAnalysisRequest request,
            List<MultipartFile> ledgerFiles,
            List<MultipartFile> registryFiles
    ) {
        PredictSubmission submission = apiClient.submitPredict(
                request.deposit(), request.address(), ledgerFiles, registryFiles
        );

        // 캐시히트: 이미 완료된 결과 → 안전등급 합쳐 즉시 반환 (taskId 없음 → 이력 저장 안 함)
        if (submission.cacheHit()) {
            return buildCompleted(null, submission.cachedResult(), user);
        }

        // 접수: 폴링용 taskId 반환
        return RiskAnalysisStatusResponse.builder()
                .taskId(submission.taskId())
                .status(TaskStatus.PENDING)
                .progress(0)
                .build();
    }

    public RiskAnalysisStatusResponse getStatus(User user, String taskId) {
        PredictPollData poll = apiClient.pollPredict(taskId);

        return switch (poll.status()) {
            case COMPLETED -> buildCompleted(taskId, poll.result(), user);
            case FAILED -> RiskAnalysisStatusResponse.builder()
                    .taskId(taskId)
                    .status(TaskStatus.FAILED)
                    .progress(poll.progress())
                    .error(poll.error())
                    .build();
            default -> RiskAnalysisStatusResponse.builder()
                    .taskId(taskId)
                    .status(poll.status())
                    .progress(poll.progress())
                    .build();
        };
    }

    /* 위험도 결과 + 안전등급 합본 구성. taskId가 있으면 이력 저장. */
    private RiskAnalysisStatusResponse buildCompleted(String taskId, RiskAnalysisResult result, User user) {
        SafetyScoreResponse.Data safetyScore = resolveSafetyScore(result.address());
        saveHistoryIfAbsent(user, taskId, result);

        return RiskAnalysisStatusResponse.builder()
                .taskId(taskId)
                .status(TaskStatus.COMPLETED)
                .progress(100)
                .riskAnalysis(result)
                .safetyScore(safetyScore)
                .build();
    }

    /* 안전등급 계산 실패는 위험도 결과 반환을 막지 않는다 (부분 실패 허용). */
    private SafetyScoreResponse.Data resolveSafetyScore(String address) {
        try {
            return safetyScoreService.calculateSafetyScore(address).getData();
        } catch (Exception e) {
            log.warn("안전등급 계산 실패 (위험도 결과만 반환) - 주소: {}, 원인: {}", address, e.getMessage());
            return null;
        }
    }

    private void saveHistoryIfAbsent(User user, String taskId, RiskAnalysisResult result) {
        if (taskId == null || historyRepository.existsByTaskId(taskId)) {
            return;
        }
        try {
            historyRepository.save(RiskAnalysisHistory.builder()
                    .user(user)
                    .taskId(taskId)
                    .address(result.address())
                    .deposit(result.deposit())
                    .riskScore(result.riskScore())
                    .riskLevel(result.riskLevel())
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시 폴링으로 이미 저장됨 — 무시
            log.debug("이미 저장된 분석 이력: taskId={}", taskId);
        }
    }
}
