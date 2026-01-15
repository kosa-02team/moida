package back.service.schedule;

import back.domain.Users;
import back.domain.schedule.ScheduleParticipants;
import back.domain.schedule.Schedules;
import back.domain.vote.VoteOptions;
import back.domain.vote.Votes;
import back.dto.schedule.ScheduleCreateRequest;
import back.dto.schedule.ScheduleResponse;
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
}
