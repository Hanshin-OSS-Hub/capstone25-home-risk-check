package hanshin.home_risk_check.community.controller;

import hanshin.home_risk_check.community.dto.CommentCreateRequest;
import hanshin.home_risk_check.community.dto.CommentResponse;
import hanshin.home_risk_check.community.service.CommunityFacade;
import hanshin.home_risk_check.global.dto.ApiResponse;
import hanshin.home_risk_check.user.entity.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommunityFacade communityFacade;

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<Slice<CommentResponse>>> getComments(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Slice<CommentResponse> response = communityFacade.getRootComments(postId, currentUser.getUser(), PageRequest.of(page, size));
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "댓글 목록 조회 성공", response));
    }

    @GetMapping("/comments/{rootCommentId}/childComments")
    public ResponseEntity<ApiResponse<Slice<CommentResponse>>> getChildComments(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long rootCommentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Slice<CommentResponse> response = communityFacade.getChildComments(rootCommentId, currentUser.getUser(), PageRequest.of(page, size));
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "답글 조회 성공", response));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        CommentResponse response = communityFacade.createComment(postId, currentUser.getUser(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "댓글 작성 성공", response));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long commentId
    ) {
        communityFacade.deleteComment(commentId, currentUser.getUser());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "댓글 삭제 성공", null));
    }
}