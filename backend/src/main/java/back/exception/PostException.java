package back.exception;

import back.exception.response.ErrorCode;

public class PostException extends CustomGlobalException {

    protected PostException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static class NotFound extends PostException{
        public NotFound() {
            super(ErrorCode.POST_NOT_FOUND);
        }

    }

    public static class Deleted extends PostException{
        public Deleted() {
            super(ErrorCode.POST_DELETED);
        }

    }

    public static class Forbidden extends PostException{
        public Forbidden() {
            super(ErrorCode.POST_FORBIDDEN);
        }

    }
}
