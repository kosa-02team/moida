package back.repository.post;

import back.domain.post.CommentLikes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLikes, Long> {
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    
    void deleteByCommentIdAndUserId(Long commentId, Long userId);
    
    long countByCommentId(Long commentId);
}
