package hanshin.home_risk_check.community.dto;

import lombok.Getter;

/*
 * 게시글 수정 요청(Request) DTO
 *
 * 클라이언트가 게시글 수정 API를 호출할 때
 * 수정할 데이터를 담는 객체
 *
 * 예 요청 JSON
 *
 * {
 *   "categoryLabel": "서울시 성동구",
 *   "title": "수정된 제목입니다",
 *   "content": "내용을 수정했습니다"
 * }
 */
@Getter
public class PostUpdateRequest {

    /*
     * 수정할 카테고리 라벨
     *
     * 예:
     * "서울시 성동구"
     */
    private String categoryLabel;

    /*
     * 수정할 게시글 제목
     */
    private String title;

    /*
     * 수정할 게시글 내용
     */
    private String content;
}