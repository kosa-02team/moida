package back.exception;

import back.exception.CustomGlobalException;
import back.exception.response.ErrorCode;

public class ResourceException extends CustomGlobalException {

    protected ResourceException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected ResourceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static class NotFound extends ResourceException {
        public NotFound() {
            super(ErrorCode.RESOURCE_NOT_FOUND);
        }

        public NotFound(String message) {
            super(ErrorCode.RESOURCE_NOT_FOUND, message);
        }
    }
}