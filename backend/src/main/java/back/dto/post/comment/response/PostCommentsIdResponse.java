package back.dto.post.comment.response;

import back.domain.post.Comments;

public record PostCommentsIdResponse(
        Long commentId
) {
    public static PostCommentsIdResponse from(Comments comments){
        return new PostCommentsIdResponse(
            comments.getCommentId()
        );
    }
}
