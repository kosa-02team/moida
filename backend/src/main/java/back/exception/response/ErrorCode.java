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

    //Vote Error
    VOTE_NOT_FOUND(NOT_FOUND,"V01", "투표를 찾을 수 없습니다"),
    VOTE_SCHEDULE_ID_REQUIRED(HttpStatus.BAD_REQUEST,"V02", "참석/불참 투표는 일정 ID가 필수입니다"),
    VOTE_CLUB_MISMATCH(HttpStatus.BAD_REQUEST,"V03", "해당 모임의 투표가 아닙니다"),
    VOTE_ALREADY_CLOSED(HttpStatus.BAD_REQUEST,"V04", "이미 종료된 투표입니다"),
    VOTE_DEADLINE_PASSED(HttpStatus.BAD_REQUEST,"V05", "투표 기한이 지났습니다"),
    VOTE_SCHEDULE_ID_MISSING(HttpStatus.BAD_REQUEST,"V06", "일정 투표는 scheduleId가 필수입니다"),
    VOTE_OPTION_REQUIRED(HttpStatus.BAD_REQUEST,"V07", "최소 하나의 옵션을 선택해야 합니다"),
    VOTE_ATTENDANCE_SINGLE_OPTION(HttpStatus.BAD_REQUEST,"V08", "참석/불참 투표는 하나의 옵션만 선택할 수 있습니다"),
    VOTE_OPTION_DUPLICATE(HttpStatus.BAD_REQUEST,"V09", "중복된 옵션 ID가 포함되어 있습니다"),
    VOTE_OPTION_INVALID(HttpStatus.BAD_REQUEST,"V10", "유효하지 않은 옵션 ID가 포함되어 있습니다"),
    VOTE_MULTIPLE_NOT_ALLOWED(HttpStatus.BAD_REQUEST,"V11", "복수 선택이 허용되지 않는 투표입니다"),
    VOTE_ALREADY_PARTICIPATED(HttpStatus.CONFLICT,"V12", "이미 투표에 참여했습니다"),
    VOTE_OPTION_ALREADY_SELECTED(HttpStatus.CONFLICT,"V13", "이미 선택한 옵션입니다"),
    VOTE_CREATOR_ONLY(HttpStatus.FORBIDDEN,"V14", "일반 투표는 생성자만 종료할 수 있습니다"),
    VOTE_MEMBER_ONLY(HttpStatus.FORBIDDEN,"V15", "해당 모임의 멤버만 투표에 참여할 수 있습니다"),
    VOTE_STAFF_ONLY(HttpStatus.FORBIDDEN,"V16", "ATTENDANCE 투표 종료는 모임장 또는 운영진만 가능합니다"),

    //Post Error
    POST_NOT_FOUND(NOT_FOUND,"P01", "게시글을 찾을 수 없습니다"),
    POST_DELETED(GONE, "P02", "삭제된 게시글입니다"),
    POST_FORBIDDEN(FORBIDDEN,"P03", "비공개 또는 제한된 게시글입니다"),

    //Schedule Error
    SCHEDULE_NOT_FOUND(NOT_FOUND,"SC01", "일정을 찾을 수 없습니다"),
    SCHEDULE_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST,"SC02", "종료일시는 시작일시보다 이후여야 합니다"),
    SCHEDULE_STAFF_ONLY(HttpStatus.FORBIDDEN,"SC03", "일정 종료는 모임장 또는 운영진만 가능합니다")
    ;
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}