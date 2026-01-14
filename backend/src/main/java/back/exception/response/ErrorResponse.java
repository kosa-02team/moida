package back.exception.response;

import org.springframework.validation.FieldError;

import java.util.List;

public record ErrorResponse<T> (
        int status,
        String code,
        String message,
        //상세 에러 목록 (Validation)
        List<T> errors
){

    public static <T> ErrorResponse<T> error(final back.exception.response.ErrorCode errorCode) {
        return new ErrorResponse<>(errorCode.getHttpStatus().value(), errorCode.getCode(), errorCode.getMessage(), List.of());
    }

    public static <T> ErrorResponse<T> validation(final ErrorCode errorCode, final List<T> errors) {
        return new ErrorResponse<>(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                errors
        );
    }

    public record ValidationError(String field, String message) {
        public static ValidationError of(final FieldError fieldError) {
            return new ValidationError(fieldError.getField(), fieldError.getDefaultMessage());
        }
    }
}
