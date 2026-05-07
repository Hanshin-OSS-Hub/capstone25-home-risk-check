package hanshin.home_risk_check.community.dto;

import jakarta.validation.constraints.NotBlank; // [변경] 빈 문자열 검증을 위해 추가
import jakarta.validation.constraints.Size; // [변경] 길이 제한 검증을 위해 추가
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
@Getter
public class PostCreateRequest {

    /*
     * 게시글 카테고리
     * 예: "서울시 성동구"
     */
    @NotBlank(message = "카테고리는 비어 있을 수 없습니다.") // [변경]
    @Size(max = 50, message = "카테고리는 최대 50자까지 가능합니다.") // [변경]
    private String categoryLabel;

    /*
     * 게시글 제목
     */
    @NotBlank(message = "제목은 비어 있을 수 없습니다.") // [변경]
    @Size(max = 200, message = "제목은 최대 200자까지 가능합니다.") // [변경]
    private String title;

    /*
     * 게시글 본문 내용
     */
    @NotBlank(message = "내용은 비어 있을 수 없습니다.") // [변경]
    @Size(max = 10000, message = "내용은 최대 10000자까지 가능합니다.") // [변경]
    private String content;
}