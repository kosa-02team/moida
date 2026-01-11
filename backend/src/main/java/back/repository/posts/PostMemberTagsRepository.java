package back.repository.posts;

import back.domain.posts.PostMemberTags;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostMemberTagsRepository extends JpaRepository<PostMemberTags, Long> {
    void deleteByPostId(Long postId);
}
