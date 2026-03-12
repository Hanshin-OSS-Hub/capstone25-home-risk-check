package hanshin.home_risk_check.community.controller;

import hanshin.home_risk_check.community.dto.CommentCreateRequest;
import hanshin.home_risk_check.community.dto.CommentResponse;
import hanshin.home_risk_check.community.service.CommentService;
import hanshin.home_risk_check.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * 댓글 Controller
 *
 * 클라이언트(프론트, Postman 등)의 HTTP 요청을 받아
 * 댓글 관련 Service로 전달하는 역할
 *
 * 역할:
 * - 특정 게시글의 댓글 목록 조회
 * - 댓글 작성
 * - 댓글 삭제
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    /*
     * 댓글 비즈니스 로직을 처리하는 Service
     */
    private final CommentService commentService;

    /*
     * 댓글 목록 조회 API
     *
     * GET /api/posts/{postId}/comments
     *
     * 특정 게시글의 댓글 목록을 조회한다.
     *
     * 예:
     * /api/posts/3/comments
     */
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommentResponse>> getComments(@PathVariable Long postId) {

        /*
         * CommentService에서 댓글 목록 조회
         */
        return ApiResponse.success(commentService.getComments(postId));
    }

    /*
     * 댓글 작성 API
     *
     * POST /api/posts/{postId}/comments
     *
     * 요청 body 예:
     *
     * {
     *   "content": "이 매물 위험해 보입니다",
     *   "parentCommentId": null
     * }
     *
     * 또는 (대댓글)
     *
     * {
     *   "content": "저도 그렇게 생각합니다",
     *   "parentCommentId": 5
     * }
     */
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long postId,
            @RequestBody CommentCreateRequest request
    ) {

        /*
         * 현재는 인증 기능이 없어서 작성자 ID를 임시값으로 사용
         * 나중에 JWT 붙이면 로그인 사용자 ID로 교체
         */
        Long authorId = 1L; // TODO: 추후 JWT에서 사용자 ID 추출

        return ApiResponse.success(
                201,
                "댓글 작성 성공",
                commentService.createComment(postId, authorId, request)
        );
    }

    /*
     * 댓글 삭제 API
     *
     * DELETE /api/comments/{commentId}
     *
     * 예:
     * /api/comments/5
     */
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {

        /*
         * 현재는 인증 기능이 없어서 작성자 ID를 임시값으로 사용
         */
        Long authorId = 1L; // TODO: 추후 JWT에서 사용자 ID 추출

        commentService.deleteComment(commentId, authorId);

        return ApiResponse.success(
                200,
                "댓글 삭제 성공",
                null
        );
    }
}