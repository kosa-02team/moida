package back.repository.posts;

import back.domain.posts.PostImages;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImagesRepository extends JpaRepository<PostImages, Long> {
    void deleteByPost_PostId(Long postId);
}