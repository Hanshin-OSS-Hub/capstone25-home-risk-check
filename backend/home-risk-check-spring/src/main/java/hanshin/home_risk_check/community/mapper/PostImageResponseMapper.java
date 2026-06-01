package hanshin.home_risk_check.community.mapper;

import hanshin.home_risk_check.community.dto.PostImageResponse;
import hanshin.home_risk_check.community.entity.PostImage;
import hanshin.home_risk_check.file.util.FileUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PostImageResponseMapper {

    private final FileUrlResolver fileUrlResolver;

    public List<PostImageResponse> toPostImageResponses(List<PostImage> postImages) {
        return postImages.stream()
                         .map(this::toPostImageResponse)
                         .toList();
    }

    public PostImageResponse toPostImageResponse(PostImage postImage) {
        return PostImageResponse.builder()
                                .id(postImage.getId())
                                .imageUrl(fileUrlResolver.toUrl(postImage.getImageFile()))
                                .build();
    }
}