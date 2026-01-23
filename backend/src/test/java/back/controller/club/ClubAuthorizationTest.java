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
            Long userId = 1L;

            // SecurityContext Mocking
            org.springframework.security.core.Authentication authentication = mock(
                    org.springframework.security.core.Authentication.class);
            org.springframework.security.core.context.SecurityContext securityContext = mock(
                    org.springframework.security.core.context.SecurityContext.class);
            back.config.security.UserPrincipal userPrincipal = new back.config.security.UserPrincipal(userId,
                    "test@test.com");

            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

            given(clubAuthService.isOwner(clubId, userId)).willReturn(true);

            // when
            boolean result = clubAuthorization.isOwner(clubId);

            // then
            assertThat(result).isTrue();

            // cleanup
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("모임장 확인 실패 - 일반 멤버")
        void is_owner_fail_not_owner() {
            // given
            Long clubId = 1L;
            Long userId = 1L;

            // SecurityContext Mocking
            org.springframework.security.core.Authentication authentication = mock(
                    org.springframework.security.core.Authentication.class);
            org.springframework.security.core.context.SecurityContext securityContext = mock(
                    org.springframework.security.core.context.SecurityContext.class);
            back.config.security.UserPrincipal userPrincipal = new back.config.security.UserPrincipal(userId,
                    "test@test.com");

            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

            given(clubAuthService.isOwner(clubId, userId)).willReturn(false);

            // when
            boolean result = clubAuthorization.isOwner(clubId);

            // then
            assertThat(result).isFalse();

            // cleanup
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("모임장 확인 실패 - 인증 정보 없음")
        void is_owner_fail_no_auth() {
            // given
            Long clubId = 1L;

            org.springframework.security.core.context.SecurityContext securityContext = mock(
                    org.springframework.security.core.context.SecurityContext.class);
            given(securityContext.getAuthentication()).willReturn(null);
            org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

            // when
            boolean result = clubAuthorization.isOwner(clubId);

            // then
            assertThat(result).isFalse();

            // cleanup
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
