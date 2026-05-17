package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.entity.PostCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = {"user"})
    @Query("""
               select post
               from Post post
               where (:postCategory is null
                      or post.postCategory = :postCategory)
               and (:keyword is null
                    or :keyword = ''
                    or lower(post.title) like lower(concat('%', :keyword, '%'))
                    or lower(post.content) like lower(concat('%', :keyword, '%')))
           """)
    Slice<Post> findAllPosts(@Param("postCategory") PostCategory postCategory, @Param("keyword") String keyword, Pageable pageable);
}