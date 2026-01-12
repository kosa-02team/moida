package back.dto.posts.comments.request;

import jakarta.validation.constraints.Size;

public record PostCommentRequest(
        @Size(max = 500, message = "content는 최대 500자입니다.")
        String content
) {
}
