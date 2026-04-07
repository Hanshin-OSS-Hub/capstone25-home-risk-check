package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
 * 게시글 이미지 Repository
 */
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    /*
     * 특정 게시글에 연결된 이미지 목록 조회
     */
    List<PostImage> findAllByPost_PostIdOrderByImageOrderAsc(Long postId);

    /*
     * 특정 게시글의 이미지 개수 조회
     */
    long countByPost_PostId(Long postId);
}