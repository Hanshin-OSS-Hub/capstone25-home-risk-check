package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.dto.PostImageResponse;
import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.entity.PostImage;
import hanshin.home_risk_check.community.repository.PostImageRepository;
import hanshin.home_risk_check.community.repository.PostRepository;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/*
 * 게시글 이미지 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostImageService {

    private static final int MAX_IMAGE_COUNT = 10;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public List<PostImageResponse> getPostImages(Long postId) {
        validatePostExists(postId);

        return postImageRepository.findAllByPost_PostIdOrderByImageOrderAsc(postId)
                .stream()
                .map(PostImageResponse::from)
                .toList();
    }

    @Transactional
    public List<PostImageResponse> uploadPostImages(Long postId, List<MultipartFile> images) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (images == null || images.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_IMAGE_REQUEST);
        }

        long existingCount = postImageRepository.countByPost_PostId(postId);
        long newCount = images.stream().filter(file -> file != null && !file.isEmpty()).count();

        if (existingCount + newCount > MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.TOO_MANY_IMAGES);
        }

        Path postDir = Paths.get(uploadDir, "community", "posts", String.valueOf(postId))
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(postDir);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        List<PostImage> savedImages = new ArrayList<>();
        int imageOrder = (int) existingCount;

        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }

            validateImageFile(image);

            String originalName = image.getOriginalFilename() != null ? image.getOriginalFilename() : "unknown";
            String extension = extractExtension(originalName);
            String storedName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
            Path targetPath = postDir.resolve(storedName);

            try {
                image.transferTo(targetPath);
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
            }

            String filePath = "/uploads/community/posts/" + postId + "/" + storedName;

            PostImage postImage = PostImage.builder()
                    .post(post)
                    .originalName(originalName)
                    .storedName(storedName)
                    .extension(extension)
                    .fileSize(image.getSize())
                    .filePath(filePath)
                    .imageOrder(imageOrder++)
                    .build();

            PostImage saved = postImageRepository.save(postImage);
            post.addImage(saved);
            savedImages.add(saved);
        }

        return savedImages.stream()
                .map(PostImageResponse::from)
                .toList();
    }

    @Transactional
    public void deletePostImage(Long postId, Long postImageId) {
        validatePostExists(postId);

        PostImage postImage = postImageRepository.findById(postImageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_IMAGE_NOT_FOUND));

        if (!postImage.getPost().getPostId().equals(postId)) {
            throw new BusinessException(ErrorCode.INVALID_POST_IMAGE);
        }

        deletePhysicalFile(postImage.getFilePath());
        postImageRepository.delete(postImage);
    }

    @Transactional
    public void deleteAllFilesByPostId(Long postId) {
        List<PostImage> images = postImageRepository.findAllByPost_PostIdOrderByImageOrderAsc(postId);

        for (PostImage image : images) {
            deletePhysicalFile(image.getFilePath());
        }
    }

    private void validatePostExists(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
    }

    private void validateImageFile(MultipartFile image) {
        String contentType = image.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
        }
    }

    private String extractExtension(String originalName) {
        int lastDotIndex = originalName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(lastDotIndex + 1).toLowerCase();
    }

    private void deletePhysicalFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            String relativePath = filePath.replaceFirst("^/uploads/", "");
            Path fullPath = Paths.get(uploadDir).resolve(relativePath).toAbsolutePath().normalize();
            Files.deleteIfExists(fullPath);
        } catch (IOException ignored) {
        }
    }
}