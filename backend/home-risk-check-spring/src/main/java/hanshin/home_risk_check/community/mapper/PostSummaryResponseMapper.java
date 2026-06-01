package hanshin.home_risk_check.community.mapper;

import hanshin.home_risk_check.community.dto.PostSummaryResponse;
import hanshin.home_risk_check.community.entity.Post;
import hanshin.home_risk_check.community.entity.PostImage;
import hanshin.home_risk_check.file.util.FileUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PostSummaryResponseMapper {

    private final AuthorResponseMapper authorResponseMapper;
    private final FileUrlResolver fileUrlResolver;

    public Slice<PostSummaryResponse> toPostSummaryResponses(
            Slice<Post> posts,
            Map<Long, PostImage> thumbnails,
            Map<Long, Long> postLikeCounts,
            Map<Long, Long> commentCounts
    ) {
        return posts.map(post ->
                PostSummaryResponse.builder()
                                   .id(post.getId())
                                   .author(authorResponseMapper.toAuthorResponse(post.getUser()))
                                   .postCategory(post.getPostCategory())
                                   .title(post.getTitle())
                                   .content(post.getContent())
                                   //TODO : 썸네일 이미지가 없는 경우 Null 처리
                                   .thumbnailUrl(getThumbnailUrl(post, thumbnails))
                                   .likeCount(postLikeCounts.getOrDefault(post.getId(), 0L))
                                   .commentCount(commentCounts.getOrDefault(post.getId(), 0L))
                                   .createdAt(post.getCreatedAt())
                                   .updatedAt(post.getUpdatedAt())
                                   .build()
        );
    }

    private String getThumbnailUrl(Post post, Map<Long, PostImage> thumbnails) {
        PostImage thumbnail = thumbnails.get(post.getId());
        if(thumbnail == null){
            return null;
        }
        return fileUrlResolver.toUrl(thumbnail.getImageFile());
    }
}