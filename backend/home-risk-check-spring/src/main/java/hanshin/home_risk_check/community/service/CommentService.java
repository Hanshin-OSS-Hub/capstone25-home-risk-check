package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.dto.CommentCreateRequest;
import hanshin.home_risk_check.community.dto.CommentResponse;
import hanshin.home_risk_check.community.entity.Comment;
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
 * 댓글 관련 비즈니스 로직을 처리하는 계층
 *
 * 역할:
 * - 특정 게시글의 댓글 목록 조회
 * - 댓글 작성
 * - 대댓글 작성
 * - 댓글 삭제
 * - 게시글 존재 여부 확인
 * - 댓글 존재 여부 확인
 * - 댓글 작성자 권한 검증
 *
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    /*
     * 댓글 DB 접근용 Repository
     */
    private final CommentRepository commentRepository;

    /*
     * 게시글 존재 여부 확인용 Repository
     */
    private final PostRepository postRepository;

    /*
     * 특정 게시글의 댓글 목록 조회
     *
     * 1. 게시글 존재 여부 확인
     * 2. 댓글 목록을 rootCommentId, depth, createdAt 기준으로 정렬 조회
     * 3. Entity → Response DTO 변환
     */
    public List<CommentResponse> getComments(Long postId) {
        validatePostExists(postId);

        return commentRepository.findAllByPostIdOrderByRootCommentIdAscDepthAscCreatedAtAsc(postId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    /*
     * 댓글 작성
     *
     * 경우 1) 일반 댓글 작성
     * - parentCommentId == null
     * - depth = 0
     * - rootCommentId는 자기 자신(commentId)
     *
     * 경우 2) 대댓글 작성
     * - parentCommentId != null
     * - depth = 1
     * - rootCommentId는 루트 댓글 ID
     *
     */
    @Transactional
    public CommentResponse createComment(Long postId, Long authorId, CommentCreateRequest request) {
        validatePostExists(postId);

        Long parentCommentId = request.getParentCommentId();

        /*
         * 일반 댓글 작성
         *
         * parentCommentId가 없으면 루트 댓글로 간주
         */
        if (parentCommentId == null) {
            Comment rootComment = Comment.builder()
                    .postId(postId)
                    .authorId(authorId)
                    .content(request.getContent())
                    .parentCommentId(null)   // 부모 없음
                    .rootCommentId(null)     // 일단 null로 저장 후 자기 자신으로 세팅
                    .depth(0)                // 루트 댓글
                    .build();

            Comment savedRootComment = commentRepository.save(rootComment);

            /*
             * 루트 댓글의 rootCommentId는 자기 자신의 commentId가 되어야 함
             *
             * commentId는 save 이후에 생성되므로
             * 저장 후 다시 세팅
             */
            savedRootComment.setRootCommentId(savedRootComment.getCommentId());

            return CommentResponse.from(savedRootComment);
        }

        /*
         * 대댓글 작성
         *
         * parentCommentId가 있으면 부모 댓글을 조회
         */
        Comment parentComment = findComment(parentCommentId);

        /*
         * 부모 댓글이 현재 게시글의 댓글인지 검증
         * 다른 게시글의 댓글에 답글 달면 안 됨
         */
        if (!parentComment.getPostId().equals(postId)) {
            throw new BusinessException(ErrorCode.INVALID_COMMENT_POST);
        }

        /*
         * rootCommentId 결정
         *
         * 1. 부모가 루트 댓글(depth=0)이면
         *    rootCommentId = 부모의 commentId
         *
         * 2. 부모가 이미 대댓글(depth=1)이면
         *    rootCommentId = 부모의 rootCommentId
         *
         * 즉, 대댓글의 대댓글을 달더라도
         * 최상위 댓글 밑으로 평탄화해서 저장
         */
        Long rootCommentId;
        if (parentComment.getDepth() == 0) {
            rootCommentId = parentComment.getCommentId();
        } else {
            rootCommentId = parentComment.getRootCommentId();
        }

        /*
         * 대댓글 저장
         *
         * depth는 항상 1
         * parentCommentId와 rootCommentId 모두 루트 댓글 기준으로 맞춤
         */
        Comment replyComment = Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .content(request.getContent())
                .parentCommentId(rootCommentId)  // 부모는 루트 댓글 기준
                .rootCommentId(rootCommentId)    // 루트 댓글 ID
                .depth(1)                        // 항상 대댓글 depth
                .build();

        Comment savedReplyComment = commentRepository.save(replyComment);
        return CommentResponse.from(savedReplyComment);
    }

    /*
     * 댓글 삭제
     *
     * 1. 댓글 존재 여부 확인
     * 2. 현재 요청 사용자가 작성자인지 검증
     * 3. 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long commentId, Long authorId) {
        Comment comment = findComment(commentId);
        validateAuthor(comment.getAuthorId(), authorId);

        commentRepository.delete(comment);
    }

    /*
     * 게시글 존재 여부 확인
     *
     * 댓글 작성/조회 전에
     * 해당 게시글이 실제 존재하는지 검증
     */
    private void validatePostExists(Long postId) {
        boolean exists = postRepository.existsById(postId);
        if (!exists) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
    }

    /*
     * 댓글 조회 공통 메서드
     *
     * commentId로 댓글을 찾고,
     * 없으면 COMMENT_NOT_FOUND 예외 발생
     */
    private Comment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    /*
     * 댓글 작성자 검증
     *
     * 현재 요청한 사용자가 댓글 작성자가 아니면
     * 권한 예외 발생
     */
    private void validateAuthor(Long commentAuthorId, Long currentAuthorId) {
        if (!commentAuthorId.equals(currentAuthorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_REQUEST);
        }
    }
}