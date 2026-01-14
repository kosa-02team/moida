package back.service.post;

import back.repository.post.PostLikeRepository;
import back.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {
    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;

    @Transactional
    public void likePost(Long postId, Long userId) {
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            return;
        }

        if (!postRepository.existsById(postId)) {
            throw new IllegalArgumentException("Post not found");
        }

        postLikeRepository.save(new back.domain.post.PostLikes(postId, userId));
    }

    @Transactional
    public void unlikePost(Long postId, Long userId) {
        postLikeRepository.deleteByPostIdAndUserId(postId, userId);
    }
}
