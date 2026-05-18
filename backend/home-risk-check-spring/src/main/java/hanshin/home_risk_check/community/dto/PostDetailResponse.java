package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.PostCategory;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
    Long id,
    AuthorResponse author,
    PostCategory postCategory,
    String title,
    String content,
    List<PostImageResponse> images,
    long likeCount,
    long commentCount,
    boolean isLikedByMe,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    PostPlaceResponse place,
    PostPollResponse poll
){
    public record PostImageResponse(
        Long id,
        String imageUrl,
        int imageOrder
    ){}

    public record PostPlaceResponse(
        Long id,
        Double latitude,
        Double longitude,
        String address,
        String placeName
    ){}

    public record PostPollResponse(
        Long id,
        boolean allowMultiple,
        List<PollOptionResponse> options,
        List<Long> mySelectedOptions,
        long totalVotes
    ){
        public record PollOptionResponse(
            Long id,
            String optionName,
            int pollOrder,
            long voteCount
        ){}
    }
}