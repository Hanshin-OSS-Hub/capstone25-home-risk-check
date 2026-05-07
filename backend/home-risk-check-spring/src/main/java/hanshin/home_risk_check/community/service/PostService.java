package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.dto.PostCreateRequest;
import hanshin.home_risk_check.community.dto.PostResponse;
import hanshin.home_risk_check.community.dto.PostUpdateRequest;
import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.repository.PostRepository;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import hanshin.home_risk_check.user.entity.User; // [변경] 작성자 User 사용
import hanshin.home_risk_check.user.repository.UserRepository; // [변경] email로 User 조회
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * 게시글 Service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageService postImageService;
    private final UserRepository userRepository; // [변경] 로그인 사용자 조회용 Repository 추가

    public Page<PostResponse> getPosts(String categoryLabel, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Post> posts;

        if (categoryLabel == null || categoryLabel.isBlank()) {
            posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            posts = postRepository.findAllByCategoryLabelOrderByCreatedAtDesc(categoryLabel, pageable);
        }

        return posts.map(PostResponse::from);
    }

    public PostResponse getPost(Long postId) {
        Post post = findPost(postId);
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse createPost(String email, PostCreateRequest request) { // [변경] Long authorId -> String email
        User user = findLoginUser(email); // [변경] email로 실제 로그인 User 조회

        Post post = Post.builder()
                .user(user) // [변경] authorId 대신 User 연관관계 저장
                .categoryLabel(request.getCategoryLabel())
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        Post savedPost = postRepository.save(post);

        return PostResponse.from(savedPost);
    }

    @Transactional
    public PostResponse updatePost(Long postId, String email, PostUpdateRequest request) { // [변경] Long authorId -> String email
        User user = findLoginUser(email); // [변경]
        Post post = findPost(postId);

        validateAuthor(post.getUser().getId(), user.getId()); // [변경] post.getAuthorId() -> post.getUser().getId()

        post.update(
                request.getCategoryLabel(),
                request.getTitle(),
                request.getContent()
        );

        return PostResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId, String email) { // [변경] Long authorId -> String email
        User user = findLoginUser(email); // [변경]
        Post post = findPost(postId);

        validateAuthor(post.getUser().getId(), user.getId()); // [변경] post.getAuthorId() -> post.getUser().getId()

        postImageService.deleteAllFilesByPostId(postId);
        postRepository.delete(post);
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    /*
     * [변경]
     * JWT Filter에서 Authentication principal로 저장한 email을 기준으로
     * 실제 User 엔티티를 조회한다.
     */
    private User findLoginUser(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN_REQUEST);
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN_REQUEST));
    }

    /*
     * [변경]
     * 기존 authorId 비교는 유지하되,
     * 값의 출처가 Post.user.id / 로그인 User.id 로 변경됨.
     */
    private void validateAuthor(Long postAuthorId, Long currentUserId) {
        if (!postAuthorId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_REQUEST);
        }
    }
}