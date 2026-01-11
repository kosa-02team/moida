package back.dto.posts.response;

import back.domain.posts.PostCategory;
import back.domain.posts.Posts;
import java.time.LocalDateTime;

public record PostDetailResponse(
        Long postId,
        Long clubId,
        Long writerId,
        PostCategory category,
        String title,
        String content,
        Long schedule_id,
        String place,
        LocalDateTime createdAt,
        LocalDateTime updatedAt){
    public static PostDetailResponse from(Posts post) {
        return new PostDetailResponse(
                post.getPostId(),
                post.getClubId(),
                post.getWriterId(),
                post.getCategory(),
                post.getTitle(),
                post.getContent(),
                post.getScheduleId(),
                post.getPlace(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
