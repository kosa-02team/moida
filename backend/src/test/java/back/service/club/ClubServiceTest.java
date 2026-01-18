package back.service.club;

import back.domain.club.ClubMembers;
import back.domain.club.Clubs;
import back.dto.club.ClubRequest;
import back.dto.club.ClubResponse;
import back.exception.ClubException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @InjectMocks
    private ClubService clubService;

    private Clubs createClub(Long clubId, Long ownerId) {
        Clubs club = new Clubs("테스트모임", ownerId);
        ReflectionTestUtils.setField(club, "clubId", clubId);
        return club;
    }

    private Clubs createClub(Long clubId, Long ownerId, Clubs.Visibility visibility) {
        Clubs club = new Clubs("테스트모임", ownerId);
        ReflectionTestUtils.setField(club, "clubId", clubId);
        club.setVisibility(visibility);
        return club;
    }

    @Nested
    @DisplayName("모임 생성")
    class CreateClub {

        @Test
        @DisplayName("모임 생성 성공")
        void create_club_success() {
            // given
            Long ownerId = 1L;
            ClubRequest request = ClubRequest.builder()
                    .clubName("새로운모임")
                    .visibility("PUBLIC")
                    .type("OPERATION_FEE")
                    .maxMembers(50)
                    .build();

            given(clubRepository.existsByClubName(request.getClubName())).willReturn(false);
            given(clubRepository.save(any(Clubs.class))).willAnswer(invocation -> {
                Clubs club = invocation.getArgument(0);
                ReflectionTestUtils.setField(club, "clubId", 1L);
                return club;
            });

            // when
            ClubResponse response = clubService.createClub(request, ownerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getClubName()).isEqualTo("새로운모임");
            assertThat(response.getOwnerId()).isEqualTo(ownerId);
            assertThat(response.getVisibility()).isEqualTo("PUBLIC");
            assertThat(response.getType()).isEqualTo("OPERATION_FEE");
            assertThat(response.getMaxMembers()).isEqualTo(50);
            assertThat(response.getCurrentMembers()).isEqualTo(0);

            then(clubRepository).should(times(1)).existsByClubName(request.getClubName());
            then(clubRepository).should(times(1)).save(any(Clubs.class));
        }

        @Test
        @DisplayName("모임 생성 성공 - 기본값 사용")
        void create_club_success_with_defaults() {
            // given
            Long ownerId = 1L;
            ClubRequest request = ClubRequest.builder()
                    .clubName("새로운모임")
                    .build();

            given(clubRepository.existsByClubName(request.getClubName())).willReturn(false);
            given(clubRepository.save(any(Clubs.class))).willAnswer(invocation -> {
                Clubs club = invocation.getArgument(0);
                ReflectionTestUtils.setField(club, "clubId", 1L);
                return club;
            });

            // when
            ClubResponse response = clubService.createClub(request, ownerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getClubName()).isEqualTo("새로운모임");
            assertThat(response.getVisibility()).isEqualTo("PUBLIC");
            assertThat(response.getType()).isEqualTo("OPERATION_FEE");
            assertThat(response.getMaxMembers()).isEqualTo(100);
        }

        @Test
        @DisplayName("모임 생성 실패 - 이름 중복")
        void create_club_fail_duplicate_name() {
            // given
            Long ownerId = 1L;
            ClubRequest request = ClubRequest.builder()
                    .clubName("중복모임")
                    .build();

            given(clubRepository.existsByClubName(request.getClubName())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> clubService.createClub(request, ownerId))
                    .isInstanceOf(ClubException.AlreadyExists.class);

            then(clubRepository).should(times(1)).existsByClubName(request.getClubName());
            then(clubRepository).should(never()).save(any(Clubs.class));
        }
    }

    @Nested
    @DisplayName("모임 조회")
    class GetClub {

        @Test
        @DisplayName("공개 모임 조회 성공 - 비로그인")
        void get_club_success_public_no_login() {
            // given
            Long clubId = 1L;
            Long viewerId = null;
            Clubs club = createClub(clubId, 1L, Clubs.Visibility.PUBLIC);

            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE))
                    .willReturn(10L);

            // when
            ClubResponse response = clubService.getClub(clubId, viewerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getClubId()).isEqualTo(clubId);
            assertThat(response.getCurrentMembers()).isEqualTo(10);

            then(clubRepository).should(times(1)).findById(clubId);
            then(clubMemberRepository).should(times(1))
                    .countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE);
        }

        @Test
        @DisplayName("공개 모임 조회 성공 - 로그인")
        void get_club_success_public_logged_in() {
            // given
            Long clubId = 1L;
            Long viewerId = 10L;
            Clubs club = createClub(clubId, 1L, Clubs.Visibility.PUBLIC);

            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE))
                    .willReturn(10L);

            // when
            ClubResponse response = clubService.getClub(clubId, viewerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getClubId()).isEqualTo(clubId);
            assertThat(response.getCurrentMembers()).isEqualTo(10);
        }

        @Test
        @DisplayName("비공개 모임 조회 성공 - 멤버")
        void get_club_success_private_member() {
            // given
            Long clubId = 1L;
            Long viewerId = 10L;
            Clubs club = createClub(clubId, 1L, Clubs.Visibility.PRIVATE);

            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubMemberRepository.existsByClubIdAndUserIdAndStatus(clubId, viewerId, ClubMembers.Status.ACTIVE))
                    .willReturn(true);
            given(clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE))
                    .willReturn(10L);

            // when
            ClubResponse response = clubService.getClub(clubId, viewerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getClubId()).isEqualTo(clubId);
            assertThat(response.getCurrentMembers()).isEqualTo(10);
            assertThat(response.getOwnerId()).isNotNull(); // 전체 정보 반환

            then(clubMemberRepository).should(times(1))
                    .existsByClubIdAndUserIdAndStatus(clubId, viewerId, ClubMembers.Status.ACTIVE);
        }

        @Test
        @DisplayName("비공개 모임 조회 성공 - 비멤버 (제한된 정보)")
        void get_club_success_private_non_member() {
            // given
            Long clubId = 1L;
            Long viewerId = 10L;
            Clubs club = createClub(clubId, 1L, Clubs.Visibility.PRIVATE);

            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubMemberRepository.existsByClubIdAndUserIdAndStatus(clubId, viewerId, ClubMembers.Status.ACTIVE))
                    .willReturn(false);

            // when
            ClubResponse response = clubService.getClub(clubId, viewerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getClubId()).isEqualTo(clubId);
            assertThat(response.getClubName()).isNotNull();
            assertThat(response.getType()).isNotNull();
            assertThat(response.getVisibility()).isNotNull();
            assertThat(response.getOwnerId()).isNull(); // 제한된 정보만 반환
            assertThat(response.getCurrentMembers()).isNull();

            then(clubMemberRepository).should(times(1))
                    .existsByClubIdAndUserIdAndStatus(clubId, viewerId, ClubMembers.Status.ACTIVE);
            then(clubMemberRepository).should(never())
                    .countByClubIdAndStatus(any(), any());
        }

        @Test
        @DisplayName("모임 조회 실패 - 모임 없음")
        void get_club_fail_not_found() {
            // given
            Long clubId = 999L;
            Long viewerId = null;

            given(clubRepository.findById(clubId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clubService.getClub(clubId, viewerId))
                    .isInstanceOf(ClubException.NotFound.class);

            then(clubRepository).should(times(1)).findById(clubId);
        }

        @Test
        @DisplayName("비로그인 사용자 모임 조회")
        void get_club_no_login() {
            // given
            Long clubId = 1L;
            Clubs club = createClub(clubId, 1L, Clubs.Visibility.PUBLIC);

            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE))
                    .willReturn(5L);

            // when
            ClubResponse response = clubService.getClub(clubId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getClubId()).isEqualTo(clubId);
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
            Clubs club = createClub(clubId, ownerId);
            club.setVisibility(Clubs.Visibility.PUBLIC);

            ClubRequest request = ClubRequest.builder()
                    .clubName("수정된모임명")
                    .visibility("PRIVATE")
                    .type("FAIR_SETTLEMENT")
                    .maxMembers(200)
                    .build();

            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubRepository.existsByClubName(request.getClubName())).willReturn(false);
            given(clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE))
                    .willReturn(15L);

            // when
            ClubResponse response = clubService.updateClub(clubId, request, ownerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getClubName()).isEqualTo("수정된모임명");
            assertThat(response.getVisibility()).isEqualTo("PRIVATE");
            assertThat(response.getType()).isEqualTo("FAIR_SETTLEMENT");
            assertThat(response.getMaxMembers()).isEqualTo(200);
            assertThat(response.getCurrentMembers()).isEqualTo(15);

            then(clubRepository).should(times(1)).findById(clubId);
            then(clubRepository).should(times(1)).existsByClubName(request.getClubName());
        }

        @Test
        @DisplayName("모임 수정 성공 - 이름 변경 없음")
        void update_club_success_same_name() {
            // given
            Long clubId = 1L;
            Long ownerId = 1L;
            Clubs club = createClub(clubId, ownerId);
            club.setVisibility(Clubs.Visibility.PUBLIC);

            ClubRequest request = ClubRequest.builder()
                    .clubName("테스트모임") // 기존 이름과 동일
                    .visibility("PRIVATE")
                    .build();

            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE))
                    .willReturn(15L);

            // when
            ClubResponse response = clubService.updateClub(clubId, request, ownerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getClubName()).isEqualTo("테스트모임");
            assertThat(response.getVisibility()).isEqualTo("PRIVATE");

            then(clubRepository).should(never()).existsByClubName(any());
        }

        @Test
        @DisplayName("모임 수정 실패 - 모임 없음")
        void update_club_fail_not_found() {
            // given
            Long clubId = 999L;
            Long ownerId = 1L;
            ClubRequest request = ClubRequest.builder()
                    .clubName("수정된모임명")
                    .build();

            given(clubRepository.findById(clubId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clubService.updateClub(clubId, request, ownerId))
                    .isInstanceOf(ClubException.NotFound.class);

            then(clubRepository).should(times(1)).findById(clubId);
            then(clubRepository).should(never()).existsByClubName(any());
        }

        @Test
        @DisplayName("모임 수정 실패 - 이름 중복")
        void update_club_fail_duplicate_name() {
            // given
            Long clubId = 1L;
            Long ownerId = 1L;
            Clubs club = createClub(clubId, ownerId);

            ClubRequest request = ClubRequest.builder()
                    .clubName("중복모임명")
                    .build();

            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubRepository.existsByClubName(request.getClubName())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> clubService.updateClub(clubId, request, ownerId))
                    .isInstanceOf(ClubException.AlreadyExists.class);

            then(clubRepository).should(times(1)).existsByClubName(request.getClubName());
        }

        @Test
        @DisplayName("모임 수정 성공 - 부분 수정")
        void update_club_success_partial() {
            // given
            Long clubId = 1L;
            Long ownerId = 1L;
            Clubs club = createClub(clubId, ownerId);

            ClubRequest request = ClubRequest.builder()
                    .clubName("수정된모임명")
                    .visibility("PRIVATE")
                    // type, maxMembers는 null
                    .build();

            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(clubRepository.existsByClubName(request.getClubName())).willReturn(false);
            given(clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE))
                    .willReturn(10L);

            // when
            ClubResponse response = clubService.updateClub(clubId, request, ownerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getClubName()).isEqualTo("수정된모임명");
            assertThat(response.getVisibility()).isEqualTo("PRIVATE");
            // type과 maxMembers는 기존 값 유지
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
            Clubs club = createClub(clubId, ownerId);
            assertThat(club.getStatus()).isEqualTo(Clubs.Status.ACTIVE);

            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));

            // when
            clubService.closeClub(clubId, ownerId);

            // then
            assertThat(club.getStatus()).isEqualTo(Clubs.Status.INACTIVE);
            assertThat(club.getClosedAt()).isNotNull();

            then(clubRepository).should(times(1)).findById(clubId);
        }

        @Test
        @DisplayName("모임 종료 실패 - 모임 없음")
        void close_club_fail_not_found() {
            // given
            Long clubId = 999L;
            Long ownerId = 1L;

            given(clubRepository.findById(clubId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clubService.closeClub(clubId, ownerId))
                    .isInstanceOf(ClubException.NotFound.class);

            then(clubRepository).should(times(1)).findById(clubId);
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
            Clubs club = createClub(clubId, ownerId);
            club.close(); // 먼저 종료 상태로 변경
            assertThat(club.getStatus()).isEqualTo(Clubs.Status.INACTIVE);

            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));

            // when
            clubService.activateClub(clubId, ownerId);

            // then
            assertThat(club.getStatus()).isEqualTo(Clubs.Status.ACTIVE);
            assertThat(club.getClosedAt()).isNull();

            then(clubRepository).should(times(1)).findById(clubId);
        }

        @Test
        @DisplayName("모임 활성화 실패 - 모임 없음")
        void activate_club_fail_not_found() {
            // given
            Long clubId = 999L;
            Long ownerId = 1L;

            given(clubRepository.findById(clubId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clubService.activateClub(clubId, ownerId))
                    .isInstanceOf(ClubException.NotFound.class);

            then(clubRepository).should(times(1)).findById(clubId);
        }
    }
}
