package back.service.vote;

import back.domain.Clubs;
import back.domain.ClubMembers;
import back.domain.Users;
import back.domain.posts.Posts;
import back.domain.schedule.Schedules;
import back.domain.vote.VoteOptions;
import back.domain.vote.VoteRecords;
import back.domain.vote.Votes;
import back.dto.vote.VoteAnswerRequest;
import back.dto.vote.VoteCreateRequest;
import back.dto.vote.VoteResponse;
import back.exception.ResourceException;
import back.exception.VoteException;
import back.repository.clubs.ClubMembersRepository;
import back.repository.clubs.ClubsRepository;
import back.repository.posts.PostRepository;
import back.repository.schedule.ScheduleRepository;
import back.repository.vote.VoteOptionRepository;
import back.repository.vote.VoteRecordRepository;
import back.repository.vote.VoteRepository;
import back.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private VoteOptionRepository voteOptionRepository;

    @Mock
    private VoteRecordRepository voteRecordRepository;

    @Mock
    private ClubMembersRepository clubMembersRepository;

    @Mock
    private ClubsRepository clubsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VoteService voteService;

    private static <T> T newEntity(Class<T> type) {
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Clubs club(Long id, Long ownerId) {
        Clubs c = newEntity(Clubs.class);
        ReflectionTestUtils.setField(c, "clubId", id);
        ReflectionTestUtils.setField(c, "ownerId", ownerId);
        return c;
    }

    private Users user(Long id) {
        Users u = newEntity(Users.class);
        ReflectionTestUtils.setField(u, "userId", id);
        return u;
    }

    private Schedules schedule(Long id, Long clubId) {
        Schedules s = newEntity(Schedules.class);
        ReflectionTestUtils.setField(s, "scheduleId", id);
        ReflectionTestUtils.setField(s, "clubId", clubId);
        ReflectionTestUtils.setField(s, "eventDate", LocalDateTime.now().plusDays(7));
        ReflectionTestUtils.setField(s, "location", "강남역");
        return s;
    }

    @Nested
    @DisplayName("투표 생성")
    class CreateVote {

        @Test
        @DisplayName("GENERAL 타입 투표 생성 성공")
        void create_general_vote_success() {
            // given
            Long clubId = 1L;
            Long userId = 10L;

            VoteCreateRequest request = new VoteCreateRequest(
                    "GENERAL",
                    "투표 제목",
                    "투표 설명",
                    false,
                    false,
                    null
            );

            Clubs clubRef = club(clubId, 1L);
            Users userRef = user(userId);

            Posts savedPost = Posts.vote(clubRef, userRef, null, request.title(), request.description());
            ReflectionTestUtils.setField(savedPost, "postId", 1L);

            Votes savedVote = newEntity(Votes.class);
            ReflectionTestUtils.setField(savedVote, "voteId", 1L);
            ReflectionTestUtils.setField(savedVote, "voteType", "GENERAL");
            ReflectionTestUtils.setField(savedVote, "status", "OPEN");

            given(clubsRepository.getReferenceById(clubId)).willReturn(clubRef);
            given(userRepository.getReferenceById(userId)).willReturn(userRef);
            given(postRepository.save(any(Posts.class))).willReturn(savedPost);
            given(voteRepository.save(any(Votes.class))).willReturn(savedVote);

            // when
            VoteResponse result = voteService.createVote(clubId, userId, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.voteId()).isEqualTo(1L);
            assertThat(result.voteType()).isEqualTo("GENERAL");
            assertThat(result.postId()).isEqualTo(1L);

            then(clubsRepository).should(times(1)).getReferenceById(clubId);
            then(userRepository).should(times(1)).getReferenceById(userId);
            then(postRepository).should(times(1)).save(any(Posts.class));
            then(voteRepository).should(times(1)).save(any(Votes.class));
            then(voteOptionRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("ATTENDANCE 타입 투표 생성 성공 - VoteOptions 자동 생성")
        void create_attendance_vote_success() {
            // given
            Long clubId = 1L;
            Long userId = 10L;
            Long scheduleId = 100L;

            VoteCreateRequest request = new VoteCreateRequest(
                    "ATTENDANCE",
                    "참석 투표",
                    "참석 여부를 확인합니다",
                    false,
                    false,
                    scheduleId
            );

            Schedules schedule = schedule(scheduleId, clubId);
            Clubs clubRef = club(clubId, 1L);
            Users userRef = user(userId);

            Posts savedPost = Posts.vote(clubRef, userRef, schedule, request.title(), request.description());
            ReflectionTestUtils.setField(savedPost, "postId", 1L);

            Votes savedVote = newEntity(Votes.class);
            ReflectionTestUtils.setField(savedVote, "voteId", 1L);
            ReflectionTestUtils.setField(savedVote, "voteType", "ATTENDANCE");
            ReflectionTestUtils.setField(savedVote, "scheduleId", scheduleId);
            ReflectionTestUtils.setField(savedVote, "status", "OPEN");

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(clubsRepository.getReferenceById(clubId)).willReturn(clubRef);
            given(userRepository.getReferenceById(userId)).willReturn(userRef);
            given(scheduleRepository.getReferenceById(scheduleId)).willReturn(schedule);
            given(postRepository.save(any(Posts.class))).willReturn(savedPost);
            given(voteRepository.save(any(Votes.class))).willReturn(savedVote);

            // when
            VoteResponse result = voteService.createVote(clubId, userId, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.voteType()).isEqualTo("ATTENDANCE");
            assertThat(result.scheduleId()).isEqualTo(scheduleId);

            then(scheduleRepository).should(times(1)).findById(scheduleId);
            then(postRepository).should(times(1)).save(any(Posts.class));
            then(voteRepository).should(times(1)).save(any(Votes.class));
            then(voteOptionRepository).should(times(2)).save(any(VoteOptions.class));
        }

        @Test
        @DisplayName("ATTENDANCE 타입 투표 생성 실패 - scheduleId 없음")
        void create_attendance_vote_fail_no_schedule_id() {
            // given
            Long clubId = 1L;
            Long userId = 10L;

            VoteCreateRequest request = new VoteCreateRequest(
                    "ATTENDANCE",
                    "참석 투표",
                    "설명",
                    false,
                    false,
                    null // scheduleId 없음
            );

            // when & then
            assertThatThrownBy(() -> voteService.createVote(clubId, userId, request))
                    .isInstanceOf(VoteException.ScheduleIdRequired.class);

            then(postRepository).shouldHaveNoInteractions();
            then(voteRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("ATTENDANCE 타입 투표 생성 실패 - 일정이 다른 모임에 속함")
        void create_attendance_vote_fail_club_mismatch() {
            // given
            Long clubId = 1L;
            Long userId = 10L;
            Long scheduleId = 100L;
            Long otherClubId = 999L;

            VoteCreateRequest request = new VoteCreateRequest(
                    "ATTENDANCE",
                    "참석 투표",
                    "설명",
                    false,
                    false,
                    scheduleId
            );

            Schedules schedule = schedule(scheduleId, otherClubId); // 다른 모임

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

            // when & then
            assertThatThrownBy(() -> voteService.createVote(clubId, userId, request))
                    .isInstanceOf(VoteException.ClubMismatch.class);

            then(postRepository).shouldHaveNoInteractions();
            then(voteRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("투표 종료")
    class CloseVote {

        @Test
        @DisplayName("GENERAL 타입 투표 종료 성공 - 생성자만 가능")
        void close_general_vote_success() {
            // given
            Long clubId = 1L;
            Long voteId = 1L;
            Long creatorId = 10L;

            Clubs club = club(clubId, 1L);
            Posts post = Posts.vote(club, user(creatorId), null, "제목", "설명");
            ReflectionTestUtils.setField(post, "postId", 1L);

            Votes vote = newEntity(Votes.class);
            ReflectionTestUtils.setField(vote, "voteId", voteId);
            ReflectionTestUtils.setField(vote, "voteType", "GENERAL");
            ReflectionTestUtils.setField(vote, "postId", 1L);
            ReflectionTestUtils.setField(vote, "creatorId", creatorId);
            ReflectionTestUtils.setField(vote, "status", "OPEN");

            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));
            given(postRepository.findById(1L)).willReturn(Optional.of(post));

            // when
            voteService.closeVote(clubId, voteId, creatorId);

            // then
            assertThat(vote.getStatus()).isEqualTo("CLOSED");
            then(voteRepository).should(times(1)).save(vote);
        }

        @Test
        @DisplayName("GENERAL 타입 투표 종료 실패 - 생성자가 아님")
        void close_general_vote_fail_not_creator() {
            // given
            Long clubId = 1L;
            Long voteId = 1L;
            Long creatorId = 10L;
            Long otherUserId = 20L;

            Clubs club = club(clubId, 1L);
            Posts post = Posts.vote(club, user(creatorId), null, "제목", "설명");
            ReflectionTestUtils.setField(post, "postId", 1L);

            Votes vote = newEntity(Votes.class);
            ReflectionTestUtils.setField(vote, "voteId", voteId);
            ReflectionTestUtils.setField(vote, "voteType", "GENERAL");
            ReflectionTestUtils.setField(vote, "postId", 1L);
            ReflectionTestUtils.setField(vote, "creatorId", creatorId);
            ReflectionTestUtils.setField(vote, "status", "OPEN");

            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));
            given(postRepository.findById(1L)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> voteService.closeVote(clubId, voteId, otherUserId))
                    .isInstanceOf(VoteException.CreatorOnly.class);

            assertThat(vote.getStatus()).isEqualTo("OPEN");
        }

        @Test
        @DisplayName("투표 종료 실패 - 이미 종료된 투표")
        void close_vote_fail_already_closed() {
            // given
            Long clubId = 1L;
            Long voteId = 1L;
            Long creatorId = 10L;

            Votes vote = newEntity(Votes.class);
            ReflectionTestUtils.setField(vote, "voteId", voteId);
            ReflectionTestUtils.setField(vote, "voteType", "GENERAL");
            ReflectionTestUtils.setField(vote, "postId", 1L);
            ReflectionTestUtils.setField(vote, "creatorId", creatorId);
            ReflectionTestUtils.setField(vote, "status", "CLOSED");

            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));

            // when & then
            assertThatThrownBy(() -> voteService.closeVote(clubId, voteId, creatorId))
                    .isInstanceOf(VoteException.AlreadyClosed.class);
        }
    }

    @Nested
    @DisplayName("투표 참여")
    class AnswerVote {

        @Test
        @DisplayName("GENERAL 타입 투표 참여 성공")
        void answer_general_vote_success() {
            // given
            Long clubId = 1L;
            Long voteId = 1L;
            Long userId = 10L;
            Long optionId = 100L;

            VoteAnswerRequest request = new VoteAnswerRequest(List.of(optionId));

            Votes vote = newEntity(Votes.class);
            ReflectionTestUtils.setField(vote, "voteId", voteId);
            ReflectionTestUtils.setField(vote, "voteType", "GENERAL");
            ReflectionTestUtils.setField(vote, "postId", 1L);
            ReflectionTestUtils.setField(vote, "status", "OPEN");
            ReflectionTestUtils.setField(vote, "allowMultiple", false);

            Clubs club = club(clubId, 1L);
            Posts post = Posts.vote(club, user(userId), null, "제목", "설명");
            ReflectionTestUtils.setField(post, "postId", 1L);

            VoteOptions option = newEntity(VoteOptions.class);
            ReflectionTestUtils.setField(option, "optionId", optionId);
            ReflectionTestUtils.setField(option, "voteId", voteId);

            given(clubMembersRepository.existsByClubIdAndUserIdAndStatus(
                    clubId, userId, ClubMembers.Status.ACTIVE))
                    .willReturn(true);
            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));
            given(postRepository.findById(1L)).willReturn(Optional.of(post));
            given(voteOptionRepository.findAllById(List.of(optionId))).willReturn(List.of(option));
            given(voteRecordRepository.findByVoteIdAndUserId(voteId, userId)).willReturn(List.of());

            // when
            voteService.answerVote(clubId, voteId, userId, request);

            // then
            then(voteRecordRepository).should(times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("ATTENDANCE 타입 투표 참여 성공 - 기존 기록 변경")
        void answer_attendance_vote_success() {
            // given
            Long clubId = 1L;
            Long voteId = 1L;
            Long userId = 10L;
            Long scheduleId = 100L;
            Long optionId = 100L;

            VoteAnswerRequest request = new VoteAnswerRequest(List.of(optionId));

            Schedules schedule = schedule(scheduleId, clubId);

            Votes vote = newEntity(Votes.class);
            ReflectionTestUtils.setField(vote, "voteId", voteId);
            ReflectionTestUtils.setField(vote, "voteType", "ATTENDANCE");
            ReflectionTestUtils.setField(vote, "scheduleId", scheduleId);
            ReflectionTestUtils.setField(vote, "status", "OPEN");

            VoteOptions option = newEntity(VoteOptions.class);
            ReflectionTestUtils.setField(option, "optionId", optionId);
            ReflectionTestUtils.setField(option, "voteId", voteId);

            VoteRecords existingRecord = newEntity(VoteRecords.class);
            ReflectionTestUtils.setField(existingRecord, "recordId", 1L);
            ReflectionTestUtils.setField(existingRecord, "voteId", voteId);
            ReflectionTestUtils.setField(existingRecord, "optionId", 99L);
            ReflectionTestUtils.setField(existingRecord, "userId", userId);

            given(clubMembersRepository.existsByClubIdAndUserIdAndStatus(
                    clubId, userId, ClubMembers.Status.ACTIVE))
                    .willReturn(true);
            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(voteOptionRepository.findAllById(List.of(optionId))).willReturn(List.of(option));
            given(voteRecordRepository.findByVoteIdAndUserId(voteId, userId))
                    .willReturn(List.of(existingRecord));

            // when
            voteService.answerVote(clubId, voteId, userId, request);

            // then
            then(voteRecordRepository).should(times(1)).deleteAll(List.of(existingRecord));
            then(voteRecordRepository).should(times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("투표 참여 실패 - 활성 멤버가 아님")
        void answer_vote_fail_not_active_member() {
            // given
            Long clubId = 1L;
            Long voteId = 1L;
            Long userId = 10L;

            VoteAnswerRequest request = new VoteAnswerRequest(List.of(100L));

            given(clubMembersRepository.existsByClubIdAndUserIdAndStatus(
                    clubId, userId, ClubMembers.Status.ACTIVE))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> voteService.answerVote(clubId, voteId, userId, request))
                    .isInstanceOf(VoteException.MemberOnly.class);

            then(voteRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("투표 참여 실패 - 이미 종료된 투표")
        void answer_vote_fail_already_closed() {
            // given
            Long clubId = 1L;
            Long voteId = 1L;
            Long userId = 10L;

            VoteAnswerRequest request = new VoteAnswerRequest(List.of(100L));

            Votes vote = newEntity(Votes.class);
            ReflectionTestUtils.setField(vote, "voteId", voteId);
            ReflectionTestUtils.setField(vote, "voteType", "GENERAL");
            ReflectionTestUtils.setField(vote, "postId", 1L);
            ReflectionTestUtils.setField(vote, "status", "CLOSED");

            given(clubMembersRepository.existsByClubIdAndUserIdAndStatus(
                    clubId, userId, ClubMembers.Status.ACTIVE))
                    .willReturn(true);
            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));

            Clubs club = club(clubId, 1L);
            Posts post = Posts.vote(club, user(userId), null, "제목", "설명");
            ReflectionTestUtils.setField(post, "postId", 1L);
            given(postRepository.findById(1L)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> voteService.answerVote(clubId, voteId, userId, request))
                    .isInstanceOf(VoteException.AlreadyClosed.class);
        }
    }
}
