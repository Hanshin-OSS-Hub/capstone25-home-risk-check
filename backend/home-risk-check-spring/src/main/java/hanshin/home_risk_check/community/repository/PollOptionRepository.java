package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.PollOption;
import hanshin.home_risk_check.community.entity.PostPoll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    //옵션 조회
    List<PollOption> findAllByPostPollOrderById(PostPoll postPoll);

    //옵션 삭제
    void deleteAllByPostPoll(PostPoll postPoll);
}