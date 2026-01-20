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

    //Validation Error
    INVALID_INPUT(BAD_REQUEST, "VAL01", "입력값이 올바르지 않습니다."),

    //로그인 회원가입 관련 에러
    LOGIN_FAILED(UNAUTHORIZED, "A01", "아이디 또는 비밀번호가 일치하지 않습니다."),
    USER_NOT_FOUND(NOT_FOUND, "A02", "존재하지 않는 사용자입니다."),
    LOGINID_DUPLICATED(CONFLICT, "A03", "이미 존재하는 회원입니다."),
    REFRESH_TOKEN_NOT_FOUND(UNAUTHORIZED, "A04", "저장된 리프레시 토큰이 없습니다."),
    INVALID_REFRESH_TOKEN(UNAUTHORIZED, "A05", "유효하지 않은 리프레시 토큰입니다."),

    //Club Error
    CLUB_NOT_FOUND(HttpStatus.NOT_FOUND, "C01", "존재하지 않는 클럽입니다."),
    CLUB_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "C02", "이미 존재하는 클럽 이름입니다."),
    CLUB_CLOSED(HttpStatus.BAD_REQUEST, "C03", "이미 폐쇄된 클럽입니다."),
    CLUB_MAX_CAPACITY(BAD_REQUEST, "C04", "최대 정원이 가득 찼습니다."),
    CLUB_INVALID_CATEGORY(BAD_REQUEST, "C05", "유효하지 않은 카테고리입니다."),
    CLUB_INVALID_STATUS(BAD_REQUEST, "C06", "유효하지 않은 상태값입니다."),

    //ClubAuth Error
    CLUB_AUTH_NOT_ACTIVE(FORBIDDEN, "CA01", "활성 멤버가 아닙니다."),
    CLUB_AUTH_NOT_STAFF(FORBIDDEN, "CA02", "운영진 권한이 필요한 기능입니다."),
    CLUB_AUTH_NOT_ACCOUNTANT(FORBIDDEN, "CA03", "총무 권한이 필요한 기능입니다."),
    CLUB_AUTH_STAFF_REQUIRED(FORBIDDEN, "CA04", "운영진 이상 권한이 필요합니다."),
    CLUB_AUTH_ACCOUNTANT_REQUIRED(FORBIDDEN, "CA05", "총무 이상 권한이 필요합니다."),
    CLUB_AUTH_LOGIN_REQUIRED(UNAUTHORIZED, "CA06", "로그인이 필요합니다."),
    CLUB_AUTH_NOT_OWNER(FORBIDDEN, "CA07", "모임장(방장) 권한이 필요합니다."),
    CLUB_AUTH_NO_PERMISSION(FORBIDDEN, "CA08", "해당 작업을 수행할 권한이 없습니다."),

    //ClubMember Error
    CLUB_MEMBER_REQUEST_NOT_FOUND(NOT_FOUND, "CM01", "가입 신청 내역을 찾을 수 없습니다."),
    CLUB_MEMBER_ALREADY_WAITING(CONFLICT, "CM02", "이미 가입 승인 대기 중인 회원입니다."),
    CLUB_MEMBER_ALREADY_ACTIVE_STATUS(CONFLICT, "CM03", "이미 해당 모임의 활동 멤버입니다."),
    CLUB_MEMBER_INVALID_APPROVE_TARGET(BAD_REQUEST,  "CM04", "승인 대기 상태의 회원만 가입 승인이 가능합니다."),
    CLUB_MEMBER_NOT_ACTIVE_STATUS(BAD_REQUEST, "CM05", "현재 해당 모임 멤버가 아닙니다."),
    CLUB_MEMBER_KICKED_OUT_USER(FORBIDDEN, "CM06", "강퇴 처리된 사용자는 재가입이 불가합니다."),
    CLUB_MEMBER_NICKNAME_DUPLICATE(CONFLICT, "CM07", "이미 해당 모임에서 사용 중인 닉네임입니다."),

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

    POST_COMMENT_NOT_FOUND(NOT_FOUND, "PC01","댓글을 찾을 수 없습니다"),
    POST_COMMENT_DELETED(GONE, "PC02","삭제된 댓글 입니다"),

    //Schedule Error
    SCHEDULE_NOT_FOUND(NOT_FOUND,"SC01", "일정을 찾을 수 없습니다"),
    SCHEDULE_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST,"SC02", "종료일시는 시작일시보다 이후여야 합니다"),
    SCHEDULE_STAFF_ONLY(HttpStatus.FORBIDDEN,"SC03", "일정 종료는 모임장 또는 운영진만 가능합니다"),
    SCHEDULE_ALREADY_CLOSED(HttpStatus.BAD_REQUEST,"SC04", "이미 종료된 일정입니다"),
    SCHEDULE_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST,"SC05", "이미 취소된 일정입니다")
    ;
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}