package back.service.posts;

import back.repository.posts.PostLikesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikesService {
    private final PostLikesRepository postLikesRepository;
    private final back.repository.posts.PostsRepository postsRepository;

    @Transactional
    public void likePost(Long postId, Long userId) {
        if (postLikesRepository.existsByPostIdAndUserId(postId, userId)) {
            return;
        }

        if (!postsRepository.existsById(postId)) {
            throw new IllegalArgumentException("Post not found");
        }

        postLikesRepository.save(new back.domain.posts.PostLikes(postId, userId));
    }

    @Transactional
    public void unlikePost(Long postId, Long userId) {
        postLikesRepository.deleteByPostIdAndUserId(postId, userId);
    }
}
