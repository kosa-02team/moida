package back.exception;

import back.exception.response.ErrorCode;
import lombok.Getter;

@Getter
public abstract class CustomGlobalException extends RuntimeException {

    private final ErrorCode errorCode;

    protected CustomGlobalException(final ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected CustomGlobalException(final ErrorCode errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }
}