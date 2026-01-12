package back.service;

import back.domain.Schedules;
import back.domain.VoteOptions;
import back.domain.Votes;
import back.dto.ScheduleCreateRequest;
import back.dto.ScheduleResponse;
import back.event.ScheduleRegisteredEvent;
import back.exception.ScheduleException;
import back.repository.SchedulesRepository;
import back.repository.VoteOptionsRepository;
import back.repository.VotesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final SchedulesRepository schedulesRepository;
    private final VotesRepository votesRepository;
    private final VoteOptionsRepository voteOptionsRepository;

    // 알림 전송을 위해 의존성 추가
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 모임에 속한 전체 일정 목록을 조회합니다.
     *
     * @param clubId 모임 ID
     * @param userId 현재 로그인한 사용자 ID (권한 체크용)
     * @return 일정 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedulesByClubId(Long clubId, Long userId) {
        // TODO: 권한 체크 (MEMBER 이상만 조회 가능)
        // TODO: ClubMembers를 통해 userId가 해당 clubId의 멤버인지 확인

        List<Schedules> schedules = schedulesRepository.findByClubId(clubId);

        return schedules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 일정을 생성하고 자동으로 참석/불참 투표를 생성합니다.
     *
     * @param clubId 모임 ID
     * @param userId 일정 생성자 ID (현재 로그인한 사용자)
     * @param request 일정 생성 요청 정보
     * @return 생성된 일정 정보
     */
    @Transactional
    public ScheduleResponse createSchedule(Long clubId, Long userId, ScheduleCreateRequest request) {
        // TODO: 권한 체크 (STAFF 이상만 일정 생성 가능)
        // TODO: ClubMembers를 통해 userId가 해당 clubId의 STAFF인지 확인
        // TODO: ATTENDANCE 투표 생성은 모임장/운영진만 가능

        // 날짜 검증: endDate는 eventDate보다 이후여야 함
        // (DTO 레벨에서 @AssertTrue로 검증하지만, 방어적 프로그래밍을 위해 서비스 레벨에서도 검증)
        if (request.endDate().isBefore(request.eventDate()) ||
            request.endDate().isEqual(request.eventDate())) {
            throw new ScheduleException.InvalidDateRange();
        }

        // 1. 일정 생성
        Schedules schedule = new Schedules(
                clubId,
                request.scheduleName(),
                request.eventDate(),
                request.endDate(),
                request.location(),
                request.description(),
                request.entryFee()
        );
        Schedules savedSchedule = schedulesRepository.save(schedule);

        // 2. 일정 생성과 동시에 ATTENDANCE 타입 투표 자동 생성
        Votes vote = new Votes(
                null, // postId는 null (ATTENDANCE 타입은 게시글과 무관)
                "ATTENDANCE",
                savedSchedule.getScheduleId(),
                userId,
                request.scheduleName() + " 참석 투표",
                request.description(),
                false, // isAnonymous
                false, // allowMultiple
                null   // deadline은 null (ATTENDANCE 타입은 일정 시작 5분 전 자동 종료)
        );
        Votes savedVote = votesRepository.save(vote);

        // 3. 투표 옵션 자동 생성 (참석/불참)
        VoteOptions attendOption = new VoteOptions(
                savedVote.getVoteId(),
                "참석",
                1,
                request.eventDate(),
                request.location()
        );
        VoteOptions notAttendOption = new VoteOptions(
                savedVote.getVoteId(),
                "불참",
                2,
                null,
                null
        );
        voteOptionsRepository.save(attendOption);
        voteOptionsRepository.save(notAttendOption);

        //일정 생성 이벤트 발행
        eventPublisher.publishEvent(new ScheduleRegisteredEvent(
                clubId,
                savedSchedule.getScheduleId(),
                savedSchedule.getScheduleName()
        ));

        return toResponse(savedSchedule);
    }

    private ScheduleResponse toResponse(Schedules schedule) {
        return new ScheduleResponse(
                schedule.getScheduleId(),
                schedule.getScheduleName(),
                schedule.getEventDate(),
                schedule.getEndDate(),
                schedule.getLocation(),
                schedule.getDescription(),
                schedule.getEntryFee(),
                schedule.getTotalSpent(),
                schedule.getRefundPerPerson(),
                schedule.getStatus(),
                schedule.getClosedAt(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
