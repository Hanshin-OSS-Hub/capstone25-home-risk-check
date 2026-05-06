package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
 * 댓글 Repository
 * DB의 comment 테이블에 접근하는 인터페이스
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /*
     * 특정 게시글의 댓글 전체 조회
     *
     * 연관관계 매핑 기준:
     * - post.postId 로 조건 조회
     * - rootComment.commentId -> depth -> createdAt 순으로 정렬
     *
     * 정렬 목적:
     * 1. 같은 루트 댓글 그룹끼리 묶기
     * 2. 댓글(0) -> 대댓글(1) 순 유지
     * 3. 같은 depth 안에서는 작성 시간 순 정렬
     */
    List<Comment> findAllByPost_PostIdOrderByRootComment_CommentIdAscDepthAscCreatedAtAsc(Long postId);
}