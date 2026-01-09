package back.exception;

import back.exception.response.ErrorCode;

public class VoteException extends CustomGlobalException {

    protected VoteException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected VoteException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static class NotFound extends VoteException {
        public NotFound() {
            super(ErrorCode.VOTE_NOT_FOUND);
        }
    }

    public static class ScheduleIdRequired extends VoteException {
        public ScheduleIdRequired() {
            super(ErrorCode.VOTE_SCHEDULE_ID_REQUIRED);
        }
    }

    public static class ClubMismatch extends VoteException {
        public ClubMismatch() {
            super(ErrorCode.VOTE_CLUB_MISMATCH);
        }
    }

    public static class AlreadyClosed extends VoteException {
        public AlreadyClosed() {
            super(ErrorCode.VOTE_ALREADY_CLOSED);
        }
    }

    public static class DeadlinePassed extends VoteException {
        public DeadlinePassed() {
            super(ErrorCode.VOTE_DEADLINE_PASSED);
        }
    }

    public static class ScheduleIdMissing extends VoteException {
        public ScheduleIdMissing() {
            super(ErrorCode.VOTE_SCHEDULE_ID_MISSING);
        }
    }

    public static class OptionRequired extends VoteException {
        public OptionRequired() {
            super(ErrorCode.VOTE_OPTION_REQUIRED);
        }
    }

    public static class AttendanceSingleOption extends VoteException {
        public AttendanceSingleOption() {
            super(ErrorCode.VOTE_ATTENDANCE_SINGLE_OPTION);
        }
    }

    public static class OptionDuplicate extends VoteException {
        public OptionDuplicate() {
            super(ErrorCode.VOTE_OPTION_DUPLICATE);
        }
    }

    public static class OptionInvalid extends VoteException {
        public OptionInvalid() {
            super(ErrorCode.VOTE_OPTION_INVALID);
        }
    }

    public static class MultipleNotAllowed extends VoteException {
        public MultipleNotAllowed() {
            super(ErrorCode.VOTE_MULTIPLE_NOT_ALLOWED);
        }
    }

    public static class AlreadyParticipated extends VoteException {
        public AlreadyParticipated() {
            super(ErrorCode.VOTE_ALREADY_PARTICIPATED);
        }
    }

    public static class OptionAlreadySelected extends VoteException {
        public OptionAlreadySelected() {
            super(ErrorCode.VOTE_OPTION_ALREADY_SELECTED);
        }
    }

    public static class CreatorOnly extends VoteException {
        public CreatorOnly() {
            super(ErrorCode.VOTE_CREATOR_ONLY);
        }
    }

    public static class MemberOnly extends VoteException {
        public MemberOnly() {
            super(ErrorCode.VOTE_MEMBER_ONLY);
        }
    }

    public static class StaffOnly extends VoteException {
        public StaffOnly() {
            super(ErrorCode.VOTE_STAFF_ONLY);
        }
    }
}
