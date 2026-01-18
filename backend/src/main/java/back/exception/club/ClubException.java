package back.exception.club;

import back.exception.CustomGlobalException;
import back.exception.response.ErrorCode;

public class ClubException extends CustomGlobalException {
    public ClubException(ErrorCode errorCode) {
        super(errorCode);
    }

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
}
