package back.domain.club;

import back.exception.ClubException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class ClubMembersTest {

    @Nested
    @DisplayName("가입 승인")
    class Approve {

        @Test
        @DisplayName("가입 승인 성공")
        void approve_success() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();

            // when
            member.approve();

            // then
            assertThat(member.getStatus()).isEqualTo(ClubMembers.Status.ACTIVE);
            assertThat(member.getJoinedAt()).isNotNull();
            assertThat(member.getRole()).isEqualTo(ClubMembers.Role.MEMBER);
        }

        @Test
        @DisplayName("가입 승인 실패 - PENDING 상태가 아님")
        void approve_fail_not_pending() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();
            member.approve(); // ACTIVE 상태로 변경

            // when & then
            assertThatThrownBy(() -> member.approve())
                    .isInstanceOf(ClubException.MemberNotPending.class);
        }
    }

    @Nested
    @DisplayName("가입 거절")
    class Reject {

        @Test
        @DisplayName("가입 거절 성공")
        void reject_success() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();

            // when
            member.reject();

            // then
            assertThat(member.getStatus()).isEqualTo(ClubMembers.Status.REJECTED);
        }

        @Test
        @DisplayName("가입 거절 실패 - PENDING 상태가 아님")
        void reject_fail_not_pending() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();
            member.approve(); // ACTIVE 상태로 변경

            // when & then
            assertThatThrownBy(() -> member.reject())
                    .isInstanceOf(ClubException.MemberNotPending.class);
        }
    }

    @Nested
    @DisplayName("회원 강퇴")
    class Kick {

        @Test
        @DisplayName("회원 강퇴 성공")
        void kick_success() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();
            member.approve(); // ACTIVE 상태로 변경
            member.promoteToStaff();

            // when
            member.kick();

            // then
            assertThat(member.getStatus()).isEqualTo(ClubMembers.Status.KICKED);
            assertThat(member.getRole()).isEqualTo(ClubMembers.Role.NONE);
        }

        @Test
        @DisplayName("회원 강퇴 실패 - ACTIVE 상태가 아님")
        void kick_fail_not_active() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();
            // PENDING 상태

            // when & then
            assertThatThrownBy(() -> member.kick())
                    .isInstanceOf(ClubException.MemberNotActive.class);
        }
    }

    @Nested
    @DisplayName("재가입")
    class ReApply {

        @Test
        @DisplayName("재가입 성공 - REJECTED 상태")
        void reapply_success_rejected() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("기존닉네임")
                    .build();
            member.reject();

            // when
            member.reApply();

            // then
            assertThat(member.getStatus()).isEqualTo(ClubMembers.Status.PENDING);
        }

        @Test
        @DisplayName("재가입 성공 - LEFT 상태")
        void reapply_success_left() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("기존닉네임")
                    .build();
            member.left();

            // when
            member.reApply();

            // then
            assertThat(member.getStatus()).isEqualTo(ClubMembers.Status.PENDING);
        }

        @Test
        @DisplayName("재가입 실패 - KICKED 상태")
        void reapply_fail_kicked() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();
            member.approve();
            member.kick();

            // when & then
            assertThatThrownBy(() -> member.reApply())
                    .isInstanceOf(ClubException.MemberKickedOut.class);
        }

        @Test
        @DisplayName("재가입 실패 - 이미 ACTIVE 상태")
        void reapply_fail_already_active() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();
            member.approve();

            // when & then
            assertThatThrownBy(() -> member.reApply())
                    .isInstanceOf(ClubException.MemberAlreadyActive.class);
        }

        @Test
        @DisplayName("재가입 실패 - 이미 PENDING 상태")
        void reapply_fail_already_pending() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();
            // PENDING 상태

            // when & then
            assertThatThrownBy(() -> member.reApply())
                    .isInstanceOf(ClubException.MemberAlreadyActive.class);
        }
    }

    @Nested
    @DisplayName("권한 체크")
    class RoleCheck {

        @Test
        @DisplayName("운영진 이상 권한 체크 성공")
        void is_manager_level_success() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();
            member.approve();
            member.promoteToStaff();

            // when & then
            assertThat(member.isManagerLevel()).isTrue();
        }

        @Test
        @DisplayName("운영진 이상 권한 체크 실패 - 일반 회원")
        void is_manager_level_fail_member() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();
            member.approve();

            // when & then
            assertThat(member.isManagerLevel()).isFalse();
        }

        @Test
        @DisplayName("재정 관리 권한 체크 성공 - 총무")
        void can_manage_finance_success_accountant() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();
            member.approve();
            member.promoteToAccountant();

            // when & then
            assertThat(member.canManageFinance()).isTrue();
        }

        @Test
        @DisplayName("재정 관리 권한 체크 실패 - 운영진")
        void can_manage_finance_fail_staff() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();
            member.approve();
            member.promoteToStaff();

            // when & then
            assertThat(member.canManageFinance()).isFalse();
        }
    }

    @Nested
    @DisplayName("역할 승급")
    class Promote {

        @Test
        @DisplayName("운영진 승급 성공")
        void promote_to_staff_success() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();

            // when
            member.promoteToStaff();

            // then
            assertThat(member.getRole()).isEqualTo(ClubMembers.Role.STAFF);
        }

        @Test
        @DisplayName("총무 승급 성공")
        void promote_to_accountant_success() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();

            // when
            member.promoteToAccountant();

            // then
            assertThat(member.getRole()).isEqualTo(ClubMembers.Role.ACCOUNTANT);
        }

        @Test
        @DisplayName("모임장 위임 성공")
        void promote_to_owner_success() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();

            // when
            member.promoteToOwner();

            // then
            assertThat(member.getRole()).isEqualTo(ClubMembers.Role.OWNER);
        }

        @Test
        @DisplayName("일반 회원으로 강등 성공")
        void demote_to_member_success() {
            // given
            ClubMembers member = ClubMembers.builder()
                    .clubId(1L)
                    .userId(10L)
                    .nickname("테스트닉네임")
                    .build();
            member.promoteToAccountant();

            // when
            member.demoteToMember();

            // then
            assertThat(member.getRole()).isEqualTo(ClubMembers.Role.MEMBER);
        }
    }
}
