package back.repository.post;

import back.domain.post.Comments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostCommentRepository extends JpaRepository<Comments, Long> {
    Page<Comments> findAllByPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(
            Long postId,
            Long clubId,
            Pageable pageable
    );

    Optional<Comments> findByCommentIdAndPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(
            Long commentId, Long postId, Long clubId
    );
}
