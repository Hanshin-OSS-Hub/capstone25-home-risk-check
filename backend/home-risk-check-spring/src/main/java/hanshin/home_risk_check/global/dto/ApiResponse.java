package hanshin.home_risk_check.global.dto;

import lombok.Getter;

/*
 * API 공통 응답 Wrapper 클래스
 *
 * 모든 API 응답을 다음 형식으로 통일하기 위해 사용
 *
 * {
 *   "meta": {
 *     "code": 200,
 *     "message": "success"
 *   },
 *   "data": {...}
 * }
 *
 * meta  → 응답 상태 정보
 * data  → 실제 응답 데이터
 */
@Getter
public class ApiResponse<T> {

    /*
     * 응답 상태 정보
     * code + message를 담는 객체
     */
    private final Meta meta;

    /*
     * 실제 응답 데이터
     * 제네릭(T)을 사용해서 어떤 타입이든 담을 수 있음
     *
     * 예:
     * PostResponse
     * List<PostResponse>
     * CommentResponse
     */
    private final T data;

    /*
     * 생성자 (private)
     *
     * 외부에서 직접 new ApiResponse(...)를 못 하게 막고
     * 아래 static 메서드(success, error)를 통해 생성하도록 강제
     */
    private ApiResponse(int code, String message, T data) {
        this.meta = new Meta(code, message);
        this.data = data;
    }

    /*
     * 성공 응답 생성
     *
     * 가장 기본적인 성공 응답
     *
     * 예:
     * return ApiResponse.success(postResponse);
     *
     * 결과 JSON
     *
     * {
     *   "meta": { "code": 200, "message": "success" },
     *   "data": {...}
     * }
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /*
     * 성공 응답 (code + message 커스터마이징)
     *
     * 예:
     * return ApiResponse.success(201, "post created", postResponse);
     */
    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    /*
     * 에러 응답 생성
     *
     * data는 없으므로 null
     *
     * 예:
     * return ApiResponse.error(404, "post not found");
     *
     * 결과 JSON
     *
     * {
     *   "meta": { "code": 404, "message": "post not found" },
     *   "data": null
     * }
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}