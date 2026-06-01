package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.file.entity.ImageFile;
import hanshin.home_risk_check.file.service.ImageFileService;
import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.entity.PostImage;
import hanshin.home_risk_check.community.repository.PostImageRepository;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostImageService {

    private static final int MAX_IMAGES_PER_POST = 10;
    private final PostImageRepository postImageRepository;
    private final ImageFileService imageFileService;

    @Transactional
    public List<PostImage> createPostImages(Post post, List<MultipartFile> images) {
        if (CollectionUtils.isEmpty(images)) {
            return List.of();
        }
        validateImageLimit(images.size());
        return savePostImages(post, images);
    }

    public List<PostImage> getPostImages(Post post) {
        if(post == null){
            return List.of();
        }
        return postImageRepository.findAllByPostOrderById(post);
    }

    public Map<Long, PostImage> getThumbnails(List<Post> posts) {
        if (CollectionUtils.isEmpty(posts)) {
            return Map.of();
        }
        Map<Long, PostImage> thumbnails = postImageRepository.findThumbnailsByPost(posts)
                                                             .stream()
                                                             .collect(Collectors.toMap(postImage ->
                                                                     postImage.getPost().getId(),
                                                                     postImage -> postImage)
                                                             );

        posts.forEach(post -> thumbnails.putIfAbsent(post.getId(), null));
        return thumbnails;
    }

    @Transactional
    public List<PostImage> updatePostImages(Post post, List<Long> deletePostImageIds, List<MultipartFile> newImages) {
        // 변화 없으면 작업 없이 현재 상태 반환
        if (CollectionUtils.isEmpty(deletePostImageIds) && CollectionUtils.isEmpty(newImages)) {
            return getPostImages(post);
        }

        List<Long> idsToDelete = CollectionUtils.isEmpty(deletePostImageIds) ? List.of() : deletePostImageIds;
        List<MultipartFile> imagesToAdd = CollectionUtils.isEmpty(newImages) ? List.of() : newImages;

        //현재 존재하는 postImage
        List<PostImage> postImages = postImageRepository.findAllByPostOrderById(post);

        //삭제할 postImage
        List<PostImage> postImagesToDelete = postImages.stream()
                                                       .filter(postImage -> idsToDelete.contains(postImage.getId()))
                                                       .toList();
        if (postImagesToDelete.size() != new HashSet<>(idsToDelete).size()) {
            throw new BusinessException(ErrorCode.INVALID_POST_IMAGE);
        }

        //유지할 postImage
        List<PostImage> postImagesToKeep = postImages.stream()
                                                     .filter(postImage -> !idsToDelete.contains(postImage.getId()))
                                                     .toList();

        validateImageLimit(postImagesToKeep.size() + imagesToAdd.size());
        deletePostImages(postImagesToDelete);
        savePostImages(post, imagesToAdd);

        return getPostImages(post);
    }

    @Transactional
    public void deletePostImages(List<PostImage> postImages) {
        if (CollectionUtils.isEmpty(postImages)) {
            return;
        }
        List<ImageFile> imageFiles = postImages.stream()
                                           .map(PostImage::getImageFile)
                                           .toList();

        postImageRepository.deleteAll(postImages);
        postImageRepository.flush();
        imageFileService.deleteAll(imageFiles);
    }

    private void validateImageLimit(int totalCount) {
        if (totalCount > MAX_IMAGES_PER_POST) {
            throw new BusinessException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        }
    }

    private List<PostImage> savePostImages(Post post, List<MultipartFile> images) {
        List<ImageFile> imageFiles = imageFileService.createAll(images);
        List<PostImage> postImages = imageFiles.stream()
                                                 .map(imageFile ->
                                                         PostImage.builder()
                                                                  .post(post)
                                                                  .imageFile(imageFile)
                                                                  .build()
                                                 )
                                                 .toList();

        return postImageRepository.saveAll(postImages);
    }
}