package hanshin.home_risk_check.community.service;

import hanshin.home_risk_check.community.dto.PostPollRequest;
import hanshin.home_risk_check.community.entity.PollOption;
import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.entity.PostPoll;
import hanshin.home_risk_check.community.repository.PostPollRepository;
import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import hanshin.home_risk_check.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostPollService {

    private final PostPollRepository postPollRepository;
    private final PollOptionService pollOptionService;
    private final PollRecordService pollRecordService;

    //투표 생성
    @Transactional
    public PostPoll createPostPoll(Post post, PostPollRequest req){
        if (req == null) {
            return null;
        }
        PostPoll postPoll = PostPoll.builder()
                                    .allowMultiple(req.allowMultiple())
                                    .post(post)
                                    .build();
        PostPoll saved = postPollRepository.save(postPoll);
        pollOptionService.createAll(saved, req.optionNames());
        return saved;
    }

    //투표 조회
    public PostPoll getPostPoll(Post post){
        return postPollRepository.findByPost(post).orElse(null);
    }

    //투표 옵션 조회
    public List<PollOption> getPollOptions(PostPoll postPoll){
        return pollOptionService.getPollOptions(postPoll);
    }

    //옵션별 득표수 조회
    public Map<Long, Long> getPollOptionCounts(List<PollOption> pollOptions){
        return pollRecordService.getPollOptionCounts(pollOptions);
    }

    public long getTotalVoteCount(Map<Long, Long> pollOptionCounts){
        return pollOptionCounts.values()
                               .stream()
                               .mapToLong(Long::longValue)
                               .sum();
    }

    //투표하기
    @Transactional
    public void vote(Post post, User user, List<Long> pollOptionIds) {
        if (CollectionUtils.isEmpty(pollOptionIds)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PostPoll postPoll = postPollRepository.findByPost(post)
                                              .orElseThrow(() -> new BusinessException(ErrorCode.POST_POLL_NOT_FOUND));
        List<Long> distinctPollOptionIds = pollOptionIds.stream()
                                                        .distinct()
                                                        .toList();

        if (!postPoll.isAllowMultiple() && distinctPollOptionIds.size() > 1) {
            throw new BusinessException(ErrorCode.POLL_MULTIPLE_NOT_ALLOWED);
        }

        List<PollOption> pollOptions = pollOptionService.getOptions(distinctPollOptionIds);
        pollRecordService.validateNotVoted(postPoll, user);
        pollOptionService.validateBelongsToPoll(postPoll, pollOptions);
        pollRecordService.createAll(postPoll, pollOptions, user);
    }

    //현재 사용자가 선택한 옵션id들
    public List<Long> getSelectedPollOptionIds(PostPoll postPoll, User user){
        return pollRecordService.getSelectedPollOptionIds(postPoll, user);
    }

    //투표 삭제
    @Transactional
    public void delete(PostPoll postPoll){
        if (postPoll == null) {
            return;
        }
        pollRecordService.deleteAll(postPoll);
        pollOptionService.deleteAll(postPoll);
        postPollRepository.delete(postPoll);
    }
}