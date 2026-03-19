package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
 * 댓글 Repository
 * DB의 comment 테이블에 접근하는 인터페이스
 *
 * JpaRepository<Comment, Long>
 * - Comment: 관리할 엔티티 타입
 * - Long: Comment 엔티티의 PK 타입 (commentId)
 *
 * 기본 CRUD 메서드는 JpaRepository에서 제공됨
 * 예:
 * save(), findById(), findAll(), delete()
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /*
     * 특정 게시글의 댓글 전체 조회
     *
     * postId 조건으로 댓글을 조회하고
     * rootCommentId → depth → createdAt 순으로 정렬
     *
     * 정렬 이유:
     * 1. rootCommentId
     *    같은 댓글 그룹을 묶기 위해
     *
     * 2. depth
     *    댓글(0) → 대댓글(1) 순서 유지
     *
     * 3. createdAt
     *    같은 depth 안에서는 작성 시간 순 정렬
     *
     * 결과 구조 예:
     *
     * 댓글1
     *  ├ 대댓글1
     *  └ 대댓글2
     *
     * 댓글2
     *  └ 대댓글3
     *
     * SQL로 표현하면 대략 이런 느낌
     *
     * SELECT *
     * FROM comment
     * WHERE post_id = ?
     * ORDER BY root_comment_id ASC, depth ASC, created_at ASC
     */
    List<Comment> findAllByPostIdOrderByRootCommentIdAscDepthAscCreatedAtAsc(Long postId);
}