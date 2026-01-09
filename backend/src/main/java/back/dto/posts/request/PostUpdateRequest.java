package back.dto.posts.request;

public record PostUpdateRequest(
        String title,
        String content) {
}
