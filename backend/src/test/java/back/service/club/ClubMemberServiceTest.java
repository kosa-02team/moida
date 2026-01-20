package back.service.club;

import back.domain.club.ClubMembers;
import back.domain.club.Clubs;
import back.dto.club.ClubMemberRequest;
import back.dto.club.ClubMemberResponse;
import back.exception.ClubException;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ClubMemberServiceTest {

        @Mock
        private ClubMemberRepository clubMemberRepository;

        @Mock
        private ClubRepository clubRepository;

        @Mock
        private ApplicationEventPublisher eventPublisher;

        @InjectMocks
        private ClubMemberService clubMemberService;

        private Clubs createClub(Long clubId, Long ownerId) {
                Clubs club = new Clubs("테스트모임", ownerId);
                ReflectionTestUtils.setField(club, "clubId", clubId);
                return club;
        }

        @Nested
        @DisplayName("모임 가입")
        class JoinClub {

                @Test
                @DisplayName("새로운 회원 가입 성공")
                void join_club_success_new_member() {
                        // given
                        Long clubId = 1L;
                        Long userId = 10L;
                        Long ownerId = 999L;
                        ClubMemberRequest request = ClubMemberRequest.builder()
                                        .nickname("테스트닉네임")
                                        .build();

                        Clubs club = createClub(clubId, ownerId);
                        given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
                        given(clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE))
                                        .willReturn(10L);
                        given(clubMemberRepository.existsByClubIdAndNickname(clubId, request.getNickname()))
                                        .willReturn(false);

                        ClubMembers newMember = ClubMembers.builder()
                                        .clubId(clubId)
                                        .userId(userId)
                                        .nickname(request.getNickname())
                                        .build();
                        ReflectionTestUtils.setField(newMember, "memberId", 1L);

                        given(clubMemberRepository.findByClubIdAndUserId(clubId, userId))
                                        .willReturn(Optional.empty());
                        given(clubMemberRepository.save(any(ClubMembers.class)))
                                        .willReturn(newMember);

                        // when
                        ClubMemberResponse response = clubMemberService.joinClub(clubId, userId, request);

                        // then
                        assertThat(response).isNotNull();
                        assertThat(response.getUserId()).isEqualTo(10L);
                        assertThat(response.getNickname()).isEqualTo("테스트닉네임");
                        assertThat(response.getStatus()).isEqualTo("PENDING");

                        then(clubMemberRepository).should(times(1)).findByClubIdAndUserId(clubId, userId);
                        then(clubMemberRepository).should(times(1)).save(any(ClubMembers.class));
                }

                @Test
                @DisplayName("기존 회원 재가입 성공 (REJECTED 상태)")
                void join_club_success_reapply_rejected() {
                        // given
                        Long clubId = 1L;
                        Long userId = 10L;
                        Long ownerId = 999L;
                        ClubMemberRequest request = ClubMemberRequest.builder()
                                        .nickname("새닉네임")
                                        .build();

                        Clubs club = createClub(clubId, ownerId);
                        given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
                        given(clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE))
                                        .willReturn(10L);
                        given(clubMemberRepository.existsByClubIdAndNickname(clubId, request.getNickname()))
                                        .willReturn(false);

                        ClubMembers existingMember = ClubMembers.builder()
                                        .clubId(clubId)
                                        .userId(userId)
                                        .nickname("기존닉네임")
                                        .build();
                        ReflectionTestUtils.setField(existingMember, "memberId", 1L);
                        ReflectionTestUtils.setField(existingMember, "status", ClubMembers.Status.REJECTED);

                        given(clubMemberRepository.findByClubIdAndUserId(clubId, userId))
                                        .willReturn(Optional.of(existingMember));

                        // when
                        ClubMemberResponse response = clubMemberService.joinClub(clubId, userId, request);

                        // then
                        assertThat(response).isNotNull();
                        assertThat(existingMember.getStatus()).isEqualTo(ClubMembers.Status.PENDING);

                        then(clubMemberRepository).should(times(1)).findByClubIdAndUserId(clubId, userId);
                        then(clubMemberRepository).should(never()).save(any(ClubMembers.class));
                }

                @Test
                @DisplayName("기존 회원 재가입 성공 (LEFT 상태)")
                void join_club_success_reapply_left() {
                        // given
                        Long clubId = 1L;
                        Long userId = 10L;
                        Long ownerId = 999L;
                        ClubMemberRequest request = ClubMemberRequest.builder()
                                        .nickname("새닉네임")
                                        .build();

                        Clubs club = createClub(clubId, ownerId);
                        given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
                        given(clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE))
                                        .willReturn(10L);
                        given(clubMemberRepository.existsByClubIdAndNickname(clubId, request.getNickname()))
                                        .willReturn(false);

                        ClubMembers existingMember = ClubMembers.builder()
                                        .clubId(clubId)
                                        .userId(userId)
                                        .nickname("기존닉네임")
                                        .build();
                        ReflectionTestUtils.setField(existingMember, "memberId", 1L);
                        ReflectionTestUtils.setField(existingMember, "status", ClubMembers.Status.LEFT);

                        given(clubMemberRepository.findByClubIdAndUserId(clubId, userId))
                                        .willReturn(Optional.of(existingMember));

                        // when
                        ClubMemberResponse response = clubMemberService.joinClub(clubId, userId, request);

                        // then
                        assertThat(response).isNotNull();
                        assertThat(existingMember.getStatus()).isEqualTo(ClubMembers.Status.PENDING);

                        then(clubMemberRepository).should(times(1)).findByClubIdAndUserId(clubId, userId);
                }
        }

        @Nested
        @DisplayName("가입 승인")
        class ApproveClubMember {

                @Test
                @DisplayName("가입 승인 성공")
                void approve_club_member_success() {
                        // given
                        Long clubId = 1L;
                        Long memberId = 1L;
                        Long userId = 10L;

                        ClubMembers member = ClubMembers.builder()
                                        .clubId(clubId)
                                        .userId(userId)
                                        .nickname("테스트닉네임")
                                        .build();
                        ReflectionTestUtils.setField(member, "memberId", memberId);
                        ReflectionTestUtils.setField(member, "status", ClubMembers.Status.PENDING);

                        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId))
                                        .willReturn(Optional.of(member));

                        Clubs club = createClub(clubId, 999L);
                        given(clubRepository.findById(clubId)).willReturn(Optional.of(club));

                        // when
                        ClubMemberResponse response = clubMemberService.approveClubMember(clubId, memberId);

                        // then
                        assertThat(response).isNotNull();
                        assertThat(member.getStatus()).isEqualTo(ClubMembers.Status.ACTIVE);
                        assertThat(member.getJoinedAt()).isNotNull();

                        then(clubMemberRepository).should(times(1)).findByClubIdAndMemberId(clubId, memberId);
                }

                @Test
                @DisplayName("가입 승인 실패 - 회원 없음")
                void approve_club_member_fail_not_found() {
                        // given
                        Long clubId = 1L;
                        Long memberId = 10L;

                        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId))
                                        .willReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(() -> clubMemberService.approveClubMember(clubId, memberId))
                                        .isInstanceOf(ClubException.MemberNotFound.class);
                }

                @Test
                @DisplayName("가입 승인 실패 - PENDING 상태가 아님")
                void approve_club_member_fail_not_pending() {
                        // given
                        Long clubId = 1L;
                        Long memberId = 1L;
                        Long userId = 10L;

                        ClubMembers member = ClubMembers.builder()
                                        .clubId(clubId)
                                        .userId(userId)
                                        .nickname("테스트닉네임")
                                        .build();
                        ReflectionTestUtils.setField(member, "memberId", memberId);
                        ReflectionTestUtils.setField(member, "status", ClubMembers.Status.ACTIVE);

                        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId))
                                        .willReturn(Optional.of(member));

                        // when & then
                        assertThatThrownBy(() -> clubMemberService.approveClubMember(clubId, memberId))
                                        .isInstanceOf(ClubException.MemberNotPending.class);
                }
        }

        @Nested
        @DisplayName("가입 거절")
        class RejectClubMember {

                @Test
                @DisplayName("가입 거절 성공")
                void reject_club_member_success() {
                        // given
                        Long clubId = 1L;
                        Long memberId = 1L;

                        ClubMembers member = ClubMembers.builder()
                                        .clubId(clubId)
                                        .userId(10L)
                                        .nickname("테스트닉네임")
                                        .build();
                        ReflectionTestUtils.setField(member, "memberId", memberId);
                        ReflectionTestUtils.setField(member, "status", ClubMembers.Status.PENDING);

                        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId))
                                        .willReturn(Optional.of(member));

                        // when
                        clubMemberService.rejectClubMember(clubId, memberId);

                        // then
                        assertThat(member.getStatus()).isEqualTo(ClubMembers.Status.REJECTED);

                        then(clubMemberRepository).should(times(1)).findByClubIdAndMemberId(clubId, memberId);
                }

                @Test
                @DisplayName("가입 거절 실패 - 회원 없음")
                void reject_club_member_fail_not_found() {
                        // given
                        Long clubId = 1L;
                        Long memberId = 999L;

                        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId))
                                        .willReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(() -> clubMemberService.rejectClubMember(clubId, memberId))
                                        .isInstanceOf(ClubException.MemberNotFound.class);
                }

                @Test
                @DisplayName("가입 거절 실패 - 다른 모임의 회원")
                void reject_club_member_fail_club_mismatch() {
                        // given
                        Long clubId = 1L;
                        Long otherClubId = 999L;
                        Long memberId = 1L;

                        ClubMembers member = ClubMembers.builder()
                                        .clubId(otherClubId)
                                        .userId(10L)
                                        .nickname("테스트닉네임")
                                        .build();
                        ReflectionTestUtils.setField(member, "memberId", memberId);

                        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId))
                                        .willReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(() -> clubMemberService.rejectClubMember(clubId, memberId))
                                        .isInstanceOf(ClubException.MemberNotFound.class);
                }
        }

        @Nested
        @DisplayName("회원 강퇴")
        class KickMember {

                @Test
                @DisplayName("회원 강퇴 성공")
                void kick_member_success() {
                        // given
                        Long clubId = 1L;
                        Long memberId = 1L;
                        Long userId = 10L;

                        ClubMembers member = ClubMembers.builder()
                                        .clubId(clubId)
                                        .userId(userId)
                                        .nickname("테스트닉네임")
                                        .build();
                        ReflectionTestUtils.setField(member, "memberId", memberId);
                        ReflectionTestUtils.setField(member, "status", ClubMembers.Status.ACTIVE);
                        ReflectionTestUtils.setField(member, "role", ClubMembers.Role.MEMBER);

                        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId))
                                        .willReturn(Optional.of(member));

                        // when
                        clubMemberService.kickMember(clubId, memberId);

                        // then
                        assertThat(member.getStatus()).isEqualTo(ClubMembers.Status.KICKED);
                        assertThat(member.getRole()).isEqualTo(ClubMembers.Role.NONE);

                        then(clubMemberRepository).should(times(1)).findByClubIdAndMemberId(clubId, memberId);
                }

                @Test
                @DisplayName("회원 강퇴 실패 - 회원 없음")
                void kick_member_fail_not_found() {
                        // given
                        Long clubId = 1L;
                        Long memberId = 10L;

                        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId))
                                        .willReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(() -> clubMemberService.kickMember(clubId, memberId))
                                        .isInstanceOf(ClubException.MemberNotFound.class);
                }
        }
}
