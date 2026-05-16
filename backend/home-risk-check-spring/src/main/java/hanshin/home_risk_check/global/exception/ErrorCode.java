package hanshin.home_risk_check.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/*
 * 애플리케이션 전역 에러 코드 정의
 */
@Getter
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, 400, "입력값이 올바르지 않습니다."),
    FORBIDDEN_REQUEST(HttpStatus.FORBIDDEN, 403, "권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "서버 내부 오류가 발생했습니다."),

    // 인증/인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 401, "인증이 필요합니다."),
    PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, 401, "현재 비밀번호가 일치하지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 401, "이메일 또는 비밀번호가 올바르지 않습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, 401, "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 401, "유효하지 않은 토큰입니다."),

    // 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, 409, "이미 가입된 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, 409, "이미 사용 중인 닉네임입니다."),

    // 게시글/댓글
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "댓글을 찾을 수 없습니다."),
    INVALID_COMMENT_POST(HttpStatus.BAD_REQUEST, 400, "해당 게시글의 댓글이 아닙니다."),
    INVALID_COMMENT_DEPTH(HttpStatus.BAD_REQUEST, 400, "대댓글에는 답글을 작성할 수 없습니다."),

    // 게시글 이미지
    POST_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "게시글 이미지를 찾을 수 없습니다."),
    INVALID_POST_IMAGE(HttpStatus.BAD_REQUEST, 400, "해당 게시글의 이미지가 아닙니다."),
    TOO_MANY_IMAGES(HttpStatus.BAD_REQUEST, 400, "게시글 이미지는 최대 10장까지 업로드할 수 있습니다."),
    EMPTY_IMAGE_REQUEST(HttpStatus.BAD_REQUEST, 400, "업로드할 이미지가 없습니다."),
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, 400, "지원하지 않는 이미지 형식입니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 500, "파일 업로드에 실패했습니다."),

    // 외부 연동
    EXTERNAL_API_FAILED(HttpStatus.SERVICE_UNAVAILABLE, 503, "외부 API 호출에 실패했습니다."),

    // 데이터 동기화
    DATA_SYNC_IN_PROGRESS(HttpStatus.SERVICE_UNAVAILABLE, 503, "시스템 안전 점수 데이터를 준비 중입니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
