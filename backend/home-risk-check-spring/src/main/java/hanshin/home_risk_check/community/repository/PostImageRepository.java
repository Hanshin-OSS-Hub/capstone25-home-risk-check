package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
 * 게시글 이미지 Repository
 */
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findAllByPost_PostIdOrderByImageOrderAsc(Long postId);

    long countByPost_PostId(Long postId);
}