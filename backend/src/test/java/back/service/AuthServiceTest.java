package back.service;

import back.domain.Users;
import back.dto.SignupRequest;
import back.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signupSuccess() {
        //given
        SignupRequest request = new SignupRequest("testUser", "12345678", "테스트");

        given(userRepository.existsByLoginId(request.loginId())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPw");

        Users savedUser = new Users("testUser", "encodedPw", "테스트");

        given(userRepository.save(any(Users.class))).willReturn(savedUser);

        //when
        authService.signup(request);

        //then
        verify(userRepository).save(any(Users.class));
    }
}