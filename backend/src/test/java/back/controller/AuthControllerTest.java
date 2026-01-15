package back.controller;

import back.dto.LoginRequest;
import back.domain.Users;
import back.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("올바른 아이디와 비밀번호를 보내면 토큰을 반환하는 테스트")
    void loginSuccess() throws Exception {
        //given
        String loginId = "testUser@test.com";
        String rawPassword = "12345678";
        String realName = "테스트유저";

        Users user = new Users(loginId, passwordEncoder.encode(rawPassword), realName);
        userRepository.save(user);
        //When
        LoginRequest loginRequest = new LoginRequest(loginId, rawPassword);
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))

                //Then
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());

    }

}