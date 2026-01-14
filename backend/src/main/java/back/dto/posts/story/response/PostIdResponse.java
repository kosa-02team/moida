package back.dto.posts.story.response;

import back.domain.posts.Posts;

public record PostIdResponse(
        Long postId) {

    public static PostIdResponse from(Posts post) {
        return new PostIdResponse(post.getPostId());
    }
}
