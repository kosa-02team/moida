package back.exception.club;

import back.exception.response.ErrorCode;

public class ClubMemberException extends ClubException {

    public ClubMemberException(ErrorCode errorCode) {
        super(errorCode);
    }
    public static class NotFound extends ClubMemberException {
        public NotFound() {
            super(ErrorCode.CLUB_MEMBER_REQUEST_NOT_FOUND);
        }
    }

    public static class AlreadyPending extends ClubMemberException {
        public AlreadyPending() {
            super(ErrorCode.CLUB_MEMBER_ALREADY_WAITING);
        }
    }

    public static class AlreadyActive extends ClubMemberException {
        public AlreadyActive() {
            super(ErrorCode.CLUB_MEMBER_ALREADY_ACTIVE_STATUS);
        }
    }

    public static class NotPending extends ClubMemberException {
        public NotPending() {
            super(ErrorCode.CLUB_MEMBER_INVALID_APPROVE_TARGET);
        }
    }

    public static class NotActive extends ClubMemberException {
        public NotActive() {
            super(ErrorCode.CLUB_MEMBER_NOT_ACTIVE_STATUS);
        }
    }

    public static class KickedOut extends ClubMemberException {
        public KickedOut() {
            super(ErrorCode.CLUB_MEMBER_KICKED_OUT_USER);
        }
    }

    public static class NicknameDuplicate extends ClubMemberException {
        public NicknameDuplicate() {
            super(ErrorCode.CLUB_MEMBER_NICKNAME_DUPLICATE);
        }
    }
}
