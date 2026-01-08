package back.exception;

import back.exception.response.ErrorCode;
import back.exception.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(back.exception.ResourceException.class)
    public ResponseEntity<ErrorResponse<Void>> handleResourceException(final back.exception.ResourceException e) {
        log.warn("ResourceException : {}", e.getMessage());

        final ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.error(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse<Void>> handleException(final Exception e) {
        log.error("Unhandled Exception : {}", e.getMessage(), e);

        final ErrorCode errorCode = ErrorCode.SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.error(errorCode));
    }

    @ExceptionHandler(PostException.class)
    public ResponseEntity<ErrorResponse<Void>> handlePostException(final PostException e) {
        log.warn("PostException : {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.error(e.getErrorCode()));
    }
}
