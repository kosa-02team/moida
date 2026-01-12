package back.dto.posts.images.response;

import java.util.List;

public record ScheduleImagesResponse(
        List<String> imageUrls
) {
}
