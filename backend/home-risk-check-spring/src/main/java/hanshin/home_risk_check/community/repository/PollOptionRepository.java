package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    //투표의 모든 옵션 조회
    List<PollOption> findAllByPostPoll_IdOrderByPollOrderAsc(Long postPollId);
}