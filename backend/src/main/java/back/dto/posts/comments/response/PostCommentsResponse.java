package back.dto.posts.comments.response;

import back.domain.posts.Comments;

import java.time.LocalDateTime;
import java.util.List;

public record PostCommentsResponse(
        List<Item> comments,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {
    public record Item(
            Long commentId,
            Long writerId,
            String content,
            LocalDateTime createdAt) {
        public static Item from(Comments c) {
            return new Item(
                    c.getCommentId(),
                    c.getWriter().getUserId(),
                    c.getContent(),
                    c.getCreatedAt());
        }
    }

}