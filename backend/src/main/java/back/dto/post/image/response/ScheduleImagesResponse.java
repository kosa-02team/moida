package back.dto.post.image.response;

import java.util.List;

public record ScheduleImagesResponse(
        List<String> imageUrls
) {
}
