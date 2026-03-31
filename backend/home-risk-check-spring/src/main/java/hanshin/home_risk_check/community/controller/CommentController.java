package hanshin.home_risk_check.community.controller;

import hanshin.home_risk_check.community.dto.CommentCreateRequest;
import hanshin.home_risk_check.community.dto.CommentResponse;
import hanshin.home_risk_check.community.service.CommentService;
import hanshin.home_risk_check.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * 댓글 Controller
 *
 * 클라이언트의 HTTP 요청을 받아
 * 댓글 관련 Service로 전달하는 역할
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
            @Valid @RequestBody CommentCreateRequest request
    ) {
        /*
         * 현재는 인증 미적용 상태라 임시 작성자 ID 사용
         * 추후 JWT 붙이면 실제 로그인 사용자 ID로 교체
         */
        Long authorId = 1L;

        return ApiResponse.success(
                201,
                "댓글 작성 성공",
                commentService.createComment(postId, authorId, request)
        );
    }

    /*
     * 댓글 삭제
     * DELETE /api/comments/{commentId}
     */
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        Long authorId = 1L;

        commentService.deleteComment(commentId, authorId);

        return ApiResponse.success(200, "댓글 삭제 성공", null);
    }
}