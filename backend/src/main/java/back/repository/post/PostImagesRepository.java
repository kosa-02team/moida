package back.repository.post;

import back.domain.post.PostImages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImagesRepository extends JpaRepository<PostImages, Long> {
    void deleteByPost_PostId(Long postId);
}