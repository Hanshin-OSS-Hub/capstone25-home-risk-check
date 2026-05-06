package hanshin.home_risk_check.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/*
 * 댓글 작성 요청 DTO
 */
@Getter
public class CommentCreateRequest {

    /*
     * 댓글 내용
     *
     * 공백 문자열이나 빈 문자열 방지
     */
    @NotBlank(message = "댓글 내용은 비어 있을 수 없습니다.")
    private String content;

    /*
     * 부모 댓글 ID
     *
     * null  -> 일반 댓글
     * 값 존재 -> 대댓글
     */
    private Long parentCommentId;
}