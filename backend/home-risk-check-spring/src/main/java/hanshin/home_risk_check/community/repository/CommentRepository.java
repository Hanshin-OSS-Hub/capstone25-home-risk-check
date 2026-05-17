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
    Slice<Comment> findAllRootComments(@Param("postId") Long postId, Pageable pageable);

    //대댓글 조회
    @EntityGraph(attributePaths = {"user"})
    @Query("""      
               select comment
               from Comment comment
               where (comment.rootComment.id = :rootId)
           """)
    Slice<Comment> findAllChildComments(@Param("rootId") Long rootId, Pageable pageable);

    //답글 개수 조회
    @Query("""
               select comment.rootComment.id as rootId, count(comment) as cnt
               from Comment comment
               where comment.rootComment.id in :rootIds
               group by comment.rootComment.id
           """)
    List<ReplyCount> countByRootIds(@Param("rootIds") List<Long> rootIds);

    interface ReplyCount{
        Long getRootId();
        Long getCnt();
    }
}