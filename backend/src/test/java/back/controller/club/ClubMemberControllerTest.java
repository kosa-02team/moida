package back.controller.club;

import back.common.response.SuccessResponse;
import back.config.security.UserPrincipal;
import back.dto.club.ClubMemberRequest;
import back.dto.club.ClubMemberResponse;
import back.service.club.ClubMemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ClubMemberControllerTest {

    @Mock
    private ClubMemberService clubMemberService;

    @Mock
    private ClubAuthorization clubAuthorization;

    @InjectMocks
    private ClubMemberController clubMemberController;

    private UserPrincipal createUserPrincipal(Long userId) {
        return new UserPrincipal(userId, "test@test.com");
    }

    private ClubMemberResponse createClubMemberResponse(Long memberId, Long clubId, Long userId) {
        return ClubMemberResponse.builder()
                .memberId(memberId)
                .clubId(clubId)
                .userId(userId)
                .nickname("테스트닉네임")
                .role("MEMBER")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("모임 가입")
    class JoinClub {

        @Test
        @DisplayName("모임 가입 성공")
        void join_club_success() {
            // given
            Long clubId = 1L;
            Long userId = 10L;
            UserPrincipal principal = createUserPrincipal(userId);
            ClubMemberRequest request = ClubMemberRequest.builder()
                    .nickname("새닉네임")
                    .build();

            ClubMemberResponse response = createClubMemberResponse(1L, clubId, userId);
            response = ClubMemberResponse.builder()
                    .memberId(1L)
                    .clubId(clubId)
                    .userId(userId)
                    .nickname("새닉네임")
                    .role("MEMBER")
                    .status("PENDING")
                    .build();

            given(clubAuthorization.requireUserId(principal)).willReturn(userId);
            given(clubMemberService.joinClub(eq(clubId), eq(userId), any(ClubMemberRequest.class)))
                    .willReturn(response);

            // when
            ResponseEntity<SuccessResponse<ClubMemberResponse>> result = clubMemberController.joinClub(principal, clubId, request);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().data().getNickname()).isEqualTo("새닉네임");
            assertThat(result.getBody().data().getStatus()).isEqualTo("PENDING");

            then(clubAuthorization).should(times(1)).requireUserId(principal);
            then(clubMemberService).should(times(1)).joinClub(eq(clubId), eq(userId), any(ClubMemberRequest.class));
        }

        @Test
        @DisplayName("모임 가입 실패 - 비로그인")
        void join_club_fail_not_logged_in() {
            // given
            Long clubId = 1L;
            UserPrincipal principal = null;
            ClubMemberRequest request = ClubMemberRequest.builder()
                    .nickname("새닉네임")
                    .build();

            given(clubAuthorization.requireUserId(null))
                    .willThrow(new RuntimeException("로그인이 필요합니다"));

            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                clubMemberController.joinClub(principal, clubId, request);
            });

            then(clubMemberService).should(never()).joinClub(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("가입 승인")
    class Approve {

        @Test
        @DisplayName("가입 승인 성공")
        void approve_success() {
            // given
            Long clubId = 1L;
            Long memberId = 1L;
            Long ownerId = 1L;
            UserPrincipal principal = createUserPrincipal(ownerId);

            ClubMemberResponse response = createClubMemberResponse(memberId, clubId, 10L);
            response = ClubMemberResponse.builder()
                    .memberId(memberId)
                    .clubId(clubId)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .role("MEMBER")
                    .status("ACTIVE")
                    .joinedAt(LocalDateTime.now())
                    .build();

            given(clubAuthorization.requireOwner(clubId, principal)).willReturn(ownerId);
            given(clubMemberService.approveClubMember(clubId, memberId)).willReturn(response);

            // when
            ResponseEntity<SuccessResponse<ClubMemberResponse>> result = clubMemberController.approve(principal, clubId, memberId);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().data().getStatus()).isEqualTo("ACTIVE");
            assertThat(result.getBody().data().getJoinedAt()).isNotNull();

            then(clubAuthorization).should(times(1)).requireOwner(clubId, principal);
            then(clubMemberService).should(times(1)).approveClubMember(clubId, memberId);
        }
    }

    @Nested
    @DisplayName("가입 거절")
    class Reject {

        @Test
        @DisplayName("가입 거절 성공")
        void reject_success() {
            // given
            Long clubId = 1L;
            Long memberId = 1L;
            Long ownerId = 1L;
            UserPrincipal principal = createUserPrincipal(ownerId);

            given(clubAuthorization.requireOwner(clubId, principal)).willReturn(ownerId);
            willDoNothing().given(clubMemberService).rejectClubMember(clubId, memberId);

            // when
            ResponseEntity<SuccessResponse<Void>> result = clubMemberController.reject(principal, clubId, memberId);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();

            then(clubAuthorization).should(times(1)).requireOwner(clubId, principal);
            then(clubMemberService).should(times(1)).rejectClubMember(clubId, memberId);
        }
    }

    @Nested
    @DisplayName("회원 강퇴")
    class Kick {

        @Test
        @DisplayName("회원 강퇴 성공")
        void kick_success() {
            // given
            Long clubId = 1L;
            Long memberId = 1L;
            Long ownerId = 1L;
            UserPrincipal principal = createUserPrincipal(ownerId);

            given(clubAuthorization.requireOwner(clubId, principal)).willReturn(ownerId);
            willDoNothing().given(clubMemberService).kickMember(clubId, memberId);

            // when
            ResponseEntity<SuccessResponse<Void>> result = clubMemberController.kick(principal, clubId, memberId);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();

            then(clubAuthorization).should(times(1)).requireOwner(clubId, principal);
            then(clubMemberService).should(times(1)).kickMember(clubId, memberId);
        }
    }
}
