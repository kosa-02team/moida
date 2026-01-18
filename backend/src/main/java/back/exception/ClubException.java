package back.exception;

import back.exception.response.ErrorCode;

public class ClubException extends CustomGlobalException {
    public ClubException(ErrorCode errorCode) {
        super(errorCode);
    }

    // Club 관련 예외
    public static class NotFound extends ClubException {
        public NotFound() {
            super(ErrorCode.CLUB_NOT_FOUND);
        }
    }

    public static class AlreadyExists extends ClubException {
        public AlreadyExists() {
            super(ErrorCode.CLUB_ALREADY_EXISTS);
        }
    }

    public static class IsClosed extends ClubException {
        public IsClosed() {
            super(ErrorCode.CLUB_CLOSED);
        }
    }

    public static class ClubFull extends ClubException {
        public ClubFull() {
            super(ErrorCode.CLUB_MAX_CAPACITY);
        }
    }

    // ClubMember 관련 예외
    public static class MemberNotFound extends ClubException {
        public MemberNotFound() {
            super(ErrorCode.CLUB_MEMBER_REQUEST_NOT_FOUND);
        }
    }

    public static class MemberAlreadyPending extends ClubException {
        public MemberAlreadyPending() {
            super(ErrorCode.CLUB_MEMBER_ALREADY_WAITING);
        }
    }

    public static class MemberAlreadyActive extends ClubException {
        public MemberAlreadyActive() {
            super(ErrorCode.CLUB_MEMBER_ALREADY_ACTIVE_STATUS);
        }
    }

    public static class MemberNotPending extends ClubException {
        public MemberNotPending() {
            super(ErrorCode.CLUB_MEMBER_INVALID_APPROVE_TARGET);
        }
    }

    public static class MemberNotActive extends ClubException {
        public MemberNotActive() {
            super(ErrorCode.CLUB_MEMBER_NOT_ACTIVE_STATUS);
        }
    }

    public static class MemberKickedOut extends ClubException {
        public MemberKickedOut() {
            super(ErrorCode.CLUB_MEMBER_KICKED_OUT_USER);
        }
    }

    public static class MemberNicknameDuplicate extends ClubException {
        public MemberNicknameDuplicate() {
            super(ErrorCode.CLUB_MEMBER_NICKNAME_DUPLICATE);
        }
    }

    // ClubAuth 관련 예외
    public static class AuthNotActive extends ClubException {
        public AuthNotActive() {
            super(ErrorCode.CLUB_AUTH_NOT_ACTIVE);
        }
    }

    public static class AuthNotStaff extends ClubException {
        public AuthNotStaff() {
            super(ErrorCode.CLUB_AUTH_NOT_STAFF);
        }
    }

    public static class AuthNotAccountant extends ClubException {
        public AuthNotAccountant() {
            super(ErrorCode.CLUB_AUTH_NOT_ACCOUNTANT);
        }
    }

    public static class AuthStaffRequired extends ClubException {
        public AuthStaffRequired() {
            super(ErrorCode.CLUB_AUTH_STAFF_REQUIRED);
        }
    }

    public static class AuthAccountantRequired extends ClubException {
        public AuthAccountantRequired() {
            super(ErrorCode.CLUB_AUTH_ACCOUNTANT_REQUIRED);
        }
    }

    public static class AuthLoginRequired extends ClubException {
        public AuthLoginRequired() {
            super(ErrorCode.CLUB_AUTH_LOGIN_REQUIRED);
        }
    }

    public static class AuthNotOwner extends ClubException {
        public AuthNotOwner() {
            super(ErrorCode.CLUB_AUTH_NOT_OWNER);
        }
    }

    public static class AuthNoPermission extends ClubException {
        public AuthNoPermission() {
            super(ErrorCode.CLUB_AUTH_NO_PERMISSION);
        }
    }
}
