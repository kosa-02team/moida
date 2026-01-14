package back.dto.post.story.response;

import java.time.LocalDateTime;

public record PostCardBase(
        Long clubId,
        Long postId,
        Long writerId,
        String title,
        String content,
        Long postLikes,
        Long commentCount,
        LocalDateTime createdAt
) {}
