package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    //게시글별 이미지 조회
    List<PostImage> findAllByPost_IdOrderByImageOrderAsc(Long postId);

    //썸네일 이미지 조회
    List<PostImage> findAllByPost_IdInAndImageOrder(List<Long> postIds, int imageOrder);

    long countByPost_Id(Long postId);
}