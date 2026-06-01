package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.entity.PostLike;
import hanshin.home_risk_check.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    List<PostLike> findByPost(Post post);

    //게시글 좋아요 수
    long countByPost(Post post);

    //게시글별 좋아요 수
    @Query("""
               select postLike.post.id as postId, count(postLike) as postLikeCount
               from PostLike postLike
               where postLike.post in :posts
               group by postLike.post
           """)
    List<PostLikeCount> countByPosts(@Param("posts") List<Post> posts);

    //사용자의 게시글 좋아요 여부
    boolean existsByUserAndPost(User user, Post post);

    Optional<PostLike> findByUserAndPost(User user, Post post);

    interface PostLikeCount {
        Long getPostId();
        Long getPostLikeCount();
    }
}
