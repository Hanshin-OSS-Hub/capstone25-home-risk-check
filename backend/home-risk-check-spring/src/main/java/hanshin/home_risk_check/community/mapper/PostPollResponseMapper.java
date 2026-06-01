package hanshin.home_risk_check.community.mapper;

import hanshin.home_risk_check.community.dto.PostPollResponse;
import hanshin.home_risk_check.community.entity.PostPoll;
import org.springframework.stereotype.Component;

@Component
public class PostPollResponseMapper {

    public PostPollResponse toPostPollResponse(PostPoll postPoll, long totalVoteCount) {
        if (postPoll == null) {
            return null;
        }
        return PostPollResponse.builder()
                .id(postPoll.getId())
                .allowMultiple(postPoll.isAllowMultiple())
                .totalVotes(totalVoteCount)
                .build();
    }
}