package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.PostPoll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostPollRepository extends JpaRepository<PostPoll, Long> {

    //게시글에 포함된 투표 조회
    Optional<PostPoll> findByPost_Id(Long postId);
}