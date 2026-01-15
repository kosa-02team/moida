package back.exception.response;

public record ErrorResponse (
        int status,
        String code,
        String message
){

    public static ErrorResponse error(final back.exception.response.ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getHttpStatus().value(), errorCode.getCode(), errorCode.getMessage());
    }
}
