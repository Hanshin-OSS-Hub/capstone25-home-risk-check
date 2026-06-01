package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.PostCategory;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PostDetailResponse(
    Long id,
    AuthorResponse author,
    PostCategory postCategory,
    String title,
    String content,
    PostPlaceResponse place,
    PostPollResponse poll,
    List<PollOptionResponse> pollOptions,
    List<PostImageResponse> images,
    long likeCount,
    long commentCount,
    boolean isWrittenByMe,
    boolean isLikedByMe,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}