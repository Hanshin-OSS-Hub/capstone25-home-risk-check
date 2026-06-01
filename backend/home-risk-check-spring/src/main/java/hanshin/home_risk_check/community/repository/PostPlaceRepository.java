package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.entity.PostPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostPlaceRepository extends JpaRepository<PostPlace, Long> {

    //게시글에 포함된 장소 조회
    Optional<PostPlace> findByPost(Post post);
}