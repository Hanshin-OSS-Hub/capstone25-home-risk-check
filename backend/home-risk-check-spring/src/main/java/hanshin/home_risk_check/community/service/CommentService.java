package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.dto.CommentCreateRequest;
import hanshin.home_risk_check.community.dto.CommentResponse;
import hanshin.home_risk_check.community.entity.Comment;
import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.repository.CommentRepository;
import hanshin.home_risk_check.community.repository.PostRepository;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import hanshin.home_risk_check.user.entity.User; // [변경] 작성자 User 사용
import hanshin.home_risk_check.user.repository.UserRepository; // [변경] email로 User 조회
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * 댓글 Service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository; // [변경] 로그인 사용자 조회용 Repository 추가

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
     */
    @Transactional
    public CommentResponse createComment(Long postId, String email, CommentCreateRequest request) { // [변경] Long authorId -> String email
        User user = findLoginUser(email); // [변경] email로 실제 로그인 User 조회

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        Long parentCommentId = request.getParentCommentId();

        /*
         * 1) 일반 댓글 작성
         */
        if (parentCommentId == null) {
            Comment rootComment = Comment.builder()
                    .post(post)
                    .user(user) // [변경] authorId 대신 User 연관관계 저장
                    .content(request.getContent())
                    .parentComment(null)
                    .rootComment(null)
                    .depth(0)
                    .build();

            Comment saved = commentRepository.save(rootComment);

            saved.setRootComment(saved);

            return CommentResponse.from(saved);
        }

        /*
         * 2) 대댓글 작성
         */
        Comment parent = findComment(parentCommentId);

        if (!parent.getPost().getPostId().equals(postId)) {
            throw new BusinessException(ErrorCode.INVALID_COMMENT_POST);
        }

        if (parent.getDepth() >= 1) {
            throw new BusinessException(ErrorCode.INVALID_COMMENT_DEPTH);
        }

        Comment reply = Comment.builder()
                .post(post)
                .user(user) // [변경] authorId 대신 User 연관관계 저장
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
    public void deleteComment(Long commentId, String email) { // [변경] Long authorId -> String email
        User user = findLoginUser(email); // [변경]
        Comment comment = findComment(commentId);

        validateAuthor(comment.getUser().getId(), user.getId()); // [변경] comment.getAuthorId() -> comment.getUser().getId()

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
     * 값의 출처가 Comment.user.id / 로그인 User.id 로 변경됨.
     */
    private void validateAuthor(Long commentAuthorId, Long currentUserId) {
        if (!commentAuthorId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_REQUEST);
        }
    }
}