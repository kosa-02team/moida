package back.controller;

import back.dto.LoginRequest;
import back.dto.auth.RefreshTokenRequest;
import back.dto.auth.RefreshTokenResponse;
import back.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // 시큐리티 필터 비활성화 (순수 컨트롤러 로직 검증)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Spring Boot 3.4+ 대응: @MockBean -> @MockitoBean
    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("로그인 성공 시 토큰 응답 반환")
    void login_Success() throws Exception {
        // given
        // [중요] LoginRequest의 loginId는 @Email 검증이 있으므로 이메일 형식을 지켜야 함
        LoginRequest request = new LoginRequest("test@email.com", "password1234");
        RefreshTokenResponse response = new RefreshTokenResponse("access-token", "refresh-token");

        // 서비스가 호출되면 준비된 response를 반환하도록 Mocking
        given(authService.login(any(LoginRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                // SuccessResponse 구조에 따라 data 필드 하위 검증
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("리프레시 토큰 재발급 요청 성공")
    void refresh_Success() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
        RefreshTokenResponse response = new RefreshTokenResponse("new-access-token", "new-refresh-token");

        // [중요] 서비스 메서드 파라미터가 String이 아닌 DTO(RefreshTokenRequest)로 변경됨
        given(authService.refresh(any(RefreshTokenRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));

        // [검증] 컨트롤러가 서비스에 DTO를 잘 넘겨줬는지 확인
        verify(authService).refresh(any(RefreshTokenRequest.class));
    }
}