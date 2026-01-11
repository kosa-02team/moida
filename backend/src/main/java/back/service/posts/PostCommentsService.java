package back.service.posts;

import back.repository.posts.PostCommentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCommentsService {
    private final PostCommentsRepository postCommentsRepository;

}
