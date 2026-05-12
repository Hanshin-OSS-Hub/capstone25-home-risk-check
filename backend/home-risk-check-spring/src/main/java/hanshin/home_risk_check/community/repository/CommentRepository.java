package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
 * 댓글 Repository
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /*
     * 특정 게시글의 댓글 전체 조회 (정렬 메서드명 기반)
     * - rootComment.commentId -> depth -> createdAt 순 정렬
     */
    List<Comment> findAllByPost_PostIdOrderByRootComment_CommentIdAscDepthAscCreatedAtAsc(Long postId);

    /*
     * 특정 게시글의 댓글 페이지 조회
     * 정렬은 호출자가 Pageable의 Sort로 지정.
     */
    Page<Comment> findByPost_PostId(Long postId, Pageable pageable);
}
