package back.config;

import back.repository.post.PostRepository;
import back.service.post.ai.PostVectorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DemoVectorInitializer {

    private final PostRepository postRepository;
    private final PostVectorService postVectorService;

    @Transactional
    public void initVectors() {
        for (long postId = 3L; postId <= 13L; postId++) {
            postRepository
                    .findByPostIdAndClub_ClubId(postId, 1L)
                    .ifPresent(postVectorService::savePost);
        }
    }
}