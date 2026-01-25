package back.config;

import back.repository.post.PostRepository;
import back.service.post.ai.PostVectorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoVectorInitializer {

    private final PostRepository postRepository;
    private final PostVectorService postVectorService;

    /**
     * 애플리케이션 시작 시 자동으로 데모 포스트 벡터화
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initVectors() {
        log.info("[DemoVectorInitializer] 데모 포스트 벡터화 시작...");
        int count = 0;
        for (long postId = 3L; postId <= 13L; postId++) {
            try {
                if (postRepository.findByPostIdAndClub_ClubId(postId, 1L).isPresent()) {
                    postRepository.findByPostIdAndClub_ClubId(postId, 1L)
                            .ifPresent(postVectorService::savePost);
                    count++;
                }
            } catch (Exception e) {
                log.warn("[DemoVectorInitializer] postId={} 벡터화 실패: {}", postId, e.getMessage());
            }
        }
        log.info("[DemoVectorInitializer] 데모 포스트 벡터화 완료: {}건", count);
    }
}