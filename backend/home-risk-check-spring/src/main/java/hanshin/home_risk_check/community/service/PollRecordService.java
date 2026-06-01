package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.entity.PollOption;
import hanshin.home_risk_check.community.entity.PollRecord;
import hanshin.home_risk_check.community.entity.PostPoll;
import hanshin.home_risk_check.community.repository.PollRecordRepository;
import hanshin.home_risk_check.community.repository.PollRecordRepository.PollOptionCount;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import hanshin.home_risk_check.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PollRecordService {

    private final PollRecordRepository pollRecordRepository;

    public Map<Long, Long> getPollOptionCounts(List<PollOption> pollOptions) {
        if (pollOptions.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> pollOptionCounts = pollRecordRepository.countPollOptions(pollOptions)
                                                               .stream()
                                                               .collect(Collectors.toMap(PollOptionCount::getPollOptionId, PollOptionCount::getPollOptionCount));
        pollOptions.forEach(option -> pollOptionCounts.putIfAbsent(option.getId(), 0L));
        return pollOptionCounts;
    }

    public List<Long> getSelectedPollOptionIds(PostPoll postPoll, User user) {
        if (postPoll == null) {
            return List.of();
        }
        return pollRecordRepository.findAllPollOptionIdsByPostPollAndUser(postPoll, user);
    }

    public void validateNotVoted(PostPoll postPoll, User user) {
        if (pollRecordRepository.existsByPostPollAndUser(postPoll, user)) {
            throw new BusinessException(ErrorCode.POLL_ALREADY_VOTED);
        }
    }

    @Transactional
    public List<PollRecord> createAll(PostPoll postPoll, List<PollOption> pollOptions, User user) {
        List<PollRecord> pollRecords = pollOptions.stream()
                                                  .map(pollOption -> PollRecord.builder()
                                                          .postPoll(postPoll)
                                                          .pollOption(pollOption)
                                                          .user(user)
                                                          .build())
                                                  .toList();
        return pollRecordRepository.saveAll(pollRecords);
    }

    @Transactional
    public void deleteAll(PostPoll postPoll) {
        if (postPoll == null) {
            return;
        }
        pollRecordRepository.deleteAllByPostPoll(postPoll);
    }
}