package back.config.security;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "custom.jwt.secretKey=thisIsATestSecretKeyForJwtTokenProviderTesting1234",
        "custom.jwt.accessToken.expirationSeconds=3600"
})
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("토큰 생성및 토큰에서 데이터 추출 테스트")
    void createAndParseToken() {
        //Given loginId와 role 변수 만들기
        String loginId = "testUser";
        String role = "USER";
        Long userId = 1L;
        //When jwtTokenProvider를 사용해 토큰 만들기
        String token = jwtTokenProvider.createAccessToken(loginId, role, userId);

        //Then 만든 토큰에서 다시 정보를 꺼내서 원래 값과 같은지 확인
        String extractedLoginId = jwtTokenProvider.getLoginId(token);
        Assertions.assertThat(extractedLoginId).isEqualTo(loginId);

        String extractedRole = jwtTokenProvider.getRole(token);
        Assertions.assertThat(extractedRole).isEqualTo(role);
    }
}