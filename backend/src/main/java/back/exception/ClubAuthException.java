package back.exception;

import back.exception.response.ErrorCode;

public class ClubAuthException extends CustomGlobalException {
    protected ClubAuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static class NotFound extends ClubAuthException {
        public NotFound() {
            super(ErrorCode.CLUB_NOT_FOUND);
        }
    }

    public static class NotActive extends ClubAuthException {
        public NotActive() {
            super(ErrorCode.CLUB_NOT_ACTIVE);
        }
    }

    public static class StaffRequired extends ClubAuthException {
        public StaffRequired() {
            super(ErrorCode.CLUB_STAFF_REQUIRED);
        }
    }

    public static class AccountantRequired extends ClubAuthException {
        public AccountantRequired() {
            super(ErrorCode.CLUB_ACCOUNTANT_REQUIRED);
        }
    }

    // 기존 호환성 유지 (운영진 권한 부족)
    public static class RoleInsufficient extends ClubAuthException {
        public RoleInsufficient() {
            super(ErrorCode.CLUB_STAFF_REQUIRED);
        }
    }

    public static class LoginRequired extends ClubAuthException {
        public LoginRequired() {
            super(ErrorCode.CLUB_LOGIN_REQUIRED);
        }
    }
}
