package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/*
 * 게시글 응답(Response) DTO
 */
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

    /*
     * 게시글 이미지 목록
     * 기존 응답 구조를 깨지 않기 위해 필드만 추가
     */
    private List<PostImageResponse> images;

    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .postId(post.getPostId())
                .authorId(post.getAuthorId())
                .categoryLabel(post.getCategoryLabel())
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .images(
                        post.getImages().stream()
                                .map(PostImageResponse::from)
                                .toList()
                )
                .build();
    }
}