package back.dto.post.story.response;

import back.domain.post.Posts;

public record PostIdResponse(
        Long postId) {

    public static PostIdResponse from(Posts post) {
        return new PostIdResponse(post.getPostId());
    }
}
