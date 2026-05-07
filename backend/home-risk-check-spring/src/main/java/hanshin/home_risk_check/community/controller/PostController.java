package hanshin.home_risk_check.community.controller;

import hanshin.home_risk_check.community.dto.PostCreateRequest;
import hanshin.home_risk_check.community.dto.PostImageResponse;
import hanshin.home_risk_check.community.dto.PostResponse;
import hanshin.home_risk_check.community.dto.PostUpdateRequest;
import hanshin.home_risk_check.community.service.PostImageService;
import hanshin.home_risk_check.community.service.PostService;
import hanshin.home_risk_check.global.dto.ApiResponse;
import jakarta.validation.Valid; // [변경] DTO Bean Validation 적용을 위해 추가
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // [변경] 현재 로그인 사용자 이메일 주입을 위해 추가
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/*
 * 게시글 Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final PostImageService postImageService;

    @GetMapping
    public ApiResponse<Page<PostResponse>> getPosts(
            @RequestParam(required = false) String categoryLabel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(postService.getPosts(categoryLabel, page, size));
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPost(@PathVariable Long postId) {
        return ApiResponse.success(postService.getPost(postId));
    }

    @PostMapping
    public ApiResponse<PostResponse> createPost(
            @AuthenticationPrincipal String email, // [변경] JWT Filter에서 principal로 넣은 email 사용
            @Valid @RequestBody PostCreateRequest request // [변경] DTO Validation 적용
    ) {
        /*
         * [변경]
         * 기존 Long authorId = 1L 제거
         * Service에서 email로 User를 조회해 작성자로 사용
         */
        return ApiResponse.success(
                201,
                "게시글 작성 성공",
                postService.createPost(email, request)
        );
    }

    @PatchMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal String email, // [변경]
            @Valid @RequestBody PostUpdateRequest request // [변경] DTO Validation 적용
    ) {
        /*
         * [변경]
         * 기존 Long authorId = 1L 제거
         */
        return ApiResponse.success(
                postService.updatePost(postId, email, request)
        );
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal String email // [변경]
    ) {
        /*
         * [변경]
         * 기존 Long authorId = 1L 제거
         */
        postService.deletePost(postId, email);

        return ApiResponse.success(200, "게시글 삭제 성공", null);
    }

    /*
     * 게시글 이미지 업로드
     * 게시글 1개당 최대 10장
     */
    @PostMapping(value = "/{postId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<PostImageResponse>> uploadPostImages(
            @PathVariable Long postId,
            @RequestPart("images") List<MultipartFile> images
    ) {
        return ApiResponse.success(
                201,
                "게시글 이미지 업로드 성공",
                postImageService.uploadPostImages(postId, images)
        );
    }

    /*
     * 게시글 이미지 목록 조회
     */
    @GetMapping("/{postId}/images")
    public ApiResponse<List<PostImageResponse>> getPostImages(@PathVariable Long postId) {
        return ApiResponse.success(postImageService.getPostImages(postId));
    }

    /*
     * 게시글 이미지 단건 삭제
     */
    @DeleteMapping("/{postId}/images/{postImageId}")
    public ApiResponse<Void> deletePostImage(
            @PathVariable Long postId,
            @PathVariable Long postImageId
    ) {
        postImageService.deletePostImage(postId, postImageId);

        return ApiResponse.success(200, "게시글 이미지 삭제 성공", null);
    }
}