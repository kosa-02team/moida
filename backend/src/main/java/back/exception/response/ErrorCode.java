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
    USER_NOT_FOUND(NOT_FOUND, "A02", "존재하지 않는 사용자입니다."),

    //Club Authorization Error
    CLUB_NOT_ACTIVE(UNAUTHORIZED,"CA01","클럽에 접근할 수 없습니다."),
    CLUB_STAFF_REQUIRED(FORBIDDEN, "CA02","운영진 권한이 필요합니다"),

    //Post Error
    POST_NOT_FOUND(NOT_FOUND,"P01", "게시글을 찾을 수 없습니다"),
    POST_DELETED(GONE, "P02", "삭제된 게시글입니다"),
    POST_FORBIDDEN(FORBIDDEN,"P03", "비공개 또는 제한된 게시글입니다.")
    ;
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}