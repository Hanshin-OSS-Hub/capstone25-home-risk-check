package hanshin.home_risk_check.community.mapper;

import hanshin.home_risk_check.community.dto.PostLikeResponse;
import hanshin.home_risk_check.community.entity.Post;
import org.springframework.stereotype.Component;

@Component
public class PostLikeResponseMapper {

    public PostLikeResponse toPostLikeResponse(Post post, boolean liked, long likeCount) {
        return PostLikeResponse.builder()
                               .postId(post.getId())
                               .liked(liked)
                               .likeCount(likeCount)
                               .build();
    }
}
