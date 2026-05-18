package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    //루트 댓글 페이징 조회
    @EntityGraph(attributePaths = {"user"})
    @Query("""
              select comment
              from Comment comment
              where (comment.post.id = :postId)
              and (comment.rootComment is null)
          """)
    Slice<Comment> findAllRootCommentsByPostId(@Param("postId") Long postId, Pageable pageable);

    //대댓글 조회
    @EntityGraph(attributePaths = {"user"})
    @Query("""      
               select comment
               from Comment comment
               where (comment.rootComment.id = :rootCommentId)
           """)
    Slice<Comment> findAllChildCommentsByRootCommentId(@Param("rootCommentId") Long rootCommentId, Pageable pageable);

    //답글 개수 조회
    @Query("""
               select comment.rootComment.id as rootCommentId, count(comment) as count
               from Comment comment
               where comment.rootComment.id in :rootCommentIds
               group by comment.rootComment.id
           """)
    List<ChildCommentCount> countChildCommentsByRootCommentIds(@Param("rootCommentIds") List<Long> rootCommentIds);

    interface ChildCommentCount{
        Long getRootCommentId();
        Long getCount();
    }
}