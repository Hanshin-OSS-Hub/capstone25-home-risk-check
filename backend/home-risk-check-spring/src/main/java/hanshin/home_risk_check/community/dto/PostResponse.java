package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PostResponse {

    private Long postId;
    private Long authorId;
    private String categoryLabel;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .postId(post.getPostId())
                .authorId(post.getAuthorId())
                .categoryLabel(post.getCategoryLabel())
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}