package hanshin.home_risk_check.community.mapper;

import hanshin.home_risk_check.community.dto.PollOptionResponse;
import hanshin.home_risk_check.community.entity.PollOption;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class PollOptionResponseMapper {

    public List<PollOptionResponse> toPollOptionResponses(
            List<PollOption> pollOptions,
            Map<Long, Long> voteCountPerOption,
            List<Long> mySelectedOptionIds
    ) {

        return pollOptions.stream()
                          .map(pollOption ->
                                  PollOptionResponse.builder()
                                                    .id(pollOption.getId())
                                                    .optionName(pollOption.getOptionName())
                                                    .voteCount(voteCountPerOption.getOrDefault(pollOption.getId(), 0L))
                                                    .isSelectedByMe(mySelectedOptionIds.contains(pollOption.getId()))
                                                    .build())
                          .toList();
    }
}