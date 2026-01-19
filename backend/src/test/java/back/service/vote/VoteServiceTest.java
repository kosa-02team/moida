package back.service.vote;

import back.domain.club.Clubs;
import back.domain.club.ClubMembers;
import back.domain.Users;
import back.domain.post.Posts;
import back.domain.schedule.Schedules;
import back.domain.vote.VoteOptions;
import back.domain.vote.VoteRecords;
import back.domain.vote.Votes;
import back.dto.vote.*;
import back.exception.VoteException;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import back.repository.post.PostRepository;
import back.repository.schedule.ScheduleRepository;
import back.repository.vote.VoteOptionRepository;
import back.repository.vote.VoteRecordRepository;
import back.repository.vote.VoteRepository;
import back.repository.UserRepository;
import back.service.club.ClubAuthService;
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
    private ClubMemberRepository clubMembersRepository;

    @Mock
    private ClubRepository clubsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubAuthService clubsAuthorizationService;

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
                    null, // scheduleId
                    null, // deadline
                    List.of( // options (최소 2개 필요)
                            new VoteOptionCreateRequest("옵션1", 1, null, null),
                            new VoteOptionCreateRequest("옵션2", 2, null, null)
                    )
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
            then(voteOptionRepository).should(times(2)).save(any(VoteOptions.class)); // GENERAL 타입은 2개 옵션 생성
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
                    scheduleId, // scheduleId
                    null, // deadline
                    null  // options (ATTENDANCE는 null)
            );

            Schedules schedule = schedule(scheduleId, clubId);
            Clubs clubRef = club(clubId, 1L);
            Users userRef = user(userId);

            Votes savedVote = newEntity(Votes.class);
            ReflectionTestUtils.setField(savedVote, "voteId", 1L);
            ReflectionTestUtils.setField(savedVote, "voteType", "ATTENDANCE");
            ReflectionTestUtils.setField(savedVote, "scheduleId", scheduleId);
            ReflectionTestUtils.setField(savedVote, "status", "OPEN");

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(clubsRepository.getReferenceById(clubId)).willReturn(clubRef);
            given(userRepository.getReferenceById(userId)).willReturn(userRef);
            given(voteRepository.save(any(Votes.class))).willReturn(savedVote);

            // when
            VoteResponse result = voteService.createVote(clubId, userId, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.voteType()).isEqualTo("ATTENDANCE");
            assertThat(result.scheduleId()).isEqualTo(scheduleId);
            assertThat(result.postId()).isNull(); // ATTENDANCE 타입은 postId가 null

            then(scheduleRepository).should(times(1)).findById(scheduleId);
            then(postRepository).shouldHaveNoInteractions(); // ATTENDANCE 타입은 Posts 생성 안함
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
                    null, // scheduleId 없음
                    null, // deadline
                    null  // options
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
                    scheduleId, // scheduleId
                    null, // deadline
                    null  // options (ATTENDANCE는 null)
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

            Clubs club = club(clubId, 1L);
            Posts post = Posts.vote(club, user(creatorId), null, "제목", "설명");
            ReflectionTestUtils.setField(post, "postId", 1L);

            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));
            given(postRepository.findById(1L)).willReturn(Optional.of(post));

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

    @Nested
    @DisplayName("투표 상세 조회")
    class GetVoteById {

        @Test
        @DisplayName("GENERAL 타입 투표 상세 조회 성공")
        void get_general_vote_by_id_success() {
            // given
            Long clubId = 1L;
            Long voteId = 1L;
            Long userId = 10L;

            Clubs club = club(clubId, 1L);
            Posts post = Posts.vote(club, user(userId), null, "투표 제목", "설명");
            ReflectionTestUtils.setField(post, "postId", 1L);

            Votes vote = newEntity(Votes.class);
            ReflectionTestUtils.setField(vote, "voteId", voteId);
            ReflectionTestUtils.setField(vote, "voteType", "GENERAL");
            ReflectionTestUtils.setField(vote, "postId", 1L);
            ReflectionTestUtils.setField(vote, "creatorId", userId);
            ReflectionTestUtils.setField(vote, "title", "투표 제목");
            ReflectionTestUtils.setField(vote, "status", "OPEN");

            VoteOptions option1 = newEntity(VoteOptions.class);
            ReflectionTestUtils.setField(option1, "optionId", 100L);
            ReflectionTestUtils.setField(option1, "voteId", voteId);
            ReflectionTestUtils.setField(option1, "optionText", "옵션1");
            ReflectionTestUtils.setField(option1, "optionOrder", 1);

            VoteOptions option2 = newEntity(VoteOptions.class);
            ReflectionTestUtils.setField(option2, "optionId", 101L);
            ReflectionTestUtils.setField(option2, "voteId", voteId);
            ReflectionTestUtils.setField(option2, "optionText", "옵션2");
            ReflectionTestUtils.setField(option2, "optionOrder", 2);

            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));
            given(postRepository.findById(1L)).willReturn(Optional.of(post));
            given(voteOptionRepository.findByVoteIdOrderByOptionOrderAsc(voteId))
                    .willReturn(List.of(option1, option2));
            given(voteRecordRepository.countByOptionId(100L)).willReturn(5L);
            given(voteRecordRepository.countByOptionId(101L)).willReturn(3L);
            given(voteRecordRepository.findByVoteIdAndUserId(voteId, userId)).willReturn(List.of());

            // when
            VoteDetailResponse result = voteService.getVoteById(clubId, voteId, userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.voteId()).isEqualTo(voteId);
            assertThat(result.title()).isEqualTo("투표 제목");
            assertThat(result.options()).hasSize(2);
            then(clubsAuthorizationService).should(times(1)).assertActiveMember(clubId, userId);
        }

        @Test
        @DisplayName("ATTENDANCE 타입 투표 상세 조회 성공")
        void get_attendance_vote_by_id_success() {
            // given
            Long clubId = 1L;
            Long voteId = 1L;
            Long userId = 10L;
            Long scheduleId = 100L;

            Schedules schedule = schedule(scheduleId, clubId);

            Votes vote = newEntity(Votes.class);
            ReflectionTestUtils.setField(vote, "voteId", voteId);
            ReflectionTestUtils.setField(vote, "voteType", "ATTENDANCE");
            ReflectionTestUtils.setField(vote, "scheduleId", scheduleId);
            ReflectionTestUtils.setField(vote, "creatorId", userId);
            ReflectionTestUtils.setField(vote, "title", "참석 투표");
            ReflectionTestUtils.setField(vote, "status", "OPEN");

            VoteOptions attendOption = newEntity(VoteOptions.class);
            ReflectionTestUtils.setField(attendOption, "optionId", 100L);
            ReflectionTestUtils.setField(attendOption, "voteId", voteId);
            ReflectionTestUtils.setField(attendOption, "optionText", "참석");
            ReflectionTestUtils.setField(attendOption, "optionOrder", 1);

            VoteOptions absentOption = newEntity(VoteOptions.class);
            ReflectionTestUtils.setField(absentOption, "optionId", 101L);
            ReflectionTestUtils.setField(absentOption, "voteId", voteId);
            ReflectionTestUtils.setField(absentOption, "optionText", "불참");
            ReflectionTestUtils.setField(absentOption, "optionOrder", 2);

            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(voteOptionRepository.findByVoteIdOrderByOptionOrderAsc(voteId))
                    .willReturn(List.of(attendOption, absentOption));
            given(voteRecordRepository.countByOptionId(100L)).willReturn(8L);
            given(voteRecordRepository.countByOptionId(101L)).willReturn(2L);
            given(voteRecordRepository.findByVoteIdAndUserId(voteId, userId)).willReturn(List.of());

            // when
            VoteDetailResponse result = voteService.getVoteById(clubId, voteId, userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.voteType()).isEqualTo("ATTENDANCE");
            assertThat(result.scheduleId()).isEqualTo(scheduleId);
            assertThat(result.options()).hasSize(2);
        }

        @Test
        @DisplayName("투표 조회 실패 - 투표 없음")
        void get_vote_by_id_fail_not_found() {
            // given
            Long clubId = 1L;
            Long voteId = 999L;
            Long userId = 10L;

            given(voteRepository.findById(voteId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> voteService.getVoteById(clubId, voteId, userId))
                    .isInstanceOf(VoteException.NotFound.class);
        }

        @Test
        @DisplayName("투표 조회 실패 - 다른 모임의 투표")
        void get_vote_by_id_fail_club_mismatch() {
            // given
            Long clubId = 1L;
            Long voteId = 1L;
            Long userId = 10L;
            Long otherClubId = 999L;

            Clubs otherClub = club(otherClubId, 1L);
            Posts post = Posts.vote(otherClub, user(userId), null, "제목", "설명");
            ReflectionTestUtils.setField(post, "postId", 1L);

            Votes vote = newEntity(Votes.class);
            ReflectionTestUtils.setField(vote, "voteId", voteId);
            ReflectionTestUtils.setField(vote, "voteType", "GENERAL");
            ReflectionTestUtils.setField(vote, "postId", 1L);

            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));
            given(postRepository.findById(1L)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> voteService.getVoteById(clubId, voteId, userId))
                    .isInstanceOf(VoteException.ClubMismatch.class);
        }
    }

    @Nested
    @DisplayName("투표 목록 조회")
    class GetVotesByClubId {

        @Test
        @DisplayName("모임의 전체 투표 목록 조회 성공")
        void get_votes_by_club_id_success() {
            // given
            Long clubId = 1L;
            Long userId = 10L;

            Clubs club = club(clubId, 1L);
            Posts post1 = Posts.vote(club, user(userId), null, "일반 투표", "설명");
            ReflectionTestUtils.setField(post1, "postId", 1L);

            Votes generalVote = newEntity(Votes.class);
            ReflectionTestUtils.setField(generalVote, "voteId", 1L);
            ReflectionTestUtils.setField(generalVote, "voteType", "GENERAL");
            ReflectionTestUtils.setField(generalVote, "postId", 1L);
            ReflectionTestUtils.setField(generalVote, "title", "일반 투표");
            ReflectionTestUtils.setField(generalVote, "status", "OPEN");

            Schedules schedule = schedule(100L, clubId);
            Votes attendanceVote = newEntity(Votes.class);
            ReflectionTestUtils.setField(attendanceVote, "voteId", 2L);
            ReflectionTestUtils.setField(attendanceVote, "voteType", "ATTENDANCE");
            ReflectionTestUtils.setField(attendanceVote, "scheduleId", 100L);
            ReflectionTestUtils.setField(attendanceVote, "title", "참석 투표");
            ReflectionTestUtils.setField(attendanceVote, "status", "OPEN");

            given(postRepository.findByClub_ClubIdAndCategoryAndDeletedAtIsNull(eq(clubId), any()))
                    .willReturn(List.of(post1));
            given(scheduleRepository.findByClubId(clubId)).willReturn(List.of(schedule));
            given(voteRepository.findByPostIdIn(List.of(1L))).willReturn(List.of(generalVote));
            given(voteRepository.findByScheduleIdIn(List.of(100L))).willReturn(List.of(attendanceVote));
            given(voteRecordRepository.countDistinctUsersByVoteId(1L)).willReturn(5L);
            given(voteRecordRepository.countDistinctUsersByVoteId(2L)).willReturn(10L);

            // when
            List<VoteListResponse> result = voteService.getVotesByClubId(clubId, userId);

            // then
            assertThat(result).hasSize(2);
            then(clubsAuthorizationService).should(times(1)).assertActiveMember(clubId, userId);
        }

        @Test
        @DisplayName("투표 목록 조회 - 결과 없음")
        void get_votes_by_club_id_empty() {
            // given
            Long clubId = 1L;
            Long userId = 10L;

            given(postRepository.findByClub_ClubIdAndCategoryAndDeletedAtIsNull(eq(clubId), any()))
                    .willReturn(List.of());
            given(scheduleRepository.findByClubId(clubId)).willReturn(List.of());

            // when
            List<VoteListResponse> result = voteService.getVotesByClubId(clubId, userId);

            // then
            assertThat(result).isEmpty();
        }
    }
}
