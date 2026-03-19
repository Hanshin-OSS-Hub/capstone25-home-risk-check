package hanshin.home_risk_check.community.dto;

import lombok.Getter;

/*
 * 댓글 작성 요청(Request) DTO
 *
 * 클라이언트가 댓글 작성 API를 호출할 때
 * 요청 body 데이터를 담는 객체
 *
 * 예 요청 JSON
 *
 * {
 *   "content": "이 매물 위험한 것 같아요",
 *   "parentCommentId": null
 * }
 *
 * 또는 (대댓글)
 *
 * {
 *   "content": "저도 그렇게 생각합니다",
 *   "parentCommentId": 3
 * }
 */
@Getter
public class CommentCreateRequest {

    /*
     * 댓글 내용
     */
    private String content;

    /*
     * 부모 댓글 ID
     *
     * null → 일반 댓글
     * 값 존재 → 대댓글
     *
     * 예:
     * parentCommentId = 5
     * → 5번 댓글의 대댓글
     */
    private Long parentCommentId;
}