package back.repository.post;

import back.domain.post.PostMemberTags;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostMemberTagRepository extends JpaRepository<PostMemberTags, Long> {
    void deleteByPostId(Long postId);
}
