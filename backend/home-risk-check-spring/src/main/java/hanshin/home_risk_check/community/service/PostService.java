package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.dto.PostCreateRequest;
import hanshin.home_risk_check.community.dto.PostResponse;
import hanshin.home_risk_check.community.dto.PostUpdateRequest;
import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.repository.PostRepository;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * 게시글 Service
 *
 * 게시글과 관련된 비즈니스 로직을 처리하는 계층
 *
 * 역할:
 * - 게시글 목록 조회
 * - 게시글 단건 조회
 * - 게시글 작성
 * - 게시글 수정
 * - 게시글 삭제
 * - 작성자 권한 검증
 */
@Service  // Spring이 Service Bean으로 등록
@RequiredArgsConstructor  // final 필드 생성자 자동 생성 (의존성 주입용)
@Transactional(readOnly = true)
// 기본적으로 조회 전용 트랜잭션
// 수정/삭제/작성 메서드만 따로 @Transactional 붙여서 쓰기 위함
public class PostService {

    /*
     * 게시글 DB 접근용 Repository
     */
    private final PostRepository postRepository;

    /*
     * 게시글 목록 조회
     *
     * categoryLabel이 없으면 전체 게시글 조회
     * categoryLabel이 있으면 해당 카테고리 게시글만 조회
     *
     * page, size를 받아서 페이지네이션 적용
     */
    public Page<PostResponse> getPosts(String categoryLabel, int page, int size) {

        /*
         * PageRequest.of(page, size, sort)
         * - page: 페이지 번호
         * - size: 한 페이지당 게시글 개수
         * - Sort: createdAt 기준 최신순 정렬
         */
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Post> posts;

        /*
         * categoryLabel이 비어 있으면 전체 조회
         * 값이 있으면 카테고리별 조회
         */
        if (categoryLabel == null || categoryLabel.isBlank()) {
            posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            posts = postRepository.findAllByCategoryLabelOrderByCreatedAtDesc(categoryLabel, pageable);
        }

        /*
         * Entity(Post)를 응답 DTO(PostResponse)로 변환
         */
        return posts.map(PostResponse::from);
    }

    /*
     * 게시글 단건 조회
     *
     * postId로 게시글 하나 조회
     * 없으면 예외 발생
     */
    public PostResponse getPost(Long postId) {
        Post post = findPost(postId);
        return PostResponse.from(post);
    }

    /*
     * 게시글 작성
     *
     * authorId는 현재 로그인 사용자 ID
     * request에는 categoryLabel, title, content가 들어 있음
     */
    @Transactional
    public PostResponse createPost(Long authorId, PostCreateRequest request) {

        /*
         * 요청 DTO 값을 이용해 Post 엔티티 생성
         */
        Post post = Post.builder()
                .authorId(authorId)
                .categoryLabel(request.getCategoryLabel())
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        /*
         * DB 저장
         */
        Post savedPost = postRepository.save(post);

        /*
         * 저장된 엔티티를 응답 DTO로 변환
         */
        return PostResponse.from(savedPost);
    }

    /*
     * 게시글 수정
     *
     * 1. 게시글 존재 여부 확인
     * 2. 작성자 권한 확인
     * 3. 게시글 내용 수정
     */
    @Transactional
    public PostResponse updatePost(Long postId, Long authorId, PostUpdateRequest request) {

        /*
         * 게시글 조회 (없으면 예외 발생)
         */
        Post post = findPost(postId);

        /*
         * 현재 요청한 사용자가 작성자인지 검증
         */
        validateAuthor(post.getAuthorId(), authorId);

        /*
         * 엔티티 내부 update 메서드로 값 변경
         *
         * JPA는 트랜잭션 안에서 엔티티 값이 변경되면
         * dirty checking(변경 감지)으로 UPDATE 쿼리를 자동 실행함
         */
        post.update(
                request.getCategoryLabel(),
                request.getTitle(),
                request.getContent()
        );

        /*
         * 별도로 save()를 다시 안 해도 됨
         * (트랜잭션 종료 시점에 자동 반영)
         */
        return PostResponse.from(post);
    }

    /*
     * 게시글 삭제
     *
     * 1. 게시글 존재 여부 확인
     * 2. 작성자 권한 확인
     * 3. 게시글 삭제
     */
    @Transactional
    public void deletePost(Long postId, Long authorId) {
        Post post = findPost(postId);
        validateAuthor(post.getAuthorId(), authorId);

        postRepository.delete(post);
    }

    /*
     * 게시글 조회 공통 메서드
     *
     * postId로 게시글을 찾고,
     * 없으면 POST_NOT_FOUND 예외 발생
     */
    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    /*
     * 작성자 검증 공통 메서드
     *
     * 게시글 작성자와 현재 요청 사용자가 다르면 예외 발생
     */
    private void validateAuthor(Long postAuthorId, Long currentAuthorId) {
        if (!postAuthorId.equals(currentAuthorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_REQUEST);
        }
    }
}