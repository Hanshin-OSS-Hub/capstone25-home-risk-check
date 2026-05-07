package hanshin.home_risk_check.community.controller;

import hanshin.home_risk_check.community.dto.CommentCreateRequest;
import hanshin.home_risk_check.community.dto.CommentResponse;
import hanshin.home_risk_check.community.service.CommentService;
import hanshin.home_risk_check.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/*
 * 댓글 Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    /*
     * 댓글 목록 조회 (pagination)
     * GET /api/posts/{postId}/comments?page=0&size=20
     */
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<Page<CommentResponse>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(commentService.getComments(postId, page, size));
    }

    /*
     * 댓글 작성
     * POST /api/posts/{postId}/comments
     */
    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal String email,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ApiResponse.success(
                HttpStatus.CREATED.value(),
                "댓글 작성 성공",
                commentService.createComment(postId, email, request)
        );
    }

    /*
     * 댓글 삭제
     * DELETE /api/comments/{commentId}
     */
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal String email
    ) {
        commentService.deleteComment(commentId, email);
        return ApiResponse.success(HttpStatus.OK.value(), "댓글 삭제 성공", null);
    }
}
