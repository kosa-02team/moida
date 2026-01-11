package back.dto.posts.response;

import java.util.List;

public record ScheduleImagesResponse(
        List<String> imageUrls
) {
}
