package back.dto.post.story.response;

import back.domain.post.PostCategory;
import back.domain.post.Posts;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long postId,
        Long clubId,
        Long writerId,
        String writerName,
        String writerProfileImageUrl,
        PostCategory category,
        String title,
        String content,
        Long scheduleId,
        String place,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> imagesUrl,
        Long postLikes,
        Boolean isLiked,
        Boolean isMyPost,
        List<Long> taggedMemberIds) {

    public static PostDetailResponse from(
            Posts post,
            List<String> imagesUrl,
            Long postLikes,
            Boolean isLiked,
            Boolean isMyPost,
            List<Long> taggedMemberIds) {

        Long scheduleId = (post.getSchedule() == null) ? null : post.getSchedule().getScheduleId();

        return new PostDetailResponse(
                post.getPostId(),
                post.getClub().getClubId(),
                post.getWriter().getUserId(),
                post.getWriter().getNickname(),
                null, // writerProfileImageUrl (User entity has no profile image)
                post.getCategory(),
                post.getTitle(),
                post.getContent(),
                scheduleId,
                post.getPlace(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                imagesUrl,
                postLikes,
                isLiked,
                isMyPost,
                taggedMemberIds);
    }
}
