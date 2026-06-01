package hanshin.home_risk_check.community.util;

import hanshin.home_risk_check.global.exception.BusinessException;
import hanshin.home_risk_check.global.exception.ErrorCode;
import hanshin.home_risk_check.user.entity.User;

public final class AuthorValidator {

    private AuthorValidator() {}

    public static void validate(User author, User requester) {
        if (!author.getId().equals(requester.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_REQUEST);
        }
    }
}