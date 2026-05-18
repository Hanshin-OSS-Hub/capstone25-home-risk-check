package hanshin.home_risk_check.community.repository;

import hanshin.home_risk_check.community.entity.PollRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PollRecordRepository extends JpaRepository<PollRecord, Long> {

    //투표 참여 여부 확인
    boolean existsByPostPoll_IdAndUser_Id(Long postPollId, Long userId);

    //사용자가 특정 투표에서 선택한 옵션 조회
    @Query("""
               select pollRecord.pollOption.id
               from PollRecord pollRecord
               where pollRecord.postPoll.id = :postPollId
               and pollRecord.user.id = :userId
           """)
    List<Long> findAllPollOptionIdsByPostPollIdAndUserId(@Param("postPollId") Long postPollId, @Param("userId") Long userId);

    //옵션별 투표 수 조회
    @Query("""
              select pollOption.id as optionId, count(pollRecord) as count
              from PollOption pollOption
              left join PollRecord pollRecord
              on pollRecord.pollOption.id = pollOption.id
              where pollOption.postPoll.id = :postPollId
              group by pollRecord.pollOption.id
          """)
    List<OptionSelectedCount> countPollOptionsByPostPollIdGroupByPollOptionId(@Param("postPollId") Long postPollId);

    interface OptionSelectedCount{
        Long getOptionId();
        Long getCount();
    }
}