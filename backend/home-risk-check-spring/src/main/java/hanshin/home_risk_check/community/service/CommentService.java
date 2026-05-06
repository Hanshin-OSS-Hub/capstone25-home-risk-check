package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.dto.CommentCreateRequest;
import hanshin.home_risk_check.community.dto.CommentResponse;
import hanshin.home_risk_check.community.entity.Comment;
import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.repository.CommentRepository;
import hanshin.home_risk_check.community.repository.PostRepository;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * 댓글 Service
 *
 * 댓글 관련 비즈니스 로직 처리 계층
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    /*
     * 특정 게시글의 댓글 목록 조회
     */
    public List<CommentResponse> getComments(Long postId) {
        validatePostExists(postId);

        return commentRepository
                .findAllByPost_PostIdOrderByRootComment_CommentIdAscDepthAscCreatedAtAsc(postId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    /*
     * 댓글 작성
     *
     * 정책:
     * - depth 0: 일반 댓글
     * - depth 1: 대댓글
     * - 대댓글의 대댓글은 허용하지 않음
     */
    @Transactional
    public CommentResponse createComment(Long postId, Long authorId, CommentCreateRequest request) {

        /*
         * 댓글이 달릴 게시글이 실제 존재하는지 확인
         */
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        Long parentCommentId = request.getParentCommentId();

        /*
         * 1) 일반 댓글 작성
         *
         * 일반 댓글은 parentComment = null
         * rootComment도 처음엔 null로 저장하고,
         * 저장 후 자기 자신을 rootComment로 세팅한다.
         */
        if (parentCommentId == null) {
            Comment rootComment = Comment.builder()
                    .post(post)
                    .authorId(authorId)
                    .content(request.getContent())
                    .parentComment(null)
                    .rootComment(null)
                    .depth(0)
                    .build();

            Comment saved = commentRepository.save(rootComment);

            /*
             * 루트 댓글은 자기 자신이 rootComment가 된다.
             * 트랜잭션 안이므로 dirty checking으로 반영된다.
             */
            saved.setRootComment(saved);

            return CommentResponse.from(saved);
        }

        /*
         * 2) 대댓글 작성
         */
        Comment parent = findComment(parentCommentId);

        /*
         * 다른 게시글의 댓글에 대댓글 다는 것 방지
         */
        if (!parent.getPost().getPostId().equals(postId)) {
            throw new BusinessException(ErrorCode.INVALID_COMMENT_POST);
        }

        /*
         * 대댓글의 대댓글 방지
         *
         * 현재 정책상 depth 1까지만 허용
         */
        if (parent.getDepth() >= 1) {
            throw new BusinessException(ErrorCode.INVALID_COMMENT_DEPTH);
        }

        Comment reply = Comment.builder()
                .post(post)
                .authorId(authorId)
                .content(request.getContent())
                .parentComment(parent)
                .rootComment(parent)
                .depth(1)
                .build();

        Comment saved = commentRepository.save(reply);

        return CommentResponse.from(saved);
    }

    /*
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long commentId, Long authorId) {
        Comment comment = findComment(commentId);
        validateAuthor(comment.getAuthorId(), authorId);

        commentRepository.delete(comment);
    }

    /*
     * 게시글 존재 여부 확인
     */
    private void validatePostExists(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
    }

    /*
     * 댓글 단건 조회 공통 메서드
     */
    private Comment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    /*
     * 작성자 검증 공통 메서드
     */
    private void validateAuthor(Long commentAuthorId, Long currentAuthorId) {
        if (!commentAuthorId.equals(currentAuthorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_REQUEST);
        }
    }
}