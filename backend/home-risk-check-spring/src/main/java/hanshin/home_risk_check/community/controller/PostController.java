package hanshin.home_risk_check.community.controller;

import hanshin.home_risk_check.community.dto.PostCreateRequest;
import hanshin.home_risk_check.community.dto.PostDetailResponse;
import hanshin.home_risk_check.community.dto.PostLikeResponse;
import hanshin.home_risk_check.community.dto.PostPollVoteRequest;
import hanshin.home_risk_check.community.dto.PostSearchRequest;
import hanshin.home_risk_check.community.dto.PostSummaryResponse;
import hanshin.home_risk_check.community.dto.PostUpdateRequest;
import hanshin.home_risk_check.community.service.CommunityFacade;
import hanshin.home_risk_check.global.dto.ApiResponse;
import hanshin.home_risk_check.user.entity.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final CommunityFacade communityFacade;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostDetailResponse>> create(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestPart("data") PostCreateRequest postCreateRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        PostDetailResponse response = communityFacade.createPost(currentUser.getUser(), postCreateRequest, images);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "게시글 작성 성공", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Slice<PostSummaryResponse>>> getPosts(
            @Valid @ModelAttribute PostSearchRequest request
    ) {
        Slice<PostSummaryResponse> response = communityFacade.getPosts(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "게시글 목록 조회 성공", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Slice<PostSummaryResponse>>> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Slice<PostSummaryResponse> response =
                communityFacade.getMyPosts(currentUser.getUser(), PageRequest.of(page, size));
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "내 게시글 목록 조회 성공", response));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> get(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long postId
    ) {
        PostDetailResponse response = communityFacade.getPost(currentUser.getUser(), postId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "게시글 조회 성공", response));
    }

    @PostMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<PostLikeResponse>> like(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long postId
    ) {
        PostLikeResponse response = communityFacade.like(currentUser.getUser(), postId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "게시글 좋아요 성공", response));
    }

    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<PostLikeResponse>> cancelLike(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long postId
    ) {
        PostLikeResponse response = communityFacade.cancelLike(currentUser.getUser(), postId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "게시글 좋아요 취소 성공", response));
    }

    @PostMapping("/{postId}/likes/toggle")
    public ResponseEntity<ApiResponse<PostLikeResponse>> toggleLike(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long postId
    ) {
        PostLikeResponse response = communityFacade.toggleLike(currentUser.getUser(), postId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "게시글 좋아요 토글 성공", response));
    }

    @PostMapping("/{postId}/poll/votes")
    public ResponseEntity<ApiResponse<PostDetailResponse>> vote(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody PostPollVoteRequest request
    ) {
        PostDetailResponse response = communityFacade.vote(currentUser.getUser(), postId, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "게시글 투표 성공", response));
    }

    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostDetailResponse>> update(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long postId,
            @Valid @RequestPart("data") PostUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        PostDetailResponse response = communityFacade.updatePost(currentUser.getUser(), postId, request, images);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "게시글 수정 성공", response));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long postId
    ) {
        communityFacade.deletePost(currentUser.getUser(), postId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "게시글 삭제 성공", null));
    }
}