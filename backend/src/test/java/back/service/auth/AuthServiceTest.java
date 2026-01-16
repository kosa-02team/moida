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

@ExtendWith(MockitoExtension.class) // Mockito 확장 기능을 사용
class AuthServiceTest {

    @InjectMocks
    private AuthService authService; // 가짜 객체들을 주입받을 테스트 대상

    @Mock
    private RefreshTokenRepository refreshTokenRepository; // 가짜 저장소

    @Mock
    private JwtTokenProvider jwtTokenProvider; // 가짜 토큰 생성기

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("리프레시 토큰 재발급 성공 - RTR 정책(저장 및 삭제) 검증")
    void refresh_Success() {
        // given (준비)
        String oldTokenStr = "old-refresh-token";
        String newAccessStr = "new-access-token";
        String newRefreshStr = "new-refresh-token";

        // 요청 DTO 생성
        RefreshTokenRequest request = new RefreshTokenRequest(oldTokenStr);

        // 가짜 유저와 가짜 토큰 엔티티 생성
        Users mockUser = new Users("testUser", "12345678", "테스트");

        RefreshToken mockRefreshTokenEntity = new RefreshToken(oldTokenStr, mockUser);

        // Mock 동작 정의
        given(jwtTokenProvider.validateToken(oldTokenStr)).willReturn(true); // 1. 토큰 유효성 통과
        given(refreshTokenRepository.findById(oldTokenStr))
                .willReturn(Optional.of(mockRefreshTokenEntity)); // 2. DB에서 찾음

        given(jwtTokenProvider.createAccessToken(mockUser.getLoginId(), mockUser.getSystemRole(), mockUser.getUserId()))
                .willReturn(newAccessStr); // 3. 새 액세스 토큰 생성
        given(jwtTokenProvider.createRefreshToken())
                .willReturn(newRefreshStr); // 4. 새 리프레시 토큰 생성

        // when (실행)
        RefreshTokenResponse response = authService.refresh(request);

        // then (검증)
        assertThat(response.accessToken()).isEqualTo(newAccessStr);
        assertThat(response.refreshToken()).isEqualTo(newRefreshStr);

        // ** 핵심 RTR 로직 검증 **
        verify(refreshTokenRepository, times(1)).deleteById(oldTokenStr); // 헌 토큰 삭제되었는지?
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class)); // 새 토큰 저장되었는지?
    }

    @Test
    @DisplayName("리프레시 토큰 재발급 실패 - 유효하지 않은 토큰 (검증 실패)")
    void refresh_Fail_InvalidToken() {
        // given
        String invalidToken = "invalid-token";
        RefreshTokenRequest request = new RefreshTokenRequest(invalidToken);

        given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false); // 유효하지 않다고 설정

        // when & then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(AuthException.InvalidRefreshToken.class); // 우리가 만든 예외가 터져야 함
    }

    @Test
    @DisplayName("리프레시 토큰 재발급 실패 - DB에 없는 토큰 (이미 삭제됨)")
    void refresh_Fail_TokenNotFound() {
        // given
        String notFoundToken = "not-found-token";
        RefreshTokenRequest request = new RefreshTokenRequest(notFoundToken);

        given(jwtTokenProvider.validateToken(notFoundToken)).willReturn(true); // 검증은 통과했으나
        given(refreshTokenRepository.findById(notFoundToken))
                .willReturn(Optional.empty()); // DB에는 없음

        // when & then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(AuthException.RefreshTokenNotFound.class);
    }
}