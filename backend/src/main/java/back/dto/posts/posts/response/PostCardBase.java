package back.dto.posts.posts.response;

import java.time.LocalDateTime;

public record PostCardBase(
        Long clubId,
        Long postId,
        Long writerId,
        String writerName,
        String title,
        String content,
        Long postLikes,
        Long commentCount,
        LocalDateTime createdAt
) {}
