package back.exception;

import back.exception.response.ErrorCode;

public class AdminException extends CustomGlobalException {
    public AdminException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AdminException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
