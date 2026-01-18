package back.domain.club;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClubsTest {

    @Nested
    @DisplayName("모임 생성")
    class Create {

        @Test
        @DisplayName("모임 생성 성공")
        void create_club_success() {
            // given & when
            Clubs club = new Clubs("테스트 모임", 1L);

            // then
            assertThat(club.getClubName()).isEqualTo("테스트 모임");
            assertThat(club.getOwnerId()).isEqualTo(1L);
            assertThat(club.getMainAccountId()).isNotNull();
            assertThat(club.getMainAccountId().length()).isEqualTo(36); // UUID 길이
            assertThat(club.getStatus()).isEqualTo(Clubs.Status.ACTIVE);
            assertThat(club.getVisibility()).isEqualTo(Clubs.Visibility.PUBLIC);
            assertThat(club.getInviteCode()).isNotNull();
            assertThat(club.getInviteCode().length()).isEqualTo(36); // UUID 길이
        }
    }

    @Nested
    @DisplayName("모임 이름 수정")
    class UpdateName {

        @Test
        @DisplayName("모임 이름 수정 성공")
        void update_name_success() {
            // given
            Clubs club = new Clubs("기존 이름", 1L);

            // when
            club.updateName("새 이름");

            // then
            assertThat(club.getClubName()).isEqualTo("새 이름");
        }
    }

    @Nested
    @DisplayName("모임장 위임")
    class ChangeOwner {

        @Test
        @DisplayName("모임장 위임 성공")
        void change_owner_success() {
            // given
            Clubs club = new Clubs("테스트 모임", 1L);

            // when
            club.changeOwner(2L);

            // then
            assertThat(club.getOwnerId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("모임장 위임 - 동일한 ID")
        void change_owner_same_id() {
            // given
            Clubs club = new Clubs("테스트 모임", 1L);

            // when
            club.changeOwner(1L);

            // then
            assertThat(club.getOwnerId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("대표 계좌 변경")
    class ChangeMainAccount {

        @Test
        @DisplayName("대표 계좌 변경 성공")
        void change_main_account_success() {
            // given
            Clubs club = new Clubs("테스트 모임", 1L);
            String originalAccountId = club.getMainAccountId();

            // when
            club.changeMainAccount("new-uuid-account-id");

            // then
            assertThat(club.getMainAccountId()).isEqualTo("new-uuid-account-id");
            assertThat(club.getMainAccountId()).isNotEqualTo(originalAccountId);
        }
    }

    @Nested
    @DisplayName("초대 코드 생성")
    class GenerateInviteCode {

        @Test
        @DisplayName("초대 코드 재생성 성공")
        void regenerate_invite_code_success() {
            // given
            Clubs club = new Clubs("테스트 모임", 1L);
            String originalCode = club.getInviteCode();

            // when
            club.regenerateInviteCode();

            // then
            assertThat(club.getInviteCode()).isNotNull();
            assertThat(club.getInviteCode().length()).isEqualTo(36); // UUID 길이
            assertThat(club.getInviteCode()).isNotEqualTo(originalCode);
        }
    }

    @Nested
    @DisplayName("모임 폐쇄")
    class Close {

        @Test
        @DisplayName("모임 폐쇄 성공")
        void close_club_success() {
            // given
            Clubs club = new Clubs("테스트 모임", 1L);

            // when
            club.close();

            // then
            assertThat(club.getStatus()).isEqualTo(Clubs.Status.INACTIVE);
            assertThat(club.getClosedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("모임 재활성화")
    class Activate {

        @Test
        @DisplayName("모임 재활성화 성공")
        void activate_club_success() {
            // given
            Clubs club = new Clubs("테스트 모임", 1L);
            club.close();

            // when
            club.activate();

            // then
            assertThat(club.getStatus()).isEqualTo(Clubs.Status.ACTIVE);
            assertThat(club.getClosedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("공개 설정 변경")
    class SetVisibility {

        @Test
        @DisplayName("공개 설정 변경 성공 - PRIVATE")
        void set_visibility_success_private() {
            // given
            Clubs club = new Clubs("테스트 모임", 1L);

            // when
            club.setVisibility(Clubs.Visibility.PRIVATE);

            // then
            assertThat(club.getVisibility()).isEqualTo(Clubs.Visibility.PRIVATE);
        }

        @Test
        @DisplayName("공개 설정 변경 성공 - PUBLIC")
        void set_visibility_success_public() {
            // given
            Clubs club = new Clubs("테스트 모임", 1L);
            club.setVisibility(Clubs.Visibility.PRIVATE);

            // when
            club.setVisibility(Clubs.Visibility.PUBLIC);

            // then
            assertThat(club.getVisibility()).isEqualTo(Clubs.Visibility.PUBLIC);
        }

        @Test
        @DisplayName("공개 설정 변경 - null 입력 시 변경 없음")
        void set_visibility_null_no_change() {
            // given
            Clubs club = new Clubs("테스트 모임", 1L);
            Clubs.Visibility originalVisibility = club.getVisibility();

            // when
            club.setVisibility(null);

            // then
            assertThat(club.getVisibility()).isEqualTo(originalVisibility);
        }
    }
}
