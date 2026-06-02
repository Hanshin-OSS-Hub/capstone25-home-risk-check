package hanshin.home_risk_check.analysis.controller;

import hanshin.home_risk_check.global.dto.ApiResponse;
import hanshin.home_risk_check.analysis.dto.AnalysisHistoryDetailResponse;
import hanshin.home_risk_check.analysis.dto.AnalysisHistorySummaryResponse;
import hanshin.home_risk_check.analysis.dto.AnalysisRequest;
import hanshin.home_risk_check.analysis.dto.JeonseFraudStatusResponse;
import hanshin.home_risk_check.analysis.service.AnalysisService;
import hanshin.home_risk_check.user.entity.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    /*
     * 위험도 분석 요청.
     * 응답 status: PENDING(접수, taskId로 폴링) 또는 COMPLETED(캐시히트, 즉시 결과)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<JeonseFraudStatusResponse>> submit(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestPart("data") AnalysisRequest request,
            @RequestPart(value = "ledgerFiles", required = false) List<MultipartFile> ledgerFiles,
            @RequestPart(value = "registryFiles", required = false) List<MultipartFile> registryFiles
    ) {
        JeonseFraudStatusResponse response =
                analysisService.submit(currentUser.getUser(), request, ledgerFiles, registryFiles);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "위험도 분석 요청 성공", response));
    }

    /*
     * 내 분석 이력 목록 (마이페이지). 최신순.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Slice<AnalysisHistorySummaryResponse>>> getHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Slice<AnalysisHistorySummaryResponse> response =
                analysisService.getHistory(currentUser.getUser(), PageRequest.of(page, size));
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "분석 이력 목록 조회 성공", response));
    }

    @GetMapping("/history/{historyId}")
    public ResponseEntity<ApiResponse<AnalysisHistoryDetailResponse>> getHistoryDetail(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long historyId
    ) {
        AnalysisHistoryDetailResponse response =
                analysisService.getHistoryDetail(currentUser.getUser(), historyId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "분석 이력 상세 조회 성공", response));
    }

    /*
     * 분석 작업 상태/결과 조회 (폴링).
     * COMPLETED 시 위험도 결과 + 지역 안전등급 합본 반환.
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<JeonseFraudStatusResponse>> getStatus(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable String taskId
    ) {
        JeonseFraudStatusResponse response =
                analysisService.getStatus(currentUser.getUser(), taskId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "위험도 분석 상태 조회 성공", response));
    }
}