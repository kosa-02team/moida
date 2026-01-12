package back.dto.posts.comments.response;

import back.domain.posts.Comments;

public record PostCommentsIdResponse(
        Long commentId
) {
    public static PostCommentsIdResponse from(Comments comments){
        return new PostCommentsIdResponse(
            comments.getCommentId()
        );
    }
}
