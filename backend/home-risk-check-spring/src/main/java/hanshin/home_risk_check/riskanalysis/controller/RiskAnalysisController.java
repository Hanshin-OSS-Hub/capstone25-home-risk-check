package hanshin.home_risk_check.riskanalysis.controller;

import hanshin.home_risk_check.global.dto.ApiResponse;
import hanshin.home_risk_check.riskanalysis.dto.RiskAnalysisRequest;
import hanshin.home_risk_check.riskanalysis.dto.RiskAnalysisStatusResponse;
import hanshin.home_risk_check.riskanalysis.service.RiskAnalysisService;
import hanshin.home_risk_check.user.entity.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/risk-analysis")
public class RiskAnalysisController {

    private final RiskAnalysisService riskAnalysisService;

    /*
     * 위험도 분석 요청.
     * 응답 status: PENDING(접수, taskId로 폴링) 또는 COMPLETED(캐시히트, 즉시 결과)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RiskAnalysisStatusResponse>> submit(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestPart("data") RiskAnalysisRequest request,
            @RequestPart(value = "ledgerFiles", required = false) List<MultipartFile> ledgerFiles,
            @RequestPart(value = "registryFiles", required = false) List<MultipartFile> registryFiles
    ) {
        RiskAnalysisStatusResponse response =
                riskAnalysisService.submit(currentUser.getUser(), request, ledgerFiles, registryFiles);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "위험도 분석 요청 성공", response));
    }

    /*
     * 분석 작업 상태/결과 조회 (폴링).
     * COMPLETED 시 위험도 결과 + 지역 안전등급 합본 반환.
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<RiskAnalysisStatusResponse>> getStatus(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable String taskId
    ) {
        RiskAnalysisStatusResponse response =
                riskAnalysisService.getStatus(currentUser.getUser(), taskId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "위험도 분석 상태 조회 성공", response));
    }
}