package back.service.club;

import back.domain.club.Clubs;
import back.exception.ClubException;
import back.domain.club.ClubMembers;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ClubAuthServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @InjectMocks
    private ClubAuthService clubAuthService;

    private Clubs createClub(Long clubId, Long ownerId, String visibility) {
        Clubs club = new Clubs("테스트 모임", ownerId);
        ReflectionTestUtils.setField(club, "clubId", clubId);
        if (visibility != null) {
            ReflectionTestUtils.setField(club, "visibility", Clubs.Visibility.valueOf(visibility));
        }
        return club;
    }

    private ClubMembers createMember(Long clubId, Long userId, ClubMembers.Status status, ClubMembers.Role role) {
        ClubMembers member = ClubMembers.builder()
                .clubId(clubId)
                .userId(userId)
                .nickname("테스트닉네임")
                .build();
        ReflectionTestUtils.setField(member, "memberId", 1L);
        ReflectionTestUtils.setField(member, "status", status);
        if (role != null) {
            ReflectionTestUtils.setField(member, "role", role);
        }
        return member;
    }

    @Nested
    @DisplayName("활성 멤버 확인")
    class AssertActiveMember {

        @Test
        @DisplayName("활성 멤버 확인 성공")
        void assert_active_member_success() {
            // given
            Long clubId = 1L;
            Long userId = 10L;

            given(clubMemberRepository.existsByClubIdAndUserIdAndStatus(clubId, userId, ClubMembers.Status.ACTIVE))
                    .willReturn(true);

            // when & then
            clubAuthService.assertActiveMember(clubId, userId);

            then(clubMemberRepository).should(times(1))
                    .existsByClubIdAndUserIdAndStatus(clubId, userId, ClubMembers.Status.ACTIVE);
        }

        @Test
        @DisplayName("활성 멤버 확인 실패 - 활성 멤버가 아님")
        void assert_active_member_fail_not_active() {
            // given
            Long clubId = 1L;
            Long userId = 10L;

            given(clubMemberRepository.existsByClubIdAndUserIdAndStatus(clubId, userId, ClubMembers.Status.ACTIVE))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> clubAuthService.assertActiveMember(clubId, userId))
                    .isInstanceOf(ClubException.AuthNotActive.class);
        }
    }

    @Nested
    @DisplayName("운영진 이상 권한 확인")
    class AssertAtLeastManager {

        @Test
        @DisplayName("운영진 이상 권한 확인 성공 - 모임장")
        void assert_at_least_manager_success_owner() {
            // given
            Long clubId = 1L;
            Long ownerId = 10L;

            Clubs club = createClub(clubId, ownerId, "PUBLIC");
            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));

            // when & then
            clubAuthService.assertAtLeastManager(clubId, ownerId);

            then(clubRepository).should(times(1)).findById(clubId);
            then(clubMemberRepository).should(never()).findActiveRole(any(), any());
        }

        @Test
        @DisplayName("운영진 이상 권한 확인 성공 - 총무")
        void assert_at_least_manager_success_accountant() {
            // given
            Long clubId = 1L;
            Long userId = 10L;
            Long ownerId = 999L;

            Clubs club = createClub(clubId, ownerId, "PUBLIC");
            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubMemberRepository.findActiveRole(clubId, userId))
                    .willReturn(Optional.of(ClubMembers.Role.ACCOUNTANT));

            // when & then
            clubAuthService.assertAtLeastManager(clubId, userId);

            then(clubRepository).should(times(1)).findById(clubId);
            then(clubMemberRepository).should(times(1)).findActiveRole(clubId, userId);
        }

        @Test
        @DisplayName("운영진 이상 권한 확인 성공 - 운영진")
        void assert_at_least_manager_success_staff() {
            // given
            Long clubId = 1L;
            Long userId = 10L;
            Long ownerId = 999L;

            Clubs club = createClub(clubId, ownerId, "PUBLIC");
            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubMemberRepository.findActiveRole(clubId, userId))
                    .willReturn(Optional.of(ClubMembers.Role.STAFF));

            // when & then
            clubAuthService.assertAtLeastManager(clubId, userId);

            then(clubRepository).should(times(1)).findById(clubId);
        }

        @Test
        @DisplayName("운영진 이상 권한 확인 실패 - 일반 멤버")
        void assert_at_least_manager_fail_member() {
            // given
            Long clubId = 1L;
            Long userId = 10L;
            Long ownerId = 999L;

            Clubs club = createClub(clubId, ownerId, "PUBLIC");
            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubMemberRepository.findActiveRole(clubId, userId))
                    .willReturn(Optional.of(ClubMembers.Role.MEMBER));

            // when & then
            assertThatThrownBy(() -> clubAuthService.assertAtLeastManager(clubId, userId))
                    .isInstanceOf(ClubException.AuthStaffRequired.class);
        }

        @Test
        @DisplayName("운영진 이상 권한 확인 실패 - 모임 없음")
        void assert_at_least_manager_fail_club_not_found() {
            // given
            Long clubId = 999L;
            Long userId = 10L;

            given(clubRepository.findById(clubId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clubAuthService.assertAtLeastManager(clubId, userId))
                    .isInstanceOf(ClubException.NotFound.class);
        }
    }

    @Nested
    @DisplayName("총무 이상 권한 확인")
    class AssertAtLeastAccountant {

        @Test
        @DisplayName("총무 이상 권한 확인 성공 - 모임장")
        void assert_at_least_accountant_success_owner() {
            // given
            Long clubId = 1L;
            Long ownerId = 10L;

            Clubs club = createClub(clubId, ownerId, "PUBLIC");
            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));

            // when & then
            clubAuthService.assertAtLeastAccountant(clubId, ownerId);

            then(clubRepository).should(times(1)).findById(clubId);
            then(clubMemberRepository).should(never()).findActiveRole(any(), any());
        }

        @Test
        @DisplayName("총무 이상 권한 확인 성공 - 총무")
        void assert_at_least_accountant_success_accountant() {
            // given
            Long clubId = 1L;
            Long userId = 10L;
            Long ownerId = 999L;

            Clubs club = createClub(clubId, ownerId, "PUBLIC");
            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubMemberRepository.findActiveRole(clubId, userId))
                    .willReturn(Optional.of(ClubMembers.Role.ACCOUNTANT));

            // when & then
            clubAuthService.assertAtLeastAccountant(clubId, userId);

            then(clubRepository).should(times(1)).findById(clubId);
        }

        @Test
        @DisplayName("총무 이상 권한 확인 실패 - 운영진")
        void assert_at_least_accountant_fail_staff() {
            // given
            Long clubId = 1L;
            Long userId = 10L;
            Long ownerId = 999L;

            Clubs club = createClub(clubId, ownerId, "PUBLIC");
            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubMemberRepository.findActiveRole(clubId, userId))
                    .willReturn(Optional.of(ClubMembers.Role.STAFF));

            // when & then
            assertThatThrownBy(() -> clubAuthService.assertAtLeastAccountant(clubId, userId))
                    .isInstanceOf(ClubException.AuthAccountantRequired.class);
        }
    }

    @Nested
    @DisplayName("게시글 조회 권한 확인")
    class ValidateAndGetClubForReadPosts {

        @Test
        @DisplayName("공개 모임 조회 성공 - 비로그인")
        void validate_read_posts_success_public_no_login() {
            // given
            Long clubId = 1L;
            Long viewerId = null;

            Clubs club = createClub(clubId, 1L, "PUBLIC");
            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));

            // when & then
            clubAuthService.validateAndGetClubForReadPosts(clubId, viewerId);

            then(clubRepository).should(times(1)).findById(clubId);
            then(clubMemberRepository).should(never()).existsByClubIdAndUserIdAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("비공개 모임 조회 성공 - 활성 멤버")
        void validate_read_posts_success_private_active_member() {
            // given
            Long clubId = 1L;
            Long viewerId = 10L;

            Clubs club = createClub(clubId, 1L, "PRIVATE");
            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubMemberRepository.existsByClubIdAndUserIdAndStatus(clubId, viewerId, ClubMembers.Status.ACTIVE))
                    .willReturn(true);

            // when & then
            clubAuthService.validateAndGetClubForReadPosts(clubId, viewerId);

            then(clubRepository).should(times(1)).findById(clubId);
            then(clubMemberRepository).should(times(1))
                    .existsByClubIdAndUserIdAndStatus(clubId, viewerId, ClubMembers.Status.ACTIVE);
        }

        @Test
        @DisplayName("비공개 모임 조회 실패 - 비로그인")
        void validate_read_posts_fail_private_no_login() {
            // given
            Long clubId = 1L;
            Long viewerId = null;

            Clubs club = createClub(clubId, 1L, "PRIVATE");
            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));

            // when & then
            assertThatThrownBy(() -> clubAuthService.validateAndGetClubForReadPosts(clubId, viewerId))
                    .isInstanceOf(ClubException.AuthLoginRequired.class);
        }
    }

    @Nested
    @DisplayName("게시글 수정 권한 확인")
    class ValidateAndGetClubForUpdatePosts {

        @Test
        @DisplayName("게시글 수정 권한 확인 실패 - 비로그인")
        void validate_update_posts_fail_no_login() {
            // given
            Long clubId = 1L;
            Long updateId = null;

            // when & then
            assertThatThrownBy(() -> clubAuthService.validateAndGetClubForUpdatePosts(clubId, updateId))
                    .isInstanceOf(ClubException.AuthLoginRequired.class);
        }
    }
}
