package back.service.vote;

import back.domain.vote.Votes;
import back.repository.vote.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteAutoCloseService {

    private final VoteRepository voteRepository;

    /**
     * 일반 투표 기한 지나면 자동 종료
     * 매 1분마다 실행
     */
    @Scheduled(cron = "0 * * * * ?") // 매 분마다 실행
    @Transactional
    public void closeExpiredGeneralVotes() {
        LocalDateTime now = LocalDateTime.now();
        List<Votes> expiredVotes = voteRepository.findExpiredGeneralVotes(now);
        
        if (!expiredVotes.isEmpty()) {
            log.info("기한이 지난 일반 투표 {}개를 자동 종료합니다", expiredVotes.size());
            expiredVotes.forEach(vote -> {
                vote.close();
                voteRepository.save(vote);
            });
        }
    }

    /**
     * 일정 시작 5분 전이 지난 일정 투표 자동 종료
     * 매 1분마다 실행
     *
     * 주의: ATTENDANCE 투표를 마감할 때 일정은 마감하지 않음
     * 일정 마감은 "일정 마무리" 기능에서만 수행
     */
    @Scheduled(cron = "0 * * * * ?") // 매 분마다 실행
    @Transactional
    public void closeExpiredAttendanceVotes() {
        LocalDateTime now = LocalDateTime.now();
        List<Votes> expiredVotes = voteRepository.findExpiredAttendanceVotes(now);
        
        if (!expiredVotes.isEmpty()) {
            log.info("일정 시작 5분 전이 지난 일정 투표 {}개를 자동 종료합니다", expiredVotes.size());
            expiredVotes.forEach(vote -> {
                // 투표만 마감하고 일정은 OPEN 상태로 유지
                vote.close();
                voteRepository.save(vote);
            });
        }
    }
}
