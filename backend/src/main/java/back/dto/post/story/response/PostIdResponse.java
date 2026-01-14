package back.dto.post.response;

import back.domain.post.Posts;

public record PostIdResponse(
        Long postId) {

    public static PostIdResponse from(Posts post) {
        return new PostIdResponse(post.getPostId());
    }
}
