package back.dto.post.post.response;

import java.time.LocalDateTime;
import java.util.List;

public record PostCardResponse(
        Long clubId,
        Long postId,
        Long writerId,
        String writerName,
        String title,
        String content,
        List<String> imagesUrl,
        Long postLikes,
        Long commentCount,
        LocalDateTime createdAt
) {
    public static PostCardResponse of(PostCardBase p, List<String> imagesUrl) {
        return new PostCardResponse(
                p.clubId(),
                p.postId(),
                p.writerId(),
                p.writerName(),
                p.title(),
                p.content(),
                imagesUrl,
                p.postLikes(),
                p.commentCount(),
                p.createdAt()
        );
    }
}