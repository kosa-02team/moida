package back.dto.posts.response;

import back.domain.posts.PostCategory;
import back.domain.posts.Posts;
import java.time.LocalDateTime;

public record PostResponse(
        Long postId,
        Long clubId,
        Long writerId,
        PostCategory category,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static PostResponse from(Posts post) {
        return new PostResponse(
                post.getPostId(),
                post.getClubId(),
                post.getWriterId(),
                post.getCategory(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
