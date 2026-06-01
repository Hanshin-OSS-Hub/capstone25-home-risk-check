package hanshin.home_risk_check.community.mapper;

import hanshin.home_risk_check.community.dto.PostDetailResponse;
import hanshin.home_risk_check.community.entity.PollOption;
import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.entity.PostImage;
import hanshin.home_risk_check.community.entity.PostPlace;
import hanshin.home_risk_check.community.entity.PostPoll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PostDetailResponseMapper {

    private final AuthorResponseMapper authorResponseMapper;
    private final PostImageResponseMapper postImageResponseMapper;
    private final PostPollResponseMapper postPollResponseMapper;
    private final PollOptionResponseMapper pollOptionResponseMapper;
    private final PostPlaceResponseMapper postPlaceResponseMapper;

    public PostDetailResponse toPostDetailResponse(
            Post post,
            PostPlace postPlace,
            PostPoll postPoll,
            List<PollOption> pollOptions,
            List<PostImage> postImages,
            Map<Long, Long> voteCountPerOption,
            List<Long> mySelectedOptionIds,
            boolean isWrittenByMe,
            boolean isLikedByMe,
            long likeCount,
            long commentCount,
            long totalVoteCount
    ) {
        return PostDetailResponse.builder()
                .id(post.getId())
                .author(authorResponseMapper.toAuthorResponse(post.getUser()))
                .postCategory(post.getPostCategory())
                .title(post.getTitle())
                .content(post.getContent())
                .place(postPlaceResponseMapper.toPostPlaceResponse(postPlace))
                .poll(postPollResponseMapper.toPostPollResponse(postPoll, totalVoteCount))
                .pollOptions(pollOptionResponseMapper.toPollOptionResponses(pollOptions, voteCountPerOption, mySelectedOptionIds))
                .images(postImageResponseMapper.toPostImageResponses(postImages))
                .likeCount(likeCount)
                .commentCount(commentCount)
                .isWrittenByMe(isWrittenByMe)
                .isLikedByMe(isLikedByMe)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}