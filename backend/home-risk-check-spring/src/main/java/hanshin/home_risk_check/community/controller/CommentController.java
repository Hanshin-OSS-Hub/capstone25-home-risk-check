package hanshin.home_risk_check.community.controller;

import hanshin.home_risk_check.community.dto.CommentCreateRequest;
import hanshin.home_risk_check.community.dto.CommentResponse;
import hanshin.home_risk_check.community.service.CommentService;
import hanshin.home_risk_check.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // [변경] 현재 로그인 사용자 이메일 주입을 위해 추가
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * 댓글 Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    /*
     * 댓글 목록 조회
     * GET /api/posts/{postId}/comments
     */
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommentResponse>> getComments(@PathVariable Long postId) {
        return ApiResponse.success(commentService.getComments(postId));
    }

    /*
     * 댓글 작성
     * POST /api/posts/{postId}/comments
     */
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal String email, // [변경] JWT Filter에서 principal로 넣은 email 사용
            @Valid @RequestBody CommentCreateRequest request
    ) {
        /*
         * [변경]
         * 기존 Long authorId = 1L 제거
         * Service에서 email로 User를 조회해 작성자로 사용
         */
        return ApiResponse.success(
                201,
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
            @AuthenticationPrincipal String email // [변경]
    ) {
        /*
         * [변경]
         * 기존 Long authorId = 1L 제거
         */
        commentService.deleteComment(commentId, email);

        return ApiResponse.success(200, "댓글 삭제 성공", null);
    }
}