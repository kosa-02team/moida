package back.exception;

import back.exception.response.ErrorCode;

public class AuthException extends CustomGlobalException{

    protected AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected AuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static class LoginFailed extends AuthException {

        public LoginFailed() {
            super(ErrorCode.LOGIN_FAILED);
        }

        public LoginFailed(String message) {
            super(ErrorCode.LOGIN_FAILED, message);
        }
    }

    public static class UserNotFound extends AuthException {

        public UserNotFound() {
            super(ErrorCode.USER_NOT_FOUND);
        }

        public UserNotFound(String message) {
            super(ErrorCode.USER_NOT_FOUND, message);
        }
    }

    public static class LoginIdDuplicated extends AuthException {

        public LoginIdDuplicated() {
            super(ErrorCode.LOGINID_DUPLICATED);
        }

        public LoginIdDuplicated(String message) {
            super(ErrorCode.LOGINID_DUPLICATED, message);
        }
    }
}
