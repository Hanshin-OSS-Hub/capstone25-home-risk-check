package hanshin.home_risk_check.file.storage;

import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final String bucket;
    private final String uploadPrefix;

    public S3FileStorage(
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.upload-prefix}") String uploadPrefix
    ) {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.bucket = bucket;
        this.uploadPrefix = trimSlashes(uploadPrefix);
    }

    @Override
    public String upload(MultipartFile file) {
        validateBucket();
        String storageKey = createStorageKey(file);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return storageKey;
        } catch (IOException | S3Exception e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    private String createStorageKey(MultipartFile file) {
        LocalDate now = LocalDate.now();
        String extension = extractExtension(file.getOriginalFilename());
        return "%s/%d/%02d/%02d/%s%s".formatted(
                uploadPrefix,
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                UUID.randomUUID(),
                extension
        );
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        if (bucket == null || bucket.isBlank()) {
            log.warn("S3 bucket is empty. skip object delete. storageKey={}", storageKey);
            return;
        }

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build();
            s3Client.deleteObject(request);
        } catch (S3Exception e) {
            log.warn("S3 object delete failed. storageKey={}", storageKey, e);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(lastDotIndex).toLowerCase();
    }

    private String trimSlashes(String value) {
        if (value == null || value.isBlank()) {
            return "images";
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private void validateBucket() {
        if (bucket == null || bucket.isBlank()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "S3 버킷 설정이 없습니다.");
        }
    }
}