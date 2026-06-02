package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.dto.*;
import hanshin.home_risk_check.community.entity.*;
import hanshin.home_risk_check.community.mapper.CommentResponseMapper;
import hanshin.home_risk_check.community.mapper.PostDetailResponseMapper;
import hanshin.home_risk_check.community.mapper.PostLikeResponseMapper;
import hanshin.home_risk_check.community.mapper.PostSummaryResponseMapper;
import hanshin.home_risk_check.community.util.AuthorValidator;
import hanshin.home_risk_check.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityFacade {

    private final PostService postService;
    private final PostPollService postPollService;
    private final PostPlaceService postPlaceService;
    private final PostLikeService postLikeService;
    private final PostImageService postImageService;
    private final CommentService commentService;
    private final PostDetailResponseMapper postDetailResponseMapper;
    private final PostSummaryResponseMapper postSummaryResponseMapper;
    private final PostLikeResponseMapper postLikeResponseMapper;
    private final CommentResponseMapper commentResponseMapper;

    // ─────────────────────────── 게시글 본문 ───────────────────────────

    @Transactional
    public PostDetailResponse createPost(User user, PostCreateRequest req, List<MultipartFile> images) {
        Post post = postService.createPost(user, req.postBaseRequest());
        postPlaceService.createPostPlace(post, req.postPlaceRequest());
        postPollService.createPostPoll(post, req.postPollRequest());
        postImageService.createPostImages(post, images);
        log.info("게시글 작성 - postId={}, userId={}", post.getId(), user.getId());
        return getPost(user, post.getId());
    }

    public PostDetailResponse getPost(User user, Long postId) {
        Post post = postService.getPost(postId);
        PostPlace postPlace = postPlaceService.getPostPlace(post);
        PostPoll postPoll = postPollService.getPostPoll(post);
        List<PollOption> pollOptions = postPollService.getPollOptions(postPoll);
        List<PostImage> postImages = postImageService.getPostImages(post);

        Map<Long, Long> voteCountPerOption = postPollService.getPollOptionCounts(pollOptions);
        List<Long> mySelectedOptionIds = postPollService.getSelectedPollOptionIds(postPoll, user);

        boolean isWrittenByMe = postService.isWrittenByMe(post, user);
        boolean isLikedByMe = postLikeService.isLikedBy(user, post);
        long likeCount = postLikeService.getPostLikeCount(post);
        long commentCount = commentService.getCommentCount(post);
        long totalVoteCount = postPollService.getTotalVoteCount(voteCountPerOption);

        return postDetailResponseMapper.toPostDetailResponse(post, postPlace, postPoll, pollOptions, postImages,
                voteCountPerOption, mySelectedOptionIds, isWrittenByMe, isLikedByMe, likeCount,
                commentCount, totalVoteCount
        );
    }

    public Slice<PostSummaryResponse> getPosts(PostSearchRequest req) {
        Slice<Post> posts = postService.getPosts(req);
        List<Post> postList = posts.getContent();

        Map<Long, PostImage> thumbnails = postImageService.getThumbnails(postList);
        Map<Long, Long> postLikeCounts = postLikeService.getPostLikeCounts(postList);
        Map<Long, Long> commentCounts = commentService.getCommentCounts(postList);

        return postSummaryResponseMapper.toPostSummaryResponses(posts, thumbnails, postLikeCounts, commentCounts);
    }

    @Transactional
    public PostDetailResponse updatePost(User user, Long postId, PostUpdateRequest req, List<MultipartFile> images) {
        Post post = postService.getPost(postId);
        AuthorValidator.validate(post.getUser(), user);
        PostPlace postPlace = postPlaceService.getPostPlace(post);

        postService.updatePost(post, req.postBaseRequest());
        postPlaceService.updatePostPlace(post, postPlace, req.postPlaceRequest());
        postImageService.updatePostImages(post, req.deletePostImageIds(), images);

        log.info("게시글 수정 - postId={}, userId={}", postId, user.getId());
        return getPost(user, postId);
    }

    @Transactional
    public void deletePost(User user, Long postId) {
        Post post = postService.getPost(postId);
        AuthorValidator.validate(post.getUser(), user);
        PostPlace postPlace = postPlaceService.getPostPlace(post);
        PostPoll postPoll = postPollService.getPostPoll(post);
        List<PostImage> postImages = postImageService.getPostImages(post);
        List<PostLike> postLikes = postLikeService.getPostLikes(post);

        postPollService.delete(postPoll);
        postLikeService.delete(postLikes);
        postImageService.deletePostImages(postImages);
        postPlaceService.delete(postPlace);
        commentService.deleteComments(post);
        postService.delete(post, user);
        log.info("게시글 삭제 - postId={}, userId={}", postId, user.getId());
    }

    // ─────────────────────────── 게시글 좋아요 ───────────────────────────

    @Transactional
    public PostLikeResponse like(User user, Long postId) {
        Post post = postService.getPost(postId);
        postLikeService.create(user, post);
        return postLikeResponseMapper.toPostLikeResponse(post, true, postLikeService.getPostLikeCount(post));
    }

    @Transactional
    public PostLikeResponse cancelLike(User user, Long postId) {
        Post post = postService.getPost(postId);
        postLikeService.cancel(user, post);
        return postLikeResponseMapper.toPostLikeResponse(post, false, postLikeService.getPostLikeCount(post));
    }

    @Transactional
    public PostLikeResponse toggleLike(User user, Long postId) {
        Post post = postService.getPost(postId);
        boolean liked = postLikeService.toggle(user, post);
        return postLikeResponseMapper.toPostLikeResponse(post, liked, postLikeService.getPostLikeCount(post));
    }

    // ─────────────────────────── 투표 ────────────────────────────

    @Transactional
    public PostDetailResponse vote(User user, Long postId, PostPollVoteRequest req) {
        Post post = postService.getPost(postId);
        postPollService.vote(post, user, req.pollOptionIds());
        return getPost(user, postId);
    }

    // ─────────────────────────── 댓글 ────────────────────────────
    // TODO: 알림 / soft-delete 도입 시 cross-aggregate 정책을 이 layer에 추가
    //  - createComment: 게시글 작성자/대댓글 부모 작성자에게 알림
    //  - deleteComment: 자식 유무에 따라 soft-delete / hard-delete 분기, 댓글 좋아요 cleanup

    public Slice<CommentResponse> getRootComments(Long postId, User user, Pageable pageable) {
        Slice<Comment> rootComments = commentService.getRootComments(postId, pageable);
        Map<Long, Long> replyCounts = commentService.getChildCommentCounts(rootComments.getContent());
        Map<Long, Boolean> isWrittenByMe = commentService.isWrittenByMe(rootComments.getContent(), user);
        return commentResponseMapper.toCommentResponses(rootComments, isWrittenByMe, replyCounts);
    }

    public Slice<CommentResponse> getChildComments(Long rootCommentId, User user, Pageable pageable) {
        Slice<Comment> childComments = commentService.getChildComments(rootCommentId, pageable);
        Map<Long, Boolean> isWrittenByMe = commentService.isWrittenByMe(childComments.getContent(), user);
        return commentResponseMapper.toCommentResponses(childComments, isWrittenByMe, Map.of());
    }

    @Transactional
    public CommentResponse createComment(Long postId, User user, CommentCreateRequest req) {
        Comment comment = commentService.createComment(postId, user, req);
        boolean isWrittenByMe = commentService.isWrittenByMe(comment, user);
        log.info("댓글 작성 - commentId={}, postId={}, userId={}", comment.getId(), postId, user.getId());
        return commentResponseMapper.toCommentResponse(comment, isWrittenByMe, 0L);
    }

    @Transactional
    public void deleteComment(Long commentId, User user) {
        Comment comment = commentService.getComment(commentId);
        AuthorValidator.validate(comment.getUser(), user);
        commentService.deleteComment(comment);
        log.info("댓글 삭제 - commentId={}, userId={}", commentId, user.getId());
    }
}