package hanshin.home_risk_check.community.controller;

import hanshin.home_risk_check.community.dto.PostCreateRequest;
import hanshin.home_risk_check.community.dto.PostResponse;
import hanshin.home_risk_check.community.dto.PostUpdateRequest;
import hanshin.home_risk_check.community.service.PostService;
import hanshin.home_risk_check.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/*
 * 게시글 Controller
 *
 * 클라이언트(프론트, Postman 등)의 HTTP 요청을 받아
 * 게시글 관련 Service로 전달하는 역할
 *
 * 역할:
 * - 게시글 목록 조회 API
 * - 게시글 단건 조회 API
 * - 게시글 작성 API
 * - 게시글 수정 API
 * - 게시글 삭제 API
 */
@RestController
// 이 클래스가 REST API Controller임을 의미
// 반환값이 JSON 형태로 응답됨

@RequiredArgsConstructor
// final 필드를 생성자로 자동 주입
// 여기서는 PostService 주입에 사용

@RequestMapping("/api/posts")
// 게시글 관련 API의 공통 URL prefix
// 즉 모든 메서드는 /api/posts 아래에 매핑됨
public class PostController {

    /*
     * 게시글 비즈니스 로직을 처리하는 Service
     */
    private final PostService postService;

    /*
     * 게시글 목록 조회 API
     *
     * GET /api/posts
     *
     * query parameter:
     * - categoryLabel (선택)
     * - page (기본값 0)
     * - size (기본값 10)
     *
     * 예:
     * /api/posts?page=0&size=10
     * /api/posts?categoryLabel=서울시 성동구&page=0&size=10
     */
    @GetMapping
    public ApiResponse<Page<PostResponse>> getPosts(
            @RequestParam(required = false) String categoryLabel,
            // 카테고리 필터 (없으면 전체 조회)

            @RequestParam(defaultValue = "0") int page,
            // 페이지 번호 (기본 0)

            @RequestParam(defaultValue = "10") int size
            // 한 페이지당 게시글 개수 (기본 10)
    ) {
        return ApiResponse.success(postService.getPosts(categoryLabel, page, size));
    }

    /*
     * 게시글 단건 조회 API
     *
     * GET /api/posts/{postId}
     *
     * 예:
     * /api/posts/1
     */
    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPost(@PathVariable Long postId) {
        // URL 경로의 postId 값을 받아 게시글 하나 조회
        return ApiResponse.success(postService.getPost(postId));
    }

    /*
     * 게시글 작성 API
     *
     * POST /api/posts
     *
     * 요청 body 예:
     * {
     *   "categoryLabel": "서울시 성동구",
     *   "title": "이 매물 괜찮나요?",
     *   "content": "등기부등본을 보니 조금 이상합니다."
     * }
     */
    @PostMapping
    public ApiResponse<PostResponse> createPost(@RequestBody PostCreateRequest request) {

        /*
         * 현재는 인증 기능이 아직 없어서 authorId를 임시값 1L로 고정
         * 나중에 JWT 붙이면 로그인 사용자 ID를 여기서 꺼내야 함
         */
        Long authorId = 1L; // TODO: 추후 JWT에서 사용자 ID 추출

        return ApiResponse.success(
                201,
                "게시글 작성 성공",
                postService.createPost(authorId, request)
        );
    }

    /*
     * 게시글 수정 API
     *
     * PATCH /api/posts/{postId}
     *
     * 요청 body 예:
     * {
     *   "categoryLabel": "서울시 성동구",
     *   "title": "수정된 제목",
     *   "content": "수정된 내용"
     * }
     */
    @PatchMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable Long postId,
            @RequestBody PostUpdateRequest request
    ) {

        /*
         * 현재는 인증 기능이 없어서 작성자 ID를 임시 고정값으로 사용
         */
        Long authorId = 1L; // TODO: 추후 JWT에서 사용자 ID 추출

        return ApiResponse.success(
                postService.updatePost(postId, authorId, request)
        );
    }

    /*
     * 게시글 삭제 API
     *
     * DELETE /api/posts/{postId}
     *
     * 예:
     * DELETE /api/posts/1
     */
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId) {

        /*
         * 현재는 인증 기능이 없어서 작성자 ID를 임시 고정값으로 사용
         */
        Long authorId = 1L; // TODO: 추후 JWT에서 사용자 ID 추출

        postService.deletePost(postId, authorId);

        return ApiResponse.success(200, "게시글 삭제 성공", null);
    }
}