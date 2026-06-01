package hanshin.home_risk_check.file.util;

import hanshin.home_risk_check.file.entity.ImageFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/*
 * 저장 식별자(storageKey)를 클라이언트가 접근 가능한 URL로 변환한다.
 * 로컬/CDN/S3 등 "저장 위치 → URL" 정책을 한곳에 격리하기 위한 컴포넌트.
 *
 * 설정: file.base-url (없으면 "/uploads" 기본값)
 */
@Component
public class FileUrlResolver {

    private final String baseUrl;

    public FileUrlResolver(
            @Value("${file.base-url:}") String baseUrl,
            @Value("${aws.s3.bucket:}") String bucket,
            @Value("${aws.s3.region:ap-northeast-2}") String region
    ) {
        String resolvedBaseUrl = resolveBaseUrl(baseUrl, bucket, region);
        this.baseUrl = resolvedBaseUrl.endsWith("/")
                ? resolvedBaseUrl.substring(0, resolvedBaseUrl.length() - 1)
                : resolvedBaseUrl;
    }

    /*
     * storageKey -> 접근 URL
     *  - null/blank  -> null
     *  - 이미 절대 URL(http/https) -> 그대로
     *  - 그 외        -> baseUrl 접두
     */
    public String toUrl(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        if (storageKey.startsWith("http://") || storageKey.startsWith("https://")) {
            return storageKey;
        }
        return baseUrl + (storageKey.startsWith("/") ? "" : "/") + storageKey;
    }

    public String toUrl(ImageFile imageFile) {
        if (imageFile == null) {
            return null;
        }
        return toUrl(imageFile.getStorageKey());
    }

    private String resolveBaseUrl(String baseUrl, String bucket, String region) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }
        if (bucket != null && !bucket.isBlank()) {
            return "https://%s.s3.%s.amazonaws.com".formatted(bucket, region);
        }
        return "/uploads";
    }
}