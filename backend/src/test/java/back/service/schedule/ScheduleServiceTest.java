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
import back.service.clubs.ClubsAuthorizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleParticipantRepository scheduleParticipantRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private VoteOptionRepository voteOptionRepository;

    @Mock
    private ClubsAuthorizationService clubsAuthorizationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ScheduleService scheduleService;

    private static <T> T newEntity(Class<T> type) {
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Schedules schedule(Long id, Long clubId) {
        Schedules s = newEntity(Schedules.class);
        ReflectionTestUtils.setField(s, "scheduleId", id);
        ReflectionTestUtils.setField(s, "clubId", clubId);
        return s;
    }

    @Nested
    @DisplayName("일정 목록 조회")
    class GetSchedulesByClubId {

        @Test
        @DisplayName("모임 일정 목록 조회 성공")
        void get_schedules_success() {
            // given
            Long clubId = 1L;
            Long userId = 10L;

            Schedules schedule1 = schedule(1L, clubId);
            ReflectionTestUtils.setField(schedule1, "scheduleName", "일정1");
            ReflectionTestUtils.setField(schedule1, "status", "OPEN");

            Schedules schedule2 = schedule(2L, clubId);
            ReflectionTestUtils.setField(schedule2, "scheduleName", "일정2");
            ReflectionTestUtils.setField(schedule2, "status", "CLOSED");

            given(scheduleRepository.findByClubId(clubId))
                    .willReturn(List.of(schedule1, schedule2));

            // when
            List<ScheduleResponse> result = scheduleService.getSchedulesByClubId(clubId, userId);

            // then
            assertThat(result).hasSize(2);
            then(scheduleRepository).should(times(1)).findByClubId(clubId);
        }

        @Test
        @DisplayName("모임 일정 목록 조회 - 결과 없음")
        void get_schedules_empty() {
            // given
            Long clubId = 1L;
            Long userId = 10L;

            given(scheduleRepository.findByClubId(clubId))
                    .willReturn(List.of());

            // when
            List<ScheduleResponse> result = scheduleService.getSchedulesByClubId(clubId, userId);

            // then
            assertThat(result).isEmpty();
            then(scheduleRepository).should(times(1)).findByClubId(clubId);
        }
    }

    @Nested
    @DisplayName("일정 생성")
    class CreateSchedule {

        @Test
        @DisplayName("일정 생성 성공 - ATTENDANCE 투표 자동 생성")
        void create_schedule_success() {
            // given
            Long clubId = 1L;
            Long userId = 10L;
            LocalDateTime eventDate = LocalDateTime.now().plusDays(7);
            LocalDateTime endDate = LocalDateTime.now().plusDays(8);

            ScheduleCreateRequest request = new ScheduleCreateRequest(
                    "모임 일정",
                    eventDate,
                    endDate,
                    "강남역",
                    "모임 설명",
                    BigDecimal.valueOf(10000),
                    null
            );

            Schedules savedSchedule = schedule(1L, clubId);
            ReflectionTestUtils.setField(savedSchedule, "scheduleName", request.scheduleName());
            ReflectionTestUtils.setField(savedSchedule, "eventDate", request.eventDate());
            ReflectionTestUtils.setField(savedSchedule, "endDate", request.endDate());
            ReflectionTestUtils.setField(savedSchedule, "location", request.location());
            ReflectionTestUtils.setField(savedSchedule, "description", request.description());
            ReflectionTestUtils.setField(savedSchedule, "entryFee", request.entryFee());

            Votes savedVote = newEntity(Votes.class);
            ReflectionTestUtils.setField(savedVote, "voteId", 1L);
            ReflectionTestUtils.setField(savedVote, "voteType", "ATTENDANCE");
            ReflectionTestUtils.setField(savedVote, "scheduleId", 1L);

            given(scheduleRepository.save(any(Schedules.class))).willReturn(savedSchedule);
            given(voteRepository.save(any(Votes.class))).willReturn(savedVote);

            // when
            ScheduleResponse result = scheduleService.createSchedule(clubId, userId, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.scheduleName()).isEqualTo("모임 일정");

            then(scheduleRepository).should(times(1)).save(any(Schedules.class));
            then(voteRepository).should(times(1)).save(any(Votes.class));
            then(voteOptionRepository).should(times(2)).save(any(VoteOptions.class));
            then(eventPublisher).should(times(1)).publishEvent(any(ScheduleRegisteredEvent.class));
        }

        @Test
        @DisplayName("일정 생성 실패 - 종료일시가 시작일시보다 이전")
        void create_schedule_invalid_date_range() {
            // given
            Long clubId = 1L;
            Long userId = 10L;
            LocalDateTime eventDate = LocalDateTime.now().plusDays(7);
            LocalDateTime endDate = LocalDateTime.now().plusDays(6); // 시작일시보다 이전

            ScheduleCreateRequest request = new ScheduleCreateRequest(
                    "모임 일정",
                    eventDate,
                    endDate,
                    "강남역",
                    "모임 설명",
                    BigDecimal.valueOf(10000),
                    null
            );

            // when & then
            assertThatThrownBy(() -> scheduleService.createSchedule(clubId, userId, request))
                    .isInstanceOf(ScheduleException.InvalidDateRange.class);

            then(scheduleRepository).shouldHaveNoInteractions();
            then(voteRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("일정 생성 성공 - voteDeadline 설정")
        void create_schedule_with_vote_deadline() {
            // given
            Long clubId = 1L;
            Long userId = 10L;
            LocalDateTime eventDate = LocalDateTime.now().plusDays(7);
            LocalDateTime endDate = LocalDateTime.now().plusDays(8);
            LocalDateTime voteDeadline = LocalDateTime.now().plusDays(5);

            ScheduleCreateRequest request = new ScheduleCreateRequest(
                    "모임 일정",
                    eventDate,
                    endDate,
                    "강남역",
                    "모임 설명",
                    BigDecimal.valueOf(10000),
                    voteDeadline
            );

            Schedules savedSchedule = schedule(1L, clubId);
            ReflectionTestUtils.setField(savedSchedule, "scheduleName", request.scheduleName());
            ReflectionTestUtils.setField(savedSchedule, "voteDeadline", voteDeadline);

            Votes savedVote = newEntity(Votes.class);
            ReflectionTestUtils.setField(savedVote, "voteId", 1L);

            given(scheduleRepository.save(any(Schedules.class))).willReturn(savedSchedule);
            given(voteRepository.save(any(Votes.class))).willReturn(savedVote);

            // when
            ScheduleResponse result = scheduleService.createSchedule(clubId, userId, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.voteDeadline()).isEqualTo(voteDeadline);

            then(scheduleRepository).should(times(1)).save(any(Schedules.class));
        }
    }

    @Nested
    @DisplayName("단일 일정 조회")
    class GetScheduleById {

        @Test
        @DisplayName("일정 조회 성공")
        void get_schedule_by_id_success() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;

            Schedules schedule = schedule(scheduleId, clubId);
            ReflectionTestUtils.setField(schedule, "scheduleName", "테스트 일정");
            ReflectionTestUtils.setField(schedule, "status", "OPEN");

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

            // when
            ScheduleResponse result = scheduleService.getScheduleById(clubId, scheduleId, userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.scheduleName()).isEqualTo("테스트 일정");
            then(clubsAuthorizationService).should(times(1)).assertActiveMember(clubId, userId);
        }

        @Test
        @DisplayName("일정 조회 실패 - 일정 없음")
        void get_schedule_by_id_fail_not_found() {
            // given
            Long clubId = 1L;
            Long scheduleId = 999L;
            Long userId = 10L;

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scheduleService.getScheduleById(clubId, scheduleId, userId))
                    .isInstanceOf(ScheduleException.NotFound.class);
        }

        @Test
        @DisplayName("일정 조회 실패 - 다른 모임의 일정")
        void get_schedule_by_id_fail_club_mismatch() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;
            Long otherClubId = 999L;

            Schedules schedule = schedule(scheduleId, otherClubId);

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

            // when & then
            assertThatThrownBy(() -> scheduleService.getScheduleById(clubId, scheduleId, userId))
                    .isInstanceOf(ScheduleException.NotFound.class);
        }
    }

    @Nested
    @DisplayName("일정 수정")
    class UpdateSchedule {

        @Test
        @DisplayName("일정 수정 성공 - 참가비 변경 없이 일반 필드만 수정")
        void update_schedule_success_no_fee_change() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;
            LocalDateTime eventDate = LocalDateTime.now().plusDays(7);
            LocalDateTime endDate = LocalDateTime.now().plusDays(8);

            Schedules schedule = schedule(scheduleId, clubId);
            ReflectionTestUtils.setField(schedule, "scheduleName", "기존 일정");
            ReflectionTestUtils.setField(schedule, "status", "OPEN");
            ReflectionTestUtils.setField(schedule, "entryFee", BigDecimal.valueOf(10000));

            ScheduleUpdateRequest request = new ScheduleUpdateRequest(
                    "수정된 일정",
                    eventDate,
                    endDate,
                    "신촌역",
                    "수정된 설명",
                    BigDecimal.valueOf(10000) // 참가비 동일
            );

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

            // when
            ScheduleResponse result = scheduleService.updateSchedule(clubId, scheduleId, userId, request);

            // then
            assertThat(result).isNotNull();
            then(clubsAuthorizationService).should(times(1)).assertAtLeastManager(clubId, userId);
            then(clubsAuthorizationService).should(never()).assertAtLeastAccountant(clubId, userId);
        }

        @Test
        @DisplayName("일정 수정 성공 - 참가비 변경 시 총무 권한 체크")
        void update_schedule_success_fee_change() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;
            LocalDateTime eventDate = LocalDateTime.now().plusDays(7);
            LocalDateTime endDate = LocalDateTime.now().plusDays(8);

            Schedules schedule = schedule(scheduleId, clubId);
            ReflectionTestUtils.setField(schedule, "scheduleName", "기존 일정");
            ReflectionTestUtils.setField(schedule, "status", "OPEN");
            ReflectionTestUtils.setField(schedule, "entryFee", BigDecimal.valueOf(10000));

            ScheduleUpdateRequest request = new ScheduleUpdateRequest(
                    "수정된 일정",
                    eventDate,
                    endDate,
                    "신촌역",
                    "수정된 설명",
                    BigDecimal.valueOf(15000) // 참가비 변경
            );

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

            // when
            ScheduleResponse result = scheduleService.updateSchedule(clubId, scheduleId, userId, request);

            // then
            assertThat(result).isNotNull();
            then(clubsAuthorizationService).should(times(1)).assertAtLeastAccountant(clubId, userId);
            then(clubsAuthorizationService).should(never()).assertAtLeastManager(clubId, userId);
        }

        @Test
        @DisplayName("일정 수정 실패 - 이미 종료된 일정")
        void update_schedule_fail_already_closed() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;
            LocalDateTime eventDate = LocalDateTime.now().plusDays(7);
            LocalDateTime endDate = LocalDateTime.now().plusDays(8);

            Schedules schedule = schedule(scheduleId, clubId);
            ReflectionTestUtils.setField(schedule, "status", "CLOSED");
            ReflectionTestUtils.setField(schedule, "entryFee", BigDecimal.ZERO);

            ScheduleUpdateRequest request = new ScheduleUpdateRequest(
                    "수정된 일정",
                    eventDate,
                    endDate,
                    "신촌역",
                    "수정된 설명",
                    BigDecimal.ZERO
            );

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

            // when & then
            assertThatThrownBy(() -> scheduleService.updateSchedule(clubId, scheduleId, userId, request))
                    .isInstanceOf(ScheduleException.AlreadyClosed.class);
        }

        @Test
        @DisplayName("일정 수정 실패 - 종료일시가 시작일시보다 이전")
        void update_schedule_fail_invalid_date_range() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;
            LocalDateTime eventDate = LocalDateTime.now().plusDays(7);
            LocalDateTime endDate = LocalDateTime.now().plusDays(6); // 시작일시보다 이전

            Schedules schedule = schedule(scheduleId, clubId);
            ReflectionTestUtils.setField(schedule, "status", "OPEN");
            ReflectionTestUtils.setField(schedule, "entryFee", BigDecimal.ZERO);

            ScheduleUpdateRequest request = new ScheduleUpdateRequest(
                    "수정된 일정",
                    eventDate,
                    endDate,
                    "신촌역",
                    "수정된 설명",
                    BigDecimal.ZERO
            );

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

            // when & then
            assertThatThrownBy(() -> scheduleService.updateSchedule(clubId, scheduleId, userId, request))
                    .isInstanceOf(ScheduleException.InvalidDateRange.class);
        }
    }

    @Nested
    @DisplayName("일정 마감")
    class CloseSchedule {

        @Test
        @DisplayName("일정 마감 성공 - 참가비 없는 일정 (운영진 권한)")
        void close_schedule_success_no_fee() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;

            Schedules schedule = schedule(scheduleId, clubId);
            ReflectionTestUtils.setField(schedule, "status", "OPEN");
            ReflectionTestUtils.setField(schedule, "entryFee", BigDecimal.ZERO);

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(voteRepository.findByScheduleId(scheduleId)).willReturn(Optional.empty());

            // when
            scheduleService.closeSchedule(clubId, scheduleId, userId);

            // then
            assertThat(schedule.getStatus()).isEqualTo("CLOSED");
            then(clubsAuthorizationService).should(times(1)).assertAtLeastManager(clubId, userId);
        }

        @Test
        @DisplayName("일정 마감 성공 - 참가비 있는 일정 (총무 권한)")
        void close_schedule_success_with_fee() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;

            Schedules schedule = schedule(scheduleId, clubId);
            ReflectionTestUtils.setField(schedule, "status", "OPEN");
            ReflectionTestUtils.setField(schedule, "entryFee", BigDecimal.valueOf(10000));

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(voteRepository.findByScheduleId(scheduleId)).willReturn(Optional.empty());

            // when
            scheduleService.closeSchedule(clubId, scheduleId, userId);

            // then
            assertThat(schedule.getStatus()).isEqualTo("CLOSED");
            then(clubsAuthorizationService).should(times(1)).assertAtLeastAccountant(clubId, userId);
        }

        @Test
        @DisplayName("일정 마감 실패 - 이미 종료된 일정")
        void close_schedule_fail_already_closed() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;

            Schedules schedule = schedule(scheduleId, clubId);
            ReflectionTestUtils.setField(schedule, "status", "CLOSED");
            ReflectionTestUtils.setField(schedule, "entryFee", BigDecimal.ZERO);

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

            // when & then
            assertThatThrownBy(() -> scheduleService.closeSchedule(clubId, scheduleId, userId))
                    .isInstanceOf(ScheduleException.AlreadyClosed.class);
        }

        @Test
        @DisplayName("일정 마감 실패 - 이미 취소된 일정")
        void close_schedule_fail_already_cancelled() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;

            Schedules schedule = schedule(scheduleId, clubId);
            ReflectionTestUtils.setField(schedule, "status", "CANCELLED");
            ReflectionTestUtils.setField(schedule, "entryFee", BigDecimal.ZERO);

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

            // when & then
            assertThatThrownBy(() -> scheduleService.closeSchedule(clubId, scheduleId, userId))
                    .isInstanceOf(ScheduleException.AlreadyCancelled.class);
        }
    }

    @Nested
    @DisplayName("일정 취소")
    class CancelSchedule {

        @Test
        @DisplayName("일정 취소 성공 - 참가비 없는 일정")
        void cancel_schedule_success_no_fee() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;

            Schedules schedule = schedule(scheduleId, clubId);
            ReflectionTestUtils.setField(schedule, "status", "OPEN");
            ReflectionTestUtils.setField(schedule, "entryFee", BigDecimal.ZERO);

            ScheduleCancelRequest request = new ScheduleCancelRequest("우천으로 인한 취소");

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(voteRepository.findByScheduleId(scheduleId)).willReturn(Optional.empty());

            // when
            scheduleService.cancelSchedule(clubId, scheduleId, userId, request);

            // then
            assertThat(schedule.getStatus()).isEqualTo("CANCELLED");
            assertThat(schedule.getCancelReason()).isEqualTo("우천으로 인한 취소");
            then(clubsAuthorizationService).should(times(1)).assertAtLeastManager(clubId, userId);
        }

        @Test
        @DisplayName("일정 취소 성공 - 참가비 있는 일정 (총무 권한)")
        void cancel_schedule_success_with_fee() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;

            Schedules schedule = schedule(scheduleId, clubId);
            ReflectionTestUtils.setField(schedule, "status", "OPEN");
            ReflectionTestUtils.setField(schedule, "entryFee", BigDecimal.valueOf(10000));

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(voteRepository.findByScheduleId(scheduleId)).willReturn(Optional.empty());

            // when
            scheduleService.cancelSchedule(clubId, scheduleId, userId, null);

            // then
            assertThat(schedule.getStatus()).isEqualTo("CANCELLED");
            then(clubsAuthorizationService).should(times(1)).assertAtLeastAccountant(clubId, userId);
        }

        @Test
        @DisplayName("일정 취소 실패 - 이미 취소된 일정")
        void cancel_schedule_fail_already_cancelled() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;

            Schedules schedule = schedule(scheduleId, clubId);
            ReflectionTestUtils.setField(schedule, "status", "CANCELLED");
            ReflectionTestUtils.setField(schedule, "entryFee", BigDecimal.ZERO);

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

            // when & then
            assertThatThrownBy(() -> scheduleService.cancelSchedule(clubId, scheduleId, userId, null))
                    .isInstanceOf(ScheduleException.AlreadyCancelled.class);
        }
    }

    @Nested
    @DisplayName("정산 정보 수정")
    class UpdateSettlement {

        @Test
        @DisplayName("정산 정보 수정 성공")
        void update_settlement_success() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;

            Schedules schedule = schedule(scheduleId, clubId);
            ReflectionTestUtils.setField(schedule, "scheduleName", "테스트 일정");
            ReflectionTestUtils.setField(schedule, "entryFee", BigDecimal.valueOf(10000));

            ScheduleSettlementRequest request = new ScheduleSettlementRequest(
                    BigDecimal.valueOf(80000),
                    BigDecimal.valueOf(2000)
            );

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

            // when
            ScheduleResponse result = scheduleService.updateSettlement(clubId, scheduleId, userId, request);

            // then
            assertThat(result).isNotNull();
            then(clubsAuthorizationService).should(times(1)).assertAtLeastAccountant(clubId, userId);
        }

        @Test
        @DisplayName("정산 정보 수정 실패 - 일정 없음")
        void update_settlement_fail_not_found() {
            // given
            Long clubId = 1L;
            Long scheduleId = 999L;
            Long userId = 10L;

            ScheduleSettlementRequest request = new ScheduleSettlementRequest(
                    BigDecimal.valueOf(80000),
                    BigDecimal.valueOf(2000)
            );

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scheduleService.updateSettlement(clubId, scheduleId, userId, request))
                    .isInstanceOf(ScheduleException.NotFound.class);
        }
    }

    @Nested
    @DisplayName("참여자 목록 조회")
    class GetScheduleParticipants {

        @Test
        @DisplayName("참여자 목록 조회 성공")
        void get_schedule_participants_success() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;

            Schedules schedule = schedule(scheduleId, clubId);

            ScheduleParticipants participant1 = newEntity(ScheduleParticipants.class);
            ReflectionTestUtils.setField(participant1, "participantId", 1L);
            ReflectionTestUtils.setField(participant1, "scheduleId", scheduleId);
            ReflectionTestUtils.setField(participant1, "userId", 100L);
            ReflectionTestUtils.setField(participant1, "attendanceStatus", "ATTENDING");

            ScheduleParticipants participant2 = newEntity(ScheduleParticipants.class);
            ReflectionTestUtils.setField(participant2, "participantId", 2L);
            ReflectionTestUtils.setField(participant2, "scheduleId", scheduleId);
            ReflectionTestUtils.setField(participant2, "userId", 101L);
            ReflectionTestUtils.setField(participant2, "attendanceStatus", "NOT_ATTENDING");

            Users user1 = newEntity(Users.class);
            ReflectionTestUtils.setField(user1, "userId", 100L);
            ReflectionTestUtils.setField(user1, "realName", "홍길동");

            Users user2 = newEntity(Users.class);
            ReflectionTestUtils.setField(user2, "userId", 101L);
            ReflectionTestUtils.setField(user2, "realName", "김철수");

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(scheduleParticipantRepository.findByScheduleId(scheduleId))
                    .willReturn(List.of(participant1, participant2));
            given(userRepository.findAllById(List.of(100L, 101L)))
                    .willReturn(List.of(user1, user2));

            // when
            var result = scheduleService.getScheduleParticipants(clubId, scheduleId, userId);

            // then
            assertThat(result).hasSize(2);
            then(clubsAuthorizationService).should(times(1)).assertActiveMember(clubId, userId);
        }

        @Test
        @DisplayName("참여자 목록 조회 - 결과 없음")
        void get_schedule_participants_empty() {
            // given
            Long clubId = 1L;
            Long scheduleId = 1L;
            Long userId = 10L;

            Schedules schedule = schedule(scheduleId, clubId);

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(scheduleParticipantRepository.findByScheduleId(scheduleId))
                    .willReturn(List.of());

            // when
            var result = scheduleService.getScheduleParticipants(clubId, scheduleId, userId);

            // then
            assertThat(result).isEmpty();
        }
    }
}
