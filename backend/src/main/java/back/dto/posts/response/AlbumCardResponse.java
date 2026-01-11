package back.dto.posts.response;

import java.time.LocalDateTime;

public record AlbumCardResponse(
        Long clubId,
        Long postId,
        Long scheduleId,
        String scheduleName,
        String coverImageUrl,
        long imageCount,
        LocalDateTime lastCreatedAt
) {}
