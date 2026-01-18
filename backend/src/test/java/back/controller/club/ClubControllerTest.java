package back.controller.club;

import back.common.response.SuccessResponse;
import back.config.security.UserPrincipal;
import back.dto.club.ClubRequest;
import back.dto.club.ClubResponse;
import back.service.club.ClubService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClubControllerTest {

    @Mock
    private ClubService clubService;

    @Mock
    private ClubAuthorization clubAuthorization;

    @InjectMocks
    private ClubController clubController;


    private UserPrincipal createUserPrincipal(Long userId) {
        return new UserPrincipal(userId, "test@test.com");
    }

    private ClubResponse createClubResponse(Long clubId, Long ownerId) {
        return ClubResponse.builder()
                .clubId(clubId)
                .clubName("테스트모임")
                .ownerId(ownerId)
                .visibility("PUBLIC")
                .type("OPERATION_FEE")
                .maxMembers(100)
                .currentMembers(0)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("모임 생성")
    class CreateClub {

        @Test
        @DisplayName("모임 생성 성공")
        void create_club_success() {
            // given
            Long ownerId = 1L;
            UserPrincipal principal = createUserPrincipal(ownerId);
            ClubRequest request = ClubRequest.builder()
                    .clubName("새로운모임")
                    .visibility("PUBLIC")
                    .type("OPERATION_FEE")
                    .maxMembers(50)
                    .build();

            ClubResponse response = createClubResponse(1L, ownerId);
            response = ClubResponse.builder()
                    .clubId(1L)
                    .clubName("새로운모임")
                    .ownerId(ownerId)
                    .visibility("PUBLIC")
                    .type("OPERATION_FEE")
                    .maxMembers(50)
                    .currentMembers(0)
                    .build();

            given(clubAuthorization.requireUserId(principal)).willReturn(ownerId);
            given(clubService.createClub(any(ClubRequest.class), eq(ownerId))).willReturn(response);

            // when
            ResponseEntity<SuccessResponse<ClubResponse>> result = clubController.createClub(principal, request);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().data().getClubName()).isEqualTo("새로운모임");
            assertThat(result.getBody().data().getOwnerId()).isEqualTo(ownerId);

            then(clubAuthorization).should(times(1)).requireUserId(principal);
            then(clubService).should(times(1)).createClub(any(ClubRequest.class), eq(ownerId));
        }

        @Test
        @DisplayName("모임 생성 실패 - 비로그인")
        void create_club_fail_not_logged_in() {
            // given
            UserPrincipal principal = null;
            ClubRequest request = ClubRequest.builder()
                    .clubName("새로운모임")
                    .build();

            given(clubAuthorization.requireUserId(null))
                    .willThrow(new RuntimeException("로그인이 필요합니다"));

            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                clubController.createClub(principal, request);
            });

            then(clubService).should(never()).createClub(any(), any());
        }
    }

    @Nested
    @DisplayName("모임 조회")
    class GetClub {

        @Test
        @DisplayName("모임 조회 성공 - 로그인")
        void get_club_success_logged_in() {
            // given
            Long clubId = 1L;
            Long viewerId = 10L;
            UserPrincipal principal = createUserPrincipal(viewerId);
            ClubResponse response = createClubResponse(clubId, 1L);

            given(clubService.getClub(clubId, viewerId)).willReturn(response);

            // when
            ResponseEntity<SuccessResponse<ClubResponse>> result = clubController.getClub(principal, clubId);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().data().getClubId()).isEqualTo(clubId);

            then(clubService).should(times(1)).getClub(clubId, viewerId);
        }

        @Test
        @DisplayName("모임 조회 성공 - 비로그인")
        void get_club_success_not_logged_in() {
            // given
            Long clubId = 1L;
            UserPrincipal principal = null;
            ClubResponse response = createClubResponse(clubId, 1L);

            given(clubService.getClub(clubId, null)).willReturn(response);

            // when
            ResponseEntity<SuccessResponse<ClubResponse>> result = clubController.getClub(principal, clubId);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().data().getClubId()).isEqualTo(clubId);

            then(clubService).should(times(1)).getClub(clubId, null);
        }
    }

    @Nested
    @DisplayName("모임 수정")
    class UpdateClub {

        @Test
        @DisplayName("모임 수정 성공")
        void update_club_success() {
            // given
            Long clubId = 1L;
            Long ownerId = 1L;
            UserPrincipal principal = createUserPrincipal(ownerId);
            ClubRequest request = ClubRequest.builder()
                    .clubName("수정된모임명")
                    .visibility("PRIVATE")
                    .build();

            ClubResponse response = createClubResponse(clubId, ownerId);
            response = ClubResponse.builder()
                    .clubId(clubId)
                    .clubName("수정된모임명")
                    .ownerId(ownerId)
                    .visibility("PRIVATE")
                    .build();

            given(clubAuthorization.requireOwner(clubId, principal)).willReturn(ownerId);
            given(clubService.updateClub(eq(clubId), any(ClubRequest.class), eq(ownerId))).willReturn(response);

            // when
            ResponseEntity<SuccessResponse<ClubResponse>> result = clubController.updateClub(principal, clubId, request);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().data().getClubName()).isEqualTo("수정된모임명");
            assertThat(result.getBody().data().getVisibility()).isEqualTo("PRIVATE");

            then(clubAuthorization).should(times(1)).requireOwner(clubId, principal);
            then(clubService).should(times(1)).updateClub(eq(clubId), any(ClubRequest.class), eq(ownerId));
        }
    }

    @Nested
    @DisplayName("모임 종료")
    class CloseClub {

        @Test
        @DisplayName("모임 종료 성공")
        void close_club_success() {
            // given
            Long clubId = 1L;
            Long ownerId = 1L;
            UserPrincipal principal = createUserPrincipal(ownerId);

            given(clubAuthorization.requireOwner(clubId, principal)).willReturn(ownerId);
            willDoNothing().given(clubService).closeClub(clubId, ownerId);

            // when
            ResponseEntity<SuccessResponse<Void>> result = clubController.closeClub(principal, clubId);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();

            then(clubAuthorization).should(times(1)).requireOwner(clubId, principal);
            then(clubService).should(times(1)).closeClub(clubId, ownerId);
        }
    }

    @Nested
    @DisplayName("모임 활성화")
    class ActivateClub {

        @Test
        @DisplayName("모임 활성화 성공")
        void activate_club_success() {
            // given
            Long clubId = 1L;
            Long ownerId = 1L;
            UserPrincipal principal = createUserPrincipal(ownerId);

            given(clubAuthorization.requireOwner(clubId, principal)).willReturn(ownerId);
            willDoNothing().given(clubService).activateClub(clubId, ownerId);

            // when
            ResponseEntity<SuccessResponse<Void>> result = clubController.activateClub(principal, clubId);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();

            then(clubAuthorization).should(times(1)).requireOwner(clubId, principal);
            then(clubService).should(times(1)).activateClub(clubId, ownerId);
        }
    }
}
