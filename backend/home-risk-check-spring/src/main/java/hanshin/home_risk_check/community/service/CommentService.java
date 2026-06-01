package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.dto.CommentCreateRequest;
import hanshin.home_risk_check.community.entity.Comment;
import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.repository.CommentRepository;
import hanshin.home_risk_check.community.repository.CommentRepository.ChildCommentCount;
import hanshin.home_risk_check.community.repository.CommentRepository.AllCommentCount;
import hanshin.home_risk_check.community.repository.PostRepository;
import hanshin.home_risk_check.community.util.AuthorValidator;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import hanshin.home_risk_check.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    //루트 댓글 조회
    public Slice<Comment> getRootComments(Long postId, Pageable pageable) {
        Post post = getPost(postId);
        return commentRepository.findAllRootCommentsByPostOrderByIdAsc(post, pageable);
    }

    //루트 댓글 대댓글 목록
    public Slice<Comment> getChildComments(Long rootCommentId, Pageable pageable) {
        Comment rootComment = getComment(rootCommentId);
        return commentRepository.findAllChildCommentsByRootCommentOrderByIdAsc(rootComment, pageable);
    }

    /* 댓글/대댓글 작성 */
    @Transactional
    public Comment createComment(Long postId, User user, CommentCreateRequest request) {
        Post post = getPost(postId);
        Long rootCommentId = request.rootCommentId();

        // 댓글
        if (rootCommentId == null) {
            return commentRepository.save(Comment.createRootComment(post, user, request.content()));
        }

        // 대댓글
        Comment rootComment = getComment(rootCommentId);
        if (!rootComment.getPost().getId().equals(postId)) {
            throw new BusinessException(ErrorCode.INVALID_COMMENT_POST);
        }
        if (rootComment.getRootComment() != null) {
            throw new BusinessException(ErrorCode.INVALID_COMMENT_DEPTH);
        }
        return commentRepository.save(Comment.createReplyComment(post, user, rootComment, request.content()));
    }

    /* 댓글/대댓글 삭제 (루트면 대댓글까지 cascade) */
    @Transactional
    public void deleteComment(Comment comment) {
        if (comment.getRootComment() == null) {
            commentRepository.deleteAllByRootComment(comment);
        }
        commentRepository.delete(comment);
    }

    /* 게시글 삭제 시 댓글 일괄 삭제 (대댓글 먼저 → 루트, 자기참조 FK) */
    @Transactional
    public void deleteComments(Post post) {
        commentRepository.deleteAllChildCommentsByPost(post);
        commentRepository.deleteAllRootCommentsByPost(post);
    }

    //게시글 댓글 수 단일 조회
    public long getCommentCount(Post post) {
        return commentRepository.countByPost(post);
    }

    //게시글 댓글 수 일괄 조회
    public Map<Long, Long> getCommentCounts(List<Post> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> commentCounts = commentRepository.countByPosts(posts)
                                                         .stream()
                                                         .collect(Collectors.toMap(AllCommentCount::getPostId, AllCommentCount::getAllCommentCount));

        posts.forEach(post -> commentCounts.putIfAbsent(post.getId(), 0L));
        return commentCounts;
    }

    //대댓글 수 일괄 조회
    public Map<Long, Long> getChildCommentCounts(List<Comment> rootComments) {
        if (rootComments.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> childCommentCounts = commentRepository.countChildCommentsByRootComments(rootComments)
                                                              .stream()
                                                              .collect(Collectors.toMap(ChildCommentCount::getRootCommentId, ChildCommentCount::getChildCommentCount));

        rootComments.forEach(rootComment -> childCommentCounts.putIfAbsent(rootComment.getId(), 0L));
        return childCommentCounts;
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    public Comment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    public boolean isWrittenByMe(Comment comment, User user) {
        return comment.isWrittenBy(user);
    }

    public Map<Long, Boolean> isWrittenByMe(List<Comment> comments, User user) {
        return comments.stream()
                       .collect(Collectors.toMap(
                               Comment::getId,
                               comment -> comment.isWrittenBy(user)
                       ));
    }
}