package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.entity.PostImage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    //게시글에 포함된 이미지 조회
    @EntityGraph(attributePaths = {"post", "imageFile"})
    List<PostImage> findAllByPostOrderById(Post post);

    //썸네일 조회
    @EntityGraph(attributePaths = {"post", "imageFile"})
    @Query("""
              select postImage
              from PostImage postImage
              where postImage.id
              in (
                  select min(postImage2.id)
                  from PostImage postImage2
                  where postImage2.post in :posts
                  group by postImage2.post
                 )
           """)
    List<PostImage> findThumbnailsByPost(@Param("posts") List<Post> posts);
}