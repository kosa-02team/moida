package back.service;

import back.config.security.JwtTokenProvider;
import back.config.security.RefreshToken;
import back.domain.Users;
import back.dto.LoginRequest;
import back.dto.auth.RefreshTokenRequest;
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
    public RefreshTokenResponse login(LoginRequest loginRequest) {
        Users user = userRepository.findByLoginId(loginRequest.loginId())
                .orElseThrow(() -> new AuthException.UserNotFound());

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new AuthException.LoginFailed();
        }
        String accessToken = jwtTokenProvider.createAccessToken(user.getLoginId(), user.getSystemRole(), user.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken();

        refreshTokenRepository.save(new RefreshToken(refreshToken, user));

        return new RefreshTokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public Long signup(SignupRequest signupRequest) {
        if (userRepository.existsByLoginId(signupRequest.loginId())) {
            throw new AuthException.LoginIdDuplicated();
        }

        String encodedPassword = passwordEncoder.encode(signupRequest.password());
        Users users = new Users(signupRequest.loginId(), encodedPassword, signupRequest.realName());
        Users savedUser = userRepository.save(users);

        return savedUser.getUserId();
    }

    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest refreshTokenRequest) {
        String requestToken = refreshTokenRequest.refreshToken();

        if (!jwtTokenProvider.validateToken(requestToken)) {
            throw new AuthException.InvalidRefreshToken();
        }

        RefreshToken oldRefreshToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new AuthException.RefreshTokenNotFound());

        Users user = oldRefreshToken.getUser();

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getLoginId(), user.getSystemRole(), user.getUserId());
        String newRefreshTokenVal = jwtTokenProvider.createRefreshToken();

        refreshTokenRepository.save(new RefreshToken(newRefreshTokenVal, user));

        refreshTokenRepository.delete(oldRefreshToken);

        return new RefreshTokenResponse(newAccessToken, newRefreshTokenVal);
    }
}