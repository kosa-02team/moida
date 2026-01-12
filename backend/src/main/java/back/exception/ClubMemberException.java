package back.exception;

import back.exception.response.ErrorCode;

public class ClubMemberException extends CustomGlobalException {
    public ClubMemberException(ErrorCode errorCode) {

        super(errorCode);
    }
}
