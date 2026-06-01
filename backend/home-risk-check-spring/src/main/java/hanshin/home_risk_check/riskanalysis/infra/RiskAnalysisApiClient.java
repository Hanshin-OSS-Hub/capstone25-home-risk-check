package hanshin.home_risk_check.riskanalysis.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import hanshin.home_risk_check.riskanalysis.dto.RiskAnalysisResult;
import hanshin.home_risk_check.riskanalysis.infra.dto.FastApiEnvelope;
import hanshin.home_risk_check.riskanalysis.infra.dto.PredictAcceptedData;
import hanshin.home_risk_check.riskanalysis.infra.dto.PredictPollData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

/*
 * FastAPI 위험도 분석 서버 호출 클라이언트.
 *
 * - POST /predict        : 파일 + 본문을 multipart로 전달, 접수(202) 또는 캐시히트(200)
 * - GET  /predict/{id}   : 작업 상태/결과 폴링
 */
@Slf4j
@Component
public class RiskAnalysisApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public RiskAnalysisApiClient(
            RestTemplate restTemplate,
            @Value("${fastapi.base-url}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /*
     * 분석 요청 제출.
     * 캐시히트면 결과가 즉시 반환, 아니면 taskId 발급(202).
     */
    public PredictSubmission submitPredict(
            long deposit,
            String address,
            List<MultipartFile> ledgerFiles,
            List<MultipartFile> registryFiles
    ) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("deposit", String.valueOf(deposit));
        body.add("address", address);
        addFiles(body, "ledger_files", ledgerFiles);
        addFiles(body, "registry_files", registryFiles);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(baseUrl + "/predict", entity, String.class);

            // 202: 접수 → taskId / 200: 캐시히트 → 완성 결과
            if (response.getStatusCode() == HttpStatus.ACCEPTED) {
                PredictAcceptedData data = readEnvelope(response.getBody(), new TypeReference<>() {});
                return PredictSubmission.accepted(data.taskId());
            }
            RiskAnalysisResult cached = read(response.getBody(), RiskAnalysisResult.class);
            return PredictSubmission.cacheHit(cached);

        } catch (RestClientException e) {
            log.error("[FastAPI] /predict 요청 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED, e);
        }
    }

    /*
     * 작업 상태/결과 폴링.
     */
    public PredictPollData pollPredict(String taskId) {
        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity(baseUrl + "/predict/" + taskId, String.class);
            return readEnvelope(response.getBody(), new TypeReference<>() {});
        } catch (RestClientException e) {
            log.error("[FastAPI] /predict/{} 폴링 실패: {}", taskId, e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED, e);
        }
    }

    private void addFiles(MultiValueMap<String, Object> body, String partName, List<MultipartFile> files) {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        for (MultipartFile file : files) {
            body.add(partName, toResource(file));
        }
    }

    private Resource toResource(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String filename = file.getOriginalFilename();
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    private <T> T readEnvelope(String json, TypeReference<FastApiEnvelope<T>> typeRef) {
        try {
            FastApiEnvelope<T> envelope = objectMapper.readValue(json, typeRef);
            return envelope.data();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED, e);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED, e);
        }
    }

    /*
     * 제출 결과: 캐시히트(완성 결과) 또는 접수(taskId) 중 하나.
     */
    public record PredictSubmission(boolean cacheHit, String taskId, RiskAnalysisResult cachedResult) {
        public static PredictSubmission accepted(String taskId) {
            return new PredictSubmission(false, taskId, null);
        }

        public static PredictSubmission cacheHit(RiskAnalysisResult result) {
            return new PredictSubmission(true, null, result);
        }
    }
}