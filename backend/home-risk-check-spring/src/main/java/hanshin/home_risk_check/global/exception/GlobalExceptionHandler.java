package hanshin.home_risk_check.global.exception;

import hanshin.home_risk_check.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
 * 전역 예외 처리 클래스
 *
 * 애플리케이션 전체에서 발생하는 예외를
 * 여기서 한 번에 처리한다.
 *
 * Service에서 발생한 예외 → 여기로 전달됨
 * → ApiResponse 형태로 변환해서 반환
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
     * BusinessException 처리
     *
     * Service에서 throw new BusinessException(...) 했을 때
     * 이 메서드가 실행된다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {

        /*
         * 어떤 에러인지 가져옴
         */
        ErrorCode errorCode = e.getErrorCode();

        /*
         * HTTP 상태 코드 + API 응답 생성
         *
         * 예:
         * POST_NOT_FOUND → 404
         */
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    /*
     * 모든 예외 처리 (최종 안전망)
     *
     * 예상하지 못한 에러 발생 시 처리
     *
     * 예:
     * NullPointerException
     * DB 오류
     * 서버 내부 오류
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {

        return ResponseEntity
                .internalServerError() // HTTP 500
                .body(ApiResponse.error(500, "internal server error"));
    }
}