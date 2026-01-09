package back.repository.posts;

import back.domain.posts.PostMemberTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostMemberTagRepository extends JpaRepository<PostMemberTag, Long> {
}
