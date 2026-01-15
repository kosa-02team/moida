package back.exception;

import back.exception.response.ErrorCode;
import back.exception.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(back.exception.ResourceException.class)
    public ResponseEntity<ErrorResponse> handleResourceException(final back.exception.ResourceException e) {
        log.warn("ResourceException : {}", e.getMessage());

        final ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.error(errorCode));
    }

    @ExceptionHandler(back.exception.CustomGlobalException.class)
    public ResponseEntity<ErrorResponse> handleCustomGlobalException(final back.exception.CustomGlobalException e) {
        log.warn(e.getMessage(), e);

        final ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.error(errorCode));
    }

    @ExceptionHandler(back.exception.VoteException.class)
    public ResponseEntity<ErrorResponse> handleVoteException(final back.exception.VoteException e) {
        log.warn("VoteException : {}", e.getMessage());

        final ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.error(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(final Exception e) {
        log.error("Unhandled Exception : {}", e.getMessage(), e);

        final ErrorCode errorCode = ErrorCode.SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.error(errorCode));
    }

    @ExceptionHandler(ClubAuthException.class)
    public ResponseEntity<ErrorResponse> handleClubAuthException(final ClubAuthException e) {
        log.warn("ClubAuthException : {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.error(e.getErrorCode()));
    }



    @ExceptionHandler(PostsException.class)
    public ResponseEntity<ErrorResponse> handlePostException(final PostsException e) {
        log.warn("PostException : {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.error(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException e) {
        log.warn("MethodArgumentNotValidException occurred: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.error(ErrorCode.INVALID_INPUT));
    }
}
