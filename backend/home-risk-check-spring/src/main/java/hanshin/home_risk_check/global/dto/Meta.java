package hanshin.home_risk_check.global.dto;

import lombok.Getter;

/*
 * API 공통 응답에서 meta 정보를 담는 DTO
 *
 * 응답 구조 예:
 *
 * {
 *   "meta": {
 *      "code": 200,
 *      "message": "success"
 *   },
 *   "data": {...}
 * }
 *
 * 여기서 meta 부분을 담당하는 객체
 */
@Getter  // Lombok: 모든 필드에 대한 getter 자동 생성
public class Meta {

    /*
     * 응답 상태 코드
     *
     * 예:
     * 200 → 성공
     * 400 → 잘못된 요청
     * 404 → 데이터 없음
     * 500 → 서버 오류
     */
    private final int code;

    /*
     * 응답 메시지
     *
     * 예:
     * "success"
     * "post not found"
     * "invalid request"
     */
    private final String message;

    /*
     * 생성자
     *
     * 응답 생성 시 code와 message를 설정
     *
     * 예:
     * new Meta(200, "success")
     */
    public Meta(int code, String message) {
        this.code = code;
        this.message = message;
    }
}