package back.dto.post.response;

import java.util.List;

public record ScheduleImagesResponse(
        List<String> imageUrls
) {
}
