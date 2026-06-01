package hanshin.home_risk_check.file.service;

import hanshin.home_risk_check.file.entity.ImageFile;
import hanshin.home_risk_check.file.repository.ImageFileRepository;
import hanshin.home_risk_check.file.storage.FileStorage;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageFileService {

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final ImageFileRepository imageFileRepository;
    private final FileStorage fileStorage;

    @Transactional
    public ImageFile create(MultipartFile file) {
        validateImageFile(file);
        String storageKey = fileStorage.upload(file);
        registerDeleteStoragesAfterRollback(List.of(storageKey));

        ImageFile imageFile = ImageFile.builder()
                                       .originalName(file.getOriginalFilename())
                                       .storageKey(storageKey)
                                       .contentType(file.getContentType())
                                       .build();

        return imageFileRepository.save(imageFile);
    }

    @Transactional
    public List<ImageFile> createAll(List<MultipartFile> files) {
        if (CollectionUtils.isEmpty(files)) {
            throw new BusinessException(ErrorCode.EMPTY_IMAGE_REQUEST);
        }
        files.forEach(this::validateImageFile);

        List<String> storageKeys = uploadAll(files);
        registerDeleteStoragesAfterRollback(storageKeys);

        List<ImageFile> imageFiles = IntStream.range(0, files.size())
                                              .mapToObj(index -> {
                                                  MultipartFile file = files.get(index);
                                                  return ImageFile.builder()
                                                          .originalName(file.getOriginalFilename())
                                                          .storageKey(storageKeys.get(index))
                                                          .contentType(file.getContentType())
                                                          .build();
                                              })
                                              .toList();

        return imageFileRepository.saveAll(imageFiles);
    }

    @Transactional
    public void delete(ImageFile imageFile) {
        if (imageFile == null) {
            return;
        }
        String storageKey = imageFile.getStorageKey();
        imageFileRepository.delete(imageFile);
        deleteStoragesAfterCommit(List.of(storageKey));
    }

    @Transactional
    public void deleteAll(List<ImageFile> imageFiles) {
        if (CollectionUtils.isEmpty(imageFiles)) {
            return;
        }
        List<String> storageKeys = imageFiles.stream()
                                             .map(ImageFile::getStorageKey)
                                             .toList();

        imageFileRepository.deleteAll(imageFiles);
        deleteStoragesAfterCommit(storageKeys);
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_IMAGE_REQUEST);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
        }
    }

    private List<String> uploadAll(List<MultipartFile> files) {
        List<String> storageKeys = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                storageKeys.add(fileStorage.upload(file));
            }
            return storageKeys;
        } catch (RuntimeException e) {
            storageKeys.forEach(fileStorage::delete);
            throw e;
        }
    }

    private void deleteStoragesAfterCommit(List<String> storageKeys) {
        List<String> validStorageKeys = storageKeys.stream()
                                                   .filter(storageKey -> storageKey != null && !storageKey.isBlank())
                                                   .toList();
        if (CollectionUtils.isEmpty(validStorageKeys)) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        validStorageKeys.forEach(fileStorage::delete);
                    }
                }
        );
    }

    private void registerDeleteStoragesAfterRollback(List<String> storageKeys) {
        List<String> validStorageKeys = storageKeys.stream()
                                                   .filter(storageKey -> storageKey != null && !storageKey.isBlank())
                                                   .toList();
        if (CollectionUtils.isEmpty(validStorageKeys)) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            validStorageKeys.forEach(fileStorage::delete);
                        }
                    }
                }
        );
    }
}