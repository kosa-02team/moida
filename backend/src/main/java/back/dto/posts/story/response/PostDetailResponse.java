package back.dto.posts.story.response;

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
        Long scheduleId,
        String place,
        LocalDateTime createdAt,
        LocalDateTime updatedAt){
    public static PostDetailResponse from(Posts post) {
        Long scheduleId = (post.getSchedule() == null) ? null : post.getSchedule().getScheduleId();

        return new PostDetailResponse(
                post.getPostId(),
                post.getClub().getClubId(),
                post.getWriter().getUserId(),
                post.getCategory(),
                post.getTitle(),
                post.getContent(),
                scheduleId,
                post.getPlace(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
