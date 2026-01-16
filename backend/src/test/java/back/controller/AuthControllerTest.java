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

// 1. WebMvcTest: 컨트롤러만 집중적으로 테스트 (Service, Repository 빈은 로드 안 함)
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // 2. Spring Security 필터 비활성화 (순수 컨트롤러 로직만 테스트)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc; // 3. 가짜 HTTP 요청을 보내는 도구

    @Autowired
    private ObjectMapper objectMapper; // 4. 객체 <-> JSON 변환 도구

    @MockitoBean
    private AuthService authService; // 5. 서비스는 가짜(Mock)로 대체

    @Test
    @DisplayName("로그인 성공 시 토큰 응답 반환")
    void login_Success() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@email.com", "password1234"); // 이메일 형식으로 변경
        RefreshTokenResponse response = new RefreshTokenResponse("access-token", "refresh-token");

        // 서비스가 호출되면 가짜 응답을 주도록 설정
        given(authService.login(any(LoginRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))) // 객체를 JSON 문자열로 변환
                .andDo(print()) // 테스트 로그 출력
                .andExpect(status().isOk()) // HTTP 200 확인
                // JSON 응답 검증 (SuccessResponse 구조에 따라 경로는 달라질 수 있음)
                // 예: SuccessResponse 안에 'data' 필드에 결과가 들어간다고 가정
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("리프레시 토큰 재발급 요청 성공")
    void refresh_Success() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
        RefreshTokenResponse response = new RefreshTokenResponse("new-access-token", "new-refresh-token");

        // 서비스의 refresh 메서드가 호출되면, 준비한 response를 반환해라!
        given(authService.refresh(any(RefreshTokenRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                // 값이 제대로 넘어왔는지 확인
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));

        // 실제로 서비스가 호출되었는지 검증 (중요: DTO가 잘 넘어갔는지 확인)
        verify(authService).refresh(any(RefreshTokenRequest.class));
    }
}