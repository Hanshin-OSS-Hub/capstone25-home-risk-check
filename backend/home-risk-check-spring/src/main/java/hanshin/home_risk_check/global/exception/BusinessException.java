package hanshin.home_risk_check.global.exception;

import lombok.Getter;

/*
 * 비즈니스 로직에서 사용하는 커스텀 예외 클래스
 *
 * 서비스 계층에서 발생하는 예외를
 * ErrorCode와 함께 던지기 위해 사용한다.
 *
 * 예:
 * 게시글이 존재하지 않을 때
 * → throw new BusinessException(ErrorCode.POST_NOT_FOUND)
 */
@Getter
public class BusinessException extends RuntimeException {

    /*
     * 어떤 에러인지 정보를 담는 객체
     * ErrorCode enum과 연결됨
     */
    private final ErrorCode errorCode;

    /*
     * 생성자
     *
     * ErrorCode를 전달받아 예외 생성
     */
    public BusinessException(ErrorCode errorCode) {

        /*
         * RuntimeException의 메시지를
         * ErrorCode의 message로 설정
         *
         * 예:
         * "게시글을 찾을 수 없습니다."
         */
        super(errorCode.getMessage());

        /*
         * 전달받은 ErrorCode 저장
         */
        this.errorCode = errorCode;
    }
}