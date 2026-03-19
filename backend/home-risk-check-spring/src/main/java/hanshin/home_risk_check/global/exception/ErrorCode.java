package hanshin.home_risk_check.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/*
 * 애플리케이션에서 사용하는 에러 코드 정의
 *
 * 예외가 발생했을 때
 * 어떤 HTTP 상태 코드와 메시지를 반환할지 정의하는 enum
 *
 * 예:
 * 게시글이 없을 때
 * → POST_NOT_FOUND
 *
 * 댓글이 없을 때
 * → COMMENT_NOT_FOUND
 */
@Getter
public enum ErrorCode {

    /*
     * 게시글을 찾을 수 없는 경우
     *
     * HTTP 상태: 404 NOT FOUND
     * 응답 코드: 404
     * 메시지: 게시글을 찾을 수 없습니다.
     */
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "게시글을 찾을 수 없습니다."),

    /*
     * 댓글을 찾을 수 없는 경우
     */
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "댓글을 찾을 수 없습니다."),

    /*
     * 댓글이 해당 게시글에 속하지 않는 경우
     *
     * 예:
     * postId = 1 인데
     * commentId = 5 가 다른 게시글의 댓글일 때
     */
    INVALID_COMMENT_POST(HttpStatus.BAD_REQUEST, 400, "해당 게시글의 댓글이 아닙니다."),

    /*
     * 권한이 없는 요청
     *
     * 예:
     * 다른 사람이 작성한 게시글을 수정하려 할 때
     */
    FORBIDDEN_REQUEST(HttpStatus.FORBIDDEN, 403, "권한이 없습니다.");

    /*
     * HTTP 응답 상태 코드
     * 예: 404, 400, 403
     */
    private final HttpStatus httpStatus;

    /*
     * API 응답에 사용할 내부 에러 코드
     */
    private final int code;

    /*
     * 클라이언트에게 전달할 에러 메시지
     */
    private final String message;

    /*
     * enum 생성자
     */
    ErrorCode(HttpStatus httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}