package back.dto.post.request;

public record PostCreateRequest(
        Long clubId,
        Long writerId,
        String category,
        String title,
        String content) {
}
