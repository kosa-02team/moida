package back.service.post;

import back.repository.post.CommentLikeRepository;
import back.repository.post.PostCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLikeService {
    private final CommentLikeRepository commentLikeRepository;
    private final PostCommentRepository postCommentRepository;

    @Transactional
    public void likeComment(Long commentId, Long userId) {
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            return;
        }

        if (!postCommentRepository.existsById(commentId)) {
            throw new IllegalArgumentException("Comment not found");
        }

        commentLikeRepository.save(new back.domain.post.CommentLikes(commentId, userId));
    }

    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
    }
    
    public long getLikeCount(Long commentId) {
        return commentLikeRepository.countByCommentId(commentId);
    }
    
    public boolean isLiked(Long commentId, Long userId) {
        if (userId == null) return false;
        return commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    }
}
