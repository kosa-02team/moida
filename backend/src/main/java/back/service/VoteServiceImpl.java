package back.service;

import back.dto.VoteCreateRequest;
import back.dto.VoteResponse;
import back.repository.posts.PostsRepository;
import back.repository.VotesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VoteServiceImpl implements VoteService {

    private final PostsRepository postsRepository;
    private final VotesRepository votesRepository;

    @Override
    public VoteResponse createVote(Long clubId, Long userId, VoteCreateRequest request) {
        // ATTENDANCE 타입일 때 scheduleId 필수 검증
        if ("ATTENDANCE".equals(request.voteType()) && request.scheduleId() == null) {
            throw new IllegalArgumentException("참석/불참 투표는 일정 ID가 필수입니다");
        }
        
        // TODO: 실제 투표/게시글 생성 로직은 이후 단계에서 구현
        return null;
    }
}


