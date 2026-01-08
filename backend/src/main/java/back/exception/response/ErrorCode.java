package back.exception.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    //Server Error
    SERVER_ERROR(INTERNAL_SERVER_ERROR,"S01", "예상치 못한 서버 에러가 발생하였습니다"),

    //Global Error
    RESOURCE_NOT_FOUND(NOT_FOUND,"G01", "요청한 자원을 찾을 수 없습니다"),

    LOGIN_FAILED(UNAUTHORIZED, "A01", "아이디 또는 비밀번호가 일치하지 않습니다."),
    USER_NOT_FOUND(NOT_FOUND, "A02", "존재하지 않는 사용자입니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}