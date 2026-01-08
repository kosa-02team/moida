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
}
