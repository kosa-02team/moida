package back.service.schedule;

import back.domain.Users;
import back.domain.schedule.ScheduleParticipants;
import back.domain.schedule.Schedules;
import back.domain.vote.VoteOptions;
import back.domain.vote.Votes;
import back.dto.schedule.*;
import back.event.ScheduleRegisteredEvent;
import back.exception.ScheduleException;
import back.repository.UserRepository;
import back.repository.schedule.ScheduleParticipantRepository;
import back.repository.schedule.ScheduleRepository;
import back.repository.vote.VoteOptionRepository;
import back.repository.vote.VoteRepository;
import back.service.club.ClubAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final ClubAuthService clubAuthService;
    private final UserRepository userRepository;

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
        // 권한 체크: ACTIVE 멤버만 조회 가능
        clubAuthService.assertActiveMember(clubId, userId);

        List<Schedules> schedules = scheduleRepository.findByClubId(clubId);

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
        // 권한 체크: 참가비가 있으면 총무 이상, 없으면 운영진 이상
        boolean hasEntryFee = request.entryFee() != null && request.entryFee().compareTo(java.math.BigDecimal.ZERO) > 0;
        if (hasEntryFee) {
            clubAuthService.assertAtLeastAccountant(clubId, userId);
        } else {
            clubAuthService.assertAtLeastManager(clubId, userId);
        }

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
        if (request.voteDeadline() != null) {
            schedule.setVoteDeadline(request.voteDeadline());
        }
        Schedules savedSchedule = scheduleRepository.save(schedule);

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
        Votes savedVote = voteRepository.save(vote);

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
        voteOptionRepository.save(attendOption);
        voteOptionRepository.save(notAttendOption);

        //일정 생성 이벤트 발행
        eventPublisher.publishEvent(new ScheduleRegisteredEvent(
                clubId,
                savedSchedule.getScheduleId(),
                savedSchedule.getScheduleName()
        ));

        return toResponse(savedSchedule);
    }

    /**
     * 단일 일정을 조회합니다.
     *
     * @param clubId     모임 ID
     * @param scheduleId 일정 ID
     * @param userId     현재 로그인한 사용자 ID
     * @return 일정 정보
     */
    @Transactional(readOnly = true)
    public ScheduleResponse getScheduleById(Long clubId, Long scheduleId, Long userId) {
        // 권한 체크: ACTIVE 멤버만 조회 가능
        clubAuthService.assertActiveMember(clubId, userId);

        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        // 일정이 해당 모임에 속하는지 확인
        if (!schedule.getClubId().equals(clubId)) {
            throw new ScheduleException.NotFound();
        }

        return toResponse(schedule);
    }

    /**
     * 일정을 수정합니다.
     *
     * @param clubId     모임 ID
     * @param scheduleId 일정 ID
     * @param userId     현재 로그인한 사용자 ID
     * @param request    수정할 일정 정보
     * @return 수정된 일정 정보
     */
    @Transactional
    public ScheduleResponse updateSchedule(Long clubId, Long scheduleId, Long userId, ScheduleUpdateRequest request) {
        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        // 일정이 해당 모임에 속하는지 확인
        if (!schedule.getClubId().equals(clubId)) {
            throw new ScheduleException.NotFound();
        }

        // 참가비 변경 여부 확인
        java.math.BigDecimal currentEntryFee = schedule.getEntryFee() != null ? schedule.getEntryFee() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal newEntryFee = request.entryFee() != null ? request.entryFee() : java.math.BigDecimal.ZERO;
        boolean isEntryFeeChanged = currentEntryFee.compareTo(newEntryFee) != 0;

        // 권한 체크: 참가비 필드를 변경하면 총무 이상, 그 외 수정은 운영진 이상
        if (isEntryFeeChanged) {
            clubAuthService.assertAtLeastAccountant(clubId, userId);
        } else {
            clubAuthService.assertAtLeastManager(clubId, userId);
        }

        // 이미 종료되거나 취소된 일정은 수정 불가
        if ("CLOSED".equals(schedule.getStatus()) || "CANCELLED".equals(schedule.getStatus())) {
            throw new ScheduleException.AlreadyClosed();
        }

        // 날짜 유효성 검증
        if (request.endDate().isBefore(request.eventDate()) || 
            request.endDate().isEqual(request.eventDate())) {
            throw new ScheduleException.InvalidDateRange();
        }

        schedule.updateSchedule(
                request.scheduleName(),
                request.eventDate(),
                request.endDate(),
                request.location(),
                request.description(),
                request.entryFee()
        );

        return toResponse(schedule);
    }

    /**
     * 일정을 마감합니다.
     *
     * @param clubId     모임 ID
     * @param scheduleId 일정 ID
     * @param userId     현재 로그인한 사용자 ID
     */
    @Transactional
    public void closeSchedule(Long clubId, Long scheduleId, Long userId) {
        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        // 일정이 해당 모임에 속하는지 확인
        if (!schedule.getClubId().equals(clubId)) {
            throw new ScheduleException.NotFound();
        }

        // 권한 체크: 참가비가 있으면 총무 이상, 없으면 운영진 이상
        boolean hasEntryFee = schedule.getEntryFee() != null && schedule.getEntryFee().compareTo(java.math.BigDecimal.ZERO) > 0;
        if (hasEntryFee) {
            clubAuthService.assertAtLeastAccountant(clubId, userId);
        } else {
            clubAuthService.assertAtLeastManager(clubId, userId);
        }

        // 이미 종료되거나 취소된 일정인지 확인
        if ("CLOSED".equals(schedule.getStatus())) {
            throw new ScheduleException.AlreadyClosed();
        }
        if ("CANCELLED".equals(schedule.getStatus())) {
            throw new ScheduleException.AlreadyCancelled();
        }

        schedule.close();

        // 연관된 ATTENDANCE 투표도 종료
        voteRepository.findByScheduleId(scheduleId).ifPresent(Votes::close);
    }

    /**
     * 일정을 취소합니다.
     *
     * @param clubId     모임 ID
     * @param scheduleId 일정 ID
     * @param userId     현재 로그인한 사용자 ID
     * @param request    취소 사유 (선택사항)
     */
    @Transactional
    public void cancelSchedule(Long clubId, Long scheduleId, Long userId, ScheduleCancelRequest request) {
        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        // 일정이 해당 모임에 속하는지 확인
        if (!schedule.getClubId().equals(clubId)) {
            throw new ScheduleException.NotFound();
        }

        // 권한 체크: 참가비가 있으면 총무 이상 (환불 문제), 없으면 운영진 이상
        boolean hasEntryFee = schedule.getEntryFee() != null && schedule.getEntryFee().compareTo(java.math.BigDecimal.ZERO) > 0;
        if (hasEntryFee) {
            clubAuthService.assertAtLeastAccountant(clubId, userId);
        } else {
            clubAuthService.assertAtLeastManager(clubId, userId);
        }

        // 이미 취소된 일정인지 확인
        if ("CANCELLED".equals(schedule.getStatus())) {
            throw new ScheduleException.AlreadyCancelled();
        }

        // 취소 사유가 있으면 포함하여 취소
        if (request != null && request.cancelReason() != null && !request.cancelReason().isBlank()) {
            schedule.cancel(request.cancelReason());
        } else {
            schedule.cancel();
        }

        // 연관된 ATTENDANCE 투표도 종료
        voteRepository.findByScheduleId(scheduleId).ifPresent(vote -> {
            vote.close();
            voteRepository.save(vote);
        });
    }

    /**
     * 일정의 정산 정보를 수정합니다.
     *
     * @param clubId     모임 ID
     * @param scheduleId 일정 ID
     * @param userId     현재 로그인한 사용자 ID
     * @param request    정산 정보
     * @return 수정된 일정 정보
     */
    @Transactional
    public ScheduleResponse updateSettlement(Long clubId, Long scheduleId, Long userId, ScheduleSettlementRequest request) {
        // 권한 체크: 정산은 무조건 총무 이상만 가능 (돈 관련)
        clubAuthService.assertAtLeastAccountant(clubId, userId);

        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        // 일정이 해당 모임에 속하는지 확인
        if (!schedule.getClubId().equals(clubId)) {
            throw new ScheduleException.NotFound();
        }

        schedule.updateSettlement(request.totalSpent(), request.refundPerPerson());

        return toResponse(schedule);
    }

    /**
     * 일정의 참여자 목록을 조회합니다.
     *
     * @param clubId     모임 ID
     * @param scheduleId 일정 ID
     * @param userId     현재 로그인한 사용자 ID
     * @return 참여자 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleParticipantResponse> getScheduleParticipants(Long clubId, Long scheduleId, Long userId) {
        // 권한 체크: ACTIVE 멤버만 조회 가능
        clubAuthService.assertActiveMember(clubId, userId);

        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        // 일정이 해당 모임에 속하는지 확인
        if (!schedule.getClubId().equals(clubId)) {
            throw new ScheduleException.NotFound();
        }

        List<ScheduleParticipants> participants = scheduleParticipantRepository.findByScheduleId(scheduleId);

        // 사용자 정보 일괄 조회
        List<Long> userIds = participants.stream()
                .map(ScheduleParticipants::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> userNameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(Users::getUserId, Users::getRealName));

        return participants.stream()
                .map(p -> new ScheduleParticipantResponse(
                        p.getParticipantId(),
                        p.getScheduleId(),
                        p.getUserId(),
                        userNameMap.getOrDefault(p.getUserId(), "Unknown"),
                        p.getAttendanceStatus(),
                        p.getFeeStatus(),
                        p.getIsRefunded(),
                        p.getCreatedAt(),
                        p.getUpdatedAt()
                ))
                .collect(Collectors.toList());
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
                schedule.getCancelReason(),
                schedule.getVoteDeadline(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
