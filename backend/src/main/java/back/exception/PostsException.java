package back.exception;

import back.exception.response.ErrorCode;

public class PostsException extends CustomGlobalException {

    protected PostsException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static class PostNotFound extends PostsException {
        public PostNotFound() {
            super(ErrorCode.POST_NOT_FOUND);
        }

    }

    public static class Deleted extends PostsException {
        public Deleted() {
            super(ErrorCode.POST_DELETED);
        }

    }

    public static class Forbidden extends PostsException {
        public Forbidden() {
            super(ErrorCode.POST_FORBIDDEN);
        }

    }

    public static class PostCommentNotFound extends PostsException {
        public PostCommentNotFound() {
            super(ErrorCode.POST_COMMENT_NOT_FOUND);
        }

    }

    public static class PostCommentDeleted extends PostsException {
        public PostCommentDeleted() {
            super(ErrorCode.POST_COMMENT_DELETED);
        }

    }
}
