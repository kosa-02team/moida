package back.service.vote;

import back.domain.vote.Votes;
import back.event.VoteDeadlineEvent;
import back.repository.post.PostRepository;
import back.repository.schedule.ScheduleRepository;
import back.repository.vote.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteScheduler {

    private final VoteRepository voteRepository;
    private final ScheduleRepository scheduleRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 매 분마다 마감 시간이 1시간 ~ 1시간 1분 남은 투표를 감지하여 알림을 보냅니다.
     * (테스트 및 즉각적인 반응을 위해 1분 주기로 설정)
     */
    @Scheduled(cron = "0 * * * * *") // 매 분 0초 실행
    @Transactional(readOnly = true)
    public void checkUpcomingVoteDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);
        LocalDateTime oneHourOneMinuteLater = now.plusHours(1).plusMinutes(1);

        // 마감 기한이 [1시간 후 ~ 1시간 1분 후] 사이인 투표 조회
        List<Votes> upcomingVotes = voteRepository.findUpcomingDeadlines(oneHourLater, oneHourOneMinuteLater);

        if (upcomingVotes.isEmpty()) {
            return;
        }

        log.info("Found {} votes with upcoming deadline.", upcomingVotes.size());

        for (Votes vote : upcomingVotes) {
            Long clubId = getClubId(vote);
            if (clubId != null) {
                eventPublisher.publishEvent(new VoteDeadlineEvent(
                        vote.getVoteId(),
                        vote.getTitle(),
                        clubId));
            } else {
                log.warn("Vote {} has no associated clubId.", vote.getVoteId());
            }
        }
    }

    private Long getClubId(Votes vote) {
        if ("ATTENDANCE".equals(vote.getVoteType()) && vote.getScheduleId() != null) {
            return scheduleRepository.findById(vote.getScheduleId())
                    .map(schedule -> schedule.getClubId())
                    .orElse(null);
        } else if ("GENERAL".equals(vote.getVoteType()) && vote.getPostId() != null) {
            return postRepository.findById(vote.getPostId())
                    .map(post -> post.getClub().getClubId())
                    .orElse(null);
        }
        return null;
    }
}
