package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.entity.PostCategory;
import hanshin.home_risk_check.user.entity.User;
import jakarta.annotation.Nonnull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    //게시글 단일 조회
    @Nonnull
    @EntityGraph(attributePaths = {"user", "user.profileImageFile"})
    Optional<Post> findById(@Nonnull Long postId);

    //게시글 조건 조회_최신순
    @EntityGraph(attributePaths = {"user", "user.profileImageFile"})
    @Query("""
               select post
               from Post post
               where (:postCategory is null
                      or post.postCategory = :postCategory)
               and (:keyword is null
                    or :keyword = ''
                    or lower(post.title) like lower(concat('%', :keyword, '%'))
                    or lower(post.content) like lower(concat('%', :keyword, '%')))
               order by post.createdAt desc
           """)
    Slice<Post> findAllOrderByCreatedAtDesc(@Param("postCategory") PostCategory postCategory,
                                           @Param("keyword") String keyword,
                                           Pageable pageable
    );

    //게시글 조건 조회_인기순
    @EntityGraph(attributePaths = {"user", "user.profileImageFile"})
    @Query("""
               select post
               from Post post
               where (:postCategory is null
                      or post.postCategory = :postCategory)
               and (:keyword is null
                    or :keyword = ''
                    or lower(post.title) like lower(concat('%', :keyword, '%'))
                    or lower(post.content) like lower(concat('%', :keyword, '%')))
               order by (
                   select count(postLike.id)
                   from PostLike postLike
                   where postLike.post = post
               ) desc, post.createdAt desc
           """)
    Slice<Post> findAllOrderByLikeCountDesc(@Param("postCategory") PostCategory postCategory,
                                        @Param("keyword") String keyword,
                                        Pageable pageable
    );

    //사용자별 게시글 조회
    Slice<Post> findAllByUser(User user, Pageable pageable);
}