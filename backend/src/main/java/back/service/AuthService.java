package back.service;

import back.config.security.JwtTokenProvider;
import back.domain.Users;
import back.dto.LoginRequest;
import back.exception.AuthException;
import back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public String login(LoginRequest loginRequest) {
        // 1. 사용자 존재 여부 확인
        Users user = userRepository.findByLoginId(loginRequest.loginId())
                .orElseThrow(() -> new AuthException.UserNotFound());

        // 2. 비밀번호 일치 확인
        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new AuthException.LoginFailed();
        }
        // 3. 토큰 생성 및 반환
        return jwtTokenProvider.createAccessToken(user.getLoginId(), user.getSystemRole());
    }
}
