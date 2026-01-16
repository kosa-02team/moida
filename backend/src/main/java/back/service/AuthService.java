package back.service;

import back.config.security.JwtTokenProvider;
import back.config.security.RefreshToken;
import back.domain.Users;
import back.dto.LoginRequest;
import back.dto.auth.SignupRequest;
import back.dto.auth.RefreshTokenResponse;
import back.exception.AuthException;
import back.repository.RefreshTokenRepository;
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
    private final RefreshTokenRepository refreshTokenRepository;

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
        return jwtTokenProvider.createAccessToken(user.getLoginId(), user.getSystemRole(), user.getUserId());
    }

    @Transactional
    public Long signup(SignupRequest signupRequest) {

        //이미 가입된 사용자가 있을 경우
        if (userRepository.existsByLoginId(signupRequest.loginId())) {
            throw new AuthException.LoginIdDuplicated();
        }

        String encodedPassword = passwordEncoder.encode(signupRequest.password());

        Users users = new Users(signupRequest.loginId(), encodedPassword, signupRequest.realName());

        Users savedUser = userRepository.save(users);

        return savedUser.getUserId();
    }

    public RefreshTokenResponse refresh(String refreshToken) {
        if (jwtTokenProvider.validateToken(refreshToken)) {
            RefreshToken oldRefreshToken = refreshTokenRepository.findById(refreshToken)
                    .orElseThrow(() -> new AuthException.RefreshTokenNotFound());

            Users user = oldRefreshToken.getUser();
            String newAccessToken = jwtTokenProvider.createAccessToken(user.getLoginId(), user.getSystemRole(), user.getUserId());
            String newRefreshToken = jwtTokenProvider.createRefreshToken();

            refreshTokenRepository.save(new RefreshToken(newRefreshToken, user));
            refreshTokenRepository.deleteById(refreshToken);

            return new RefreshTokenResponse(newAccessToken, newRefreshToken);
        }
        throw new AuthException.InvalidRefreshToken();
    }
}
