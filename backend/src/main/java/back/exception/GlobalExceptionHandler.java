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

    @ExceptionHandler(back.exception.CustomGlobalException.class)
    public ResponseEntity<ErrorResponse<Void>> handleCustomGlobalException(final back.exception.CustomGlobalException e) {
        log.warn("CustomGlobalException : {}", e.getMessage());

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



}
