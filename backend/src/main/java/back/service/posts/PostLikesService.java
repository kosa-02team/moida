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
}
