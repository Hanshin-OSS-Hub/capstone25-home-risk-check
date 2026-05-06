package hanshin.home_risk_check.community.dto;

import lombok.Getter;

/*
 * 게시글 작성 요청(Request) DTO
 *
 * 클라이언트(프론트)에서 게시글 작성 API를 호출할 때
 * 요청 body 데이터를 담는 객체
 *
 * 예:
 * {
 *   "categoryLabel": "서울시 성동구",
 *   "title": "이 매물 괜찮나요?",
 *   "content": "등기부등본을 보니 조금 이상합니다."
 * }
 */
@Getter  // Lombok: 각 필드의 getter 자동 생성
public class PostCreateRequest {

    /*
     * 게시글 카테고리
     * 예: "서울시 성동구"
     */
    private String categoryLabel;

    /*
     * 게시글 제목
     */
    private String title;

    /*
     * 게시글 본문 내용
     */
    private String content;
}