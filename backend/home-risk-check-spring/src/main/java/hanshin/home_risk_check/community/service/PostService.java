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
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageService postImageService;

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
    public PostResponse createPost(Long authorId, PostCreateRequest request) {
        Post post = Post.builder()
                .authorId(authorId)
                .categoryLabel(request.getCategoryLabel())
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        Post savedPost = postRepository.save(post);
        return PostResponse.from(savedPost);
    }

    @Transactional
    public PostResponse updatePost(Long postId, Long authorId, PostUpdateRequest request) {
        Post post = findPost(postId);
        validateAuthor(post.getAuthorId(), authorId);

        post.update(
                request.getCategoryLabel(),
                request.getTitle(),
                request.getContent()
        );

        return PostResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId, Long authorId) {
        Post post = findPost(postId);
        validateAuthor(post.getAuthorId(), authorId);

        /*
         * DB에서 게시글이 삭제되기 전에
         * 연결된 실제 이미지 파일도 먼저 정리한다.
         */
        postImageService.deleteAllFilesByPostId(postId);

        postRepository.delete(post);
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private void validateAuthor(Long postAuthorId, Long currentAuthorId) {
        if (!postAuthorId.equals(currentAuthorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_REQUEST);
        }
    }
}