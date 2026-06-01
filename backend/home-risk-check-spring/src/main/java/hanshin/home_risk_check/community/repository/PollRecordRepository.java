package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.PollOption;
import hanshin.home_risk_check.community.entity.PollRecord;
import hanshin.home_risk_check.community.entity.PostPoll;
import hanshin.home_risk_check.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PollRecordRepository extends JpaRepository<PollRecord, Long> {

    //사용자가 투표에서 선택한 옵션 조회
    @Query("""
               select pollRecord.pollOption.id
               from PollRecord pollRecord
               where pollRecord.postPoll = :postPoll
               and pollRecord.user = :user
           """)
    List<Long> findAllPollOptionIdsByPostPollAndUser(@Param("postPoll") PostPoll postPoll, @Param("user") User user);

    boolean existsByPostPollAndUser(PostPoll postPoll, User user);

    //옵션별 투표 수 조회
    @Query("""
              select pollRecord.pollOption.id as pollOptionId, count(pollRecord.id) as pollOptionCount
              from PollRecord pollRecord
              where pollRecord.pollOption in :pollOptions
              group by pollRecord.pollOption
           """)
    List<PollOptionCount> countPollOptions(@Param("pollOptions") List<PollOption> pollOptions);

    void deleteAllByPostPoll(PostPoll postPoll);

    interface PollOptionCount {
        Long getPollOptionId();
        Long getPollOptionCount();
    }
}