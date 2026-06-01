package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.entity.PollOption;
import hanshin.home_risk_check.community.entity.PostPoll;
import hanshin.home_risk_check.community.repository.PollOptionRepository;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PollOptionService {

    private final PollOptionRepository pollOptionRepository;

    @Transactional
    public List<PollOption> createAll(PostPoll postPoll, List<String> optionNames) {
        List<PollOption> pollOptions = optionNames.stream()
                                                  .map(optionName -> PollOption.builder()
                                                          .optionName(optionName)
                                                          .postPoll(postPoll)
                                                          .build())
                                                  .toList();
        return pollOptionRepository.saveAll(pollOptions);
    }

    public List<PollOption> getPollOptions(PostPoll postPoll) {
        if (postPoll == null) {
            return List.of();
        }
        return pollOptionRepository.findAllByPostPollOrderById(postPoll);
    }

    public List<PollOption> getOptions(List<Long> pollOptionIds) {
        if (CollectionUtils.isEmpty(pollOptionIds)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<PollOption> pollOptions = pollOptionRepository.findAllById(pollOptionIds);
        if (pollOptions.size() != pollOptionIds.size()) {
            throw new BusinessException(ErrorCode.POLL_OPTION_NOT_FOUND);
        }
        return pollOptions;
    }

    public void validateBelongsToPoll(PostPoll postPoll, List<PollOption> pollOptions) {
        boolean hasInvalidOption = pollOptions.stream()
                                              .anyMatch(pollOption -> !pollOption.getPostPoll().getId().equals(postPoll.getId()));
        if (hasInvalidOption) {
            throw new BusinessException(ErrorCode.INVALID_POLL_OPTION);
        }
    }

    @Transactional
    public void deleteAll(PostPoll postPoll) {
        if (postPoll == null) {
            return;
        }
        pollOptionRepository.deleteAllByPostPoll(postPoll);
    }
}
