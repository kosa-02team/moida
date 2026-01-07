package back.dto.post.response;

import back.domain.post.Posts;
import java.time.LocalDateTime;

public record PostResponse(
        Long postId,
        Long clubId,
        Long writerId,
        String category,
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
