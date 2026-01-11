package back.repository.posts;

import back.domain.posts.PostComments;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentsRepository extends JpaRepository<PostComments, Long> {
}
