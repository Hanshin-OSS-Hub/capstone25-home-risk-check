package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.dto.*;
import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.entity.PostSortType;
import hanshin.home_risk_check.community.repository.PostRepository;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import hanshin.home_risk_check.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    //게시글 본문(카테고리, 제목, 내용) 생성
    @Transactional
    public Post createPost(User user, PostBaseRequest req) {
        Post post = Post.builder()
                        .postCategory(req.postCategory())
                        .title(req.title())
                        .content(req.content())
                        .user(user)
                        .build();

        return postRepository.save(post);
    }

    //게시글 단일 조회
    public Post getPost(Long postId) {
        return postRepository.findById(postId)
                             .orElseThrow(()  -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    //게시글 일괄 조회
    public Slice<Post> getPosts(PostSearchRequest req) {
        PostSortType sortType = Objects.requireNonNullElse(req.postSortType(), PostSortType.LATEST);
        Pageable pageable = PageRequest.of(req.page(), req.size());

        return switch (sortType) {
            case POPULAR ->
                    postRepository.findAllOrderByLikeCountDesc(req.postCategory(), req.keyword(), pageable);
            case LATEST ->
                    postRepository.findAllOrderByCreatedAtDesc(req.postCategory(), req.keyword(), pageable);
        };
    }

    //게시글 본문(카테고리, 제목, 내용) 수정
    @Transactional
    public Post updatePost(Post post, PostBaseRequest req) {
        post.update(req.postCategory(), req.title(), req.content());
        return post;
    }

    //게시글 본문(카테고리, 제목, 내용) 삭제
    @Transactional
    public void delete(Post post, User user) {
        postRepository.delete(post);
    }

    public boolean isWrittenByMe(Post post, User user) {
        return post.isWrittenBy(user);
    }
}