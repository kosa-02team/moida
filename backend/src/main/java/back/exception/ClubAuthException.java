package back.exception;

import back.exception.response.ErrorCode;

public class ClubAuthException extends CustomGlobalException {
    protected ClubAuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static class NotActive extends ClubAuthException{
        public NotActive() {
            super(ErrorCode.CLUB_MEMBER_NOT_FOUND);
        }

    }

    public static class RoleInsufficient extends ClubAuthException{
        public RoleInsufficient() {
            super(ErrorCode.CLUB_MEMBER_NOT_FOUND);
        }

    }
}
