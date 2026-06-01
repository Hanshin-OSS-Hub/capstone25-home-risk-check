package hanshin.home_risk_check.file.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    String upload(MultipartFile file);

    void delete(String storageKey);
}