package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.Comment;
import hanshin.home_risk_check.community.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    //게시글 댓글 수
    long countByPost(Post post);

    //게시글별 댓글 수
    @Query("""
               select comment.post.id as postId, count(comment) as commentCount
               from Comment comment
               where comment.post in :posts
               group by comment.post
           """)
    List<AllCommentCount> countByPosts(@Param("posts") List<Post> posts);

    //루트 댓글 페이징 조회
    @EntityGraph(attributePaths = {"user", "user.profileImageFile"})
    @Query("""
              select comment
              from Comment comment
              where (comment.post = :post)
              and (comment.rootComment is null)
              order by comment.id asc
          """)
    Slice<Comment> findAllRootCommentsByPostOrderByIdAsc(@Param("post") Post post, Pageable pageable);

    //대댓글 조회
    @EntityGraph(attributePaths = {"user", "user.profileImageFile"})
    @Query("""      
               select comment
               from Comment comment
               where (comment.rootComment = :rootComment)
               order by comment.id asc
           """)
    Slice<Comment> findAllChildCommentsByRootCommentOrderByIdAsc(@Param("rootComment") Comment rootComment, Pageable pageable);

    //답글 개수 조회
    @Query("""
               select comment.rootComment.id as rootCommentId, count(comment) as count
               from Comment comment
               where comment.rootComment in :rootComments
               group by comment.rootComment
           """)
    List<ChildCommentCount> countChildCommentsByRootComments(@Param("rootComments") List<Comment> rootComments);

    //최상위 댓글의 답글 일괄 삭제 (댓글 삭제 시)
    void deleteAllByRootComment(Comment rootComment);

    //게시글의 루트 댓글 일괄 삭제 (답글 삭제 후 호출)
    @Modifying
    @Query("""
               delete
               from Comment comment
               where comment.post = :post
               and comment.rootComment is null
           """)
    void deleteAllRootCommentsByPost(@Param("post") Post post);

    //게시글의 대댓글 일괄 삭제 (게시글 삭제 시 — 자기참조 FK 때문에 답글 먼저)
    @Modifying
    @Query("""
               delete
               from Comment comment
               where comment.post = :post
               and comment.rootComment is not null
           """)
    void deleteAllChildCommentsByPost(@Param("post") Post post);

    interface ChildCommentCount{
        Long getRootCommentId();
        Long getChildCommentCount();
    }

    interface AllCommentCount {
        Long getPostId();
        Long getAllCommentCount();
    }
}