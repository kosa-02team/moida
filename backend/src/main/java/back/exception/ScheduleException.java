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

    public static class AlreadyClosed extends ScheduleException {
        public AlreadyClosed() {
            super(ErrorCode.SCHEDULE_ALREADY_CLOSED);
        }
    }

    public static class AlreadyCancelled extends ScheduleException {
        public AlreadyCancelled() {
            super(ErrorCode.SCHEDULE_ALREADY_CANCELLED);
        }
    }

    public static class NotOpen extends ScheduleException {
        public NotOpen() {
            super(ErrorCode.SCHEDULE_NOT_OPEN, "일정이 진행 중이 아닙니다");
        }
    }

    public static class NotStarted extends ScheduleException {
        public NotStarted() {
            super(ErrorCode.SCHEDULE_NOT_STARTED, "일정이 아직 시작되지 않았습니다");
        }
    }

    public static class NoAttendees extends ScheduleException {
        public NoAttendees() {
            super(ErrorCode.SCHEDULE_NO_ATTENDEES, "참석자가 없습니다");
        }
    }
}
