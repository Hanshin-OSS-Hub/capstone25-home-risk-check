package hanshin.home_risk_check.community.controller;

import hanshin.home_risk_check.community.dto.PostCreateRequest;
import hanshin.home_risk_check.community.dto.PostImageResponse;
import hanshin.home_risk_check.community.dto.PostResponse;
import hanshin.home_risk_check.community.dto.PostUpdateRequest;
import hanshin.home_risk_check.community.service.PostImageService;
import hanshin.home_risk_check.community.service.PostService;
import hanshin.home_risk_check.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/*
 * 게시글 Controller
 *
 * 기존 게시글 CRUD API는 유지하고,
 * 이미지 업로드/조회/삭제 API만 추가한다.
 *
 * 이렇게 하면 기존 프론트(JSON 기반 게시글 작성/수정)와 충돌을 최소화할 수 있다.
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
    public ApiResponse<PostResponse> createPost(@RequestBody PostCreateRequest request) {
        Long authorId = 1L; // TODO: 추후 JWT에서 사용자 ID 추출

        return ApiResponse.success(
                201,
                "게시글 작성 성공",
                postService.createPost(authorId, request)
        );
    }

    @PatchMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable Long postId,
            @RequestBody PostUpdateRequest request
    ) {
        Long authorId = 1L; // TODO: 추후 JWT에서 사용자 ID 추출

        return ApiResponse.success(
                postService.updatePost(postId, authorId, request)
        );
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId) {
        Long authorId = 1L; // TODO: 추후 JWT에서 사용자 ID 추출

        postService.deletePost(postId, authorId);

        return ApiResponse.success(200, "게시글 삭제 성공", null);
    }

    /*
     * 게시글 이미지 업로드 API
     *
     * POST /api/posts/{postId}/images
     * Content-Type: multipart/form-data
     * key: images (여러 개 가능)
     *
     * 기존 게시글 생성 API는 그대로 두고,
     * 이미지만 따로 업로드하도록 분리했다.
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
     * 게시글 이미지 목록 조회 API
     */
    @GetMapping("/{postId}/images")
    public ApiResponse<List<PostImageResponse>> getPostImages(@PathVariable Long postId) {
        return ApiResponse.success(postImageService.getPostImages(postId));
    }

    /*
     * 게시글 이미지 단건 삭제 API
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