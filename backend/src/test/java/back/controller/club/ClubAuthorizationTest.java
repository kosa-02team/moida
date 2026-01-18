package back.controller.club;

import back.service.club.ClubAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ClubAuthorizationTest {

    @Mock
    private ClubAuthService clubAuthService;

    @InjectMocks
    private ClubAuthorization clubAuthorization;

    @Nested
    @DisplayName("모임장 확인")
    class IsOwner {

        @Test
        @DisplayName("모임장 확인 성공")
        void is_owner_success() {
            // given
            Long clubId = 1L;

            given(clubAuthService.isOwner(clubId, 1L))
                    .willReturn(true);

            // when
            boolean result = clubAuthorization.isOwner(clubId);

            // then
            assertThat(result).isTrue();
            then(clubAuthService).should(times(1)).isOwner(clubId, 1L);
        }

        @Test
        @DisplayName("모임장 확인 실패 - 일반 멤버")
        void is_owner_fail_not_owner() {
            // given
            Long clubId = 1L;

            given(clubAuthService.isOwner(clubId, 1L))
                    .willReturn(false);

            // when
            boolean result = clubAuthorization.isOwner(clubId);

            // then
            assertThat(result).isFalse();
        }
    }
}
