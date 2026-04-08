package hanshin.home_risk_check.community.dto;

import hanshin.home_risk_check.community.entity.PostImage;
import lombok.Builder;
import lombok.Getter;

/*
 * 게시글 이미지 응답 DTO
 */
@Getter
@Builder
public class PostImageResponse {

    private Long postImageId;
    private String originalName;
    private String storedName;
    private String extension;
    private Long fileSize;
    private String filePath;
    private Integer imageOrder;

    public static PostImageResponse from(PostImage postImage) {
        return PostImageResponse.builder()
                .postImageId(postImage.getPostImageId())
                .originalName(postImage.getOriginalName())
                .storedName(postImage.getStoredName())
                .extension(postImage.getExtension())
                .fileSize(postImage.getFileSize())
                .filePath(postImage.getFilePath())
                .imageOrder(postImage.getImageOrder())
                .build();
    }
}