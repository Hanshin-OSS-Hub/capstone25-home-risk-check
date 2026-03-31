package hanshin.home_risk_check.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/*
 * 애플리케이션 전역 에러 코드 정의
 */
@Getter
public enum ErrorCode {

    /*
     * 게시글 없음
     */
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "게시글을 찾을 수 없습니다."),

    /*
     * 댓글 없음
     */
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "댓글을 찾을 수 없습니다."),

    /*
     * 댓글이 해당 게시글 소속이 아님
     */
    INVALID_COMMENT_POST(HttpStatus.BAD_REQUEST, 400, "해당 게시글의 댓글이 아닙니다."),

    /*
     * 대댓글의 대댓글 작성 시도
     */
    INVALID_COMMENT_DEPTH(HttpStatus.BAD_REQUEST, 400, "대댓글에는 답글을 작성할 수 없습니다."),

    /*
     * 권한 없음
     */
    FORBIDDEN_REQUEST(HttpStatus.FORBIDDEN, 403, "권한이 없습니다."),

    /*
     * 입력값 검증 실패
     */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, 400, "입력값이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}