package back.dto.post.story.response;

import back.domain.post.PostCategory;
import back.domain.post.Posts;
import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime updatedAt,
        List<String> imagesUrl,
        Long postLikes,
        Boolean isLiked){
    public static PostDetailResponse from(Posts post, List<String> imagesUrl, Long postLikes, Boolean isLiked) {
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
                post.getUpdatedAt(),
                imagesUrl,
                postLikes,
                isLiked);
    }
}
