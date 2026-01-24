package back.dto.post.comment.response;

import back.domain.post.Comments;

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
            LocalDateTime createdAt,
            Long likeCount,
            Boolean isLiked) {
        public static Item from(Comments c, Long likeCount, Boolean isLiked) {
            return new Item(
                    c.getCommentId(),
                    c.getWriter() != null ? c.getWriter().getUserId() : null,
                    c.getContent(),
                    c.getCreatedAt(),
                    likeCount,
                    isLiked);
        }
    }

}