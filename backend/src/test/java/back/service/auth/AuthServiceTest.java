package back.service.auth;

import back.config.security.JwtTokenProvider;
import back.config.security.RefreshToken;
import back.domain.Users;
import back.dto.auth.RefreshTokenRequest;
import back.dto.auth.RefreshTokenResponse;
import back.exception.AuthException;
import back.repository.RefreshTokenRepository;
import back.repository.UserRepository;
import back.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("리프레시 토큰 재발급 성공 - RTR 정책(저장 및 삭제) 검증")
    void refresh_Success() {
        // given
        String oldTokenStr = "old-refresh-token";
        String newAccessStr = "new-access-token";
        String newRefreshStr = "new-refresh-token";

        RefreshTokenRequest request = new RefreshTokenRequest(oldTokenStr);

        Users mockUser = new Users("testUser@test.com", "12345678", "테스트");

        RefreshToken mockRefreshTokenEntity = new RefreshToken(oldTokenStr, mockUser);

        // Mock 동작 정의
        given(jwtTokenProvider.validateToken(oldTokenStr)).willReturn(true);

        // [수정 1] findById(String) -> findByToken(String)으로 변경
        given(refreshTokenRepository.findByToken(oldTokenStr))
                .willReturn(Optional.of(mockRefreshTokenEntity));

        given(jwtTokenProvider.createAccessToken(mockUser.getLoginId(), mockUser.getSystemRole(), mockUser.getUserId()))
                .willReturn(newAccessStr);
        given(jwtTokenProvider.createRefreshToken())
                .willReturn(newRefreshStr);

        // when
        RefreshTokenResponse response = authService.refresh(request);

        // then
        assertThat(response.accessToken()).isEqualTo(newAccessStr);
        assertThat(response.refreshToken()).isEqualTo(newRefreshStr);

        // [수정 2] deleteById(String) -> delete(Entity) 검증으로 변경
        // 서비스 코드에서 refreshTokenRepository.delete(oldRefreshToken)를 호출하므로,
        // deleteById가 아니라 delete 메서드가 호출되었는지 확인해야 함.
        verify(refreshTokenRepository, times(1)).delete(mockRefreshTokenEntity);

        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("리프레시 토큰 재발급 실패 - 유효하지 않은 토큰 (검증 실패)")
    void refresh_Fail_InvalidToken() {
        // given
        String invalidToken = "invalid-token";
        RefreshTokenRequest request = new RefreshTokenRequest(invalidToken);

        given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(AuthException.InvalidRefreshToken.class);
    }

    @Test
    @DisplayName("리프레시 토큰 재발급 실패 - DB에 없는 토큰 (이미 삭제됨)")
    void refresh_Fail_TokenNotFound() {
        // given
        String notFoundToken = "not-found-token";
        RefreshTokenRequest request = new RefreshTokenRequest(notFoundToken);

        given(jwtTokenProvider.validateToken(notFoundToken)).willReturn(true);

        // [수정 3] findById(String) -> findByToken(String)으로 변경
        given(refreshTokenRepository.findByToken(notFoundToken))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(AuthException.RefreshTokenNotFound.class);
    }
}