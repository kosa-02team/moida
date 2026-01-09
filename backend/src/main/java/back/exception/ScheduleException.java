package back.exception;

import back.exception.response.ErrorCode;

public class ScheduleException extends CustomGlobalException {

    protected ScheduleException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected ScheduleException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static class NotFound extends ScheduleException {
        public NotFound() {
            super(ErrorCode.SCHEDULE_NOT_FOUND);
        }
    }

    public static class InvalidDateRange extends ScheduleException {
        public InvalidDateRange() {
            super(ErrorCode.SCHEDULE_INVALID_DATE_RANGE);
        }
    }

    public static class StaffOnly extends ScheduleException {
        public StaffOnly() {
            super(ErrorCode.SCHEDULE_STAFF_ONLY);
        }
    }
}
