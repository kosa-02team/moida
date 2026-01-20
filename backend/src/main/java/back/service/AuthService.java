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

import java.util.Optional;

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
        // loginId와 password 앞뒤 공백 제거
        String trimmedLoginId = loginRequest.loginId().trim();
        String trimmedPassword = loginRequest.password().trim();
        
        Users user = userRepository.findByLoginId(trimmedLoginId)
                .orElseThrow(() -> new AuthException.UserNotFound());

        // 탈퇴한 사용자 로그인 차단
        if ("DELETED".equals(user.getStatus()) || user.getDeletedAt() != null) {
            throw new AuthException.LoginFailed();
        }

        // 디버깅 로그: 로그인 시도 정보
        System.out.println("=== 로그인 시도 ===");
        System.out.println("loginId (원본): " + loginRequest.loginId());
        System.out.println("loginId (trim): " + trimmedLoginId);
        System.out.println("입력 비밀번호 길이 (원본): " + loginRequest.password().length());
        System.out.println("입력 비밀번호 길이 (trim): " + trimmedPassword.length());
        System.out.println("저장된 비밀번호 해시 전체 길이: " + (user.getPassword() != null ? user.getPassword().length() : 0));
        System.out.println("저장된 비밀번호 해시 (처음 60자): " + (user.getPassword() != null ? user.getPassword().substring(0, Math.min(60, user.getPassword().length())) : "null"));
        
        boolean passwordMatches = passwordEncoder.matches(trimmedPassword, user.getPassword());
        System.out.println("비밀번호 매칭 결과: " + passwordMatches);
        
        if (!passwordMatches) {
            System.out.println("==================");
            throw new AuthException.LoginFailed();
        }
        
        System.out.println("로그인 성공!");
        System.out.println("==================");
        String accessToken = jwtTokenProvider.createAccessToken(user.getLoginId(), user.getSystemRole(), user.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken();

        refreshTokenRepository.save(new RefreshToken(refreshToken, user));

        return new RefreshTokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public Long signup(SignupRequest signupRequest) {
        // loginId, password, realName 앞뒤 공백 제거
        String trimmedLoginId = signupRequest.loginId().trim();
        String trimmedPassword = signupRequest.password().trim();
        String trimmedRealName = signupRequest.realName().trim();
        
        // 활성화된 사용자(ACTIVE, BANNED)의 loginId 중복 체크
        // DELETED 상태 사용자는 같은 아이디로 재가입 가능
        if (userRepository.existsActiveByLoginId(trimmedLoginId)) {
            throw new AuthException.LoginIdDuplicated();
        }
        
        // 탈퇴한 사용자가 재가입하는 경우, 기존 사용자 정보를 재활성화
        Optional<Users> deletedUser = userRepository.findByLoginId(trimmedLoginId);
        if (deletedUser.isPresent() && "DELETED".equals(deletedUser.get().getStatus())) {
            Users user = deletedUser.get();
            user.activate(); // status를 ACTIVE로 변경, deletedAt을 null로 설정
            user.changePassword(passwordEncoder.encode(trimmedPassword)); // 새 비밀번호 설정
            user.updateProfile(trimmedRealName); // 새 이름으로 업데이트
            
            Users savedUser = userRepository.save(user);
            return savedUser.getUserId();
        }

        // 새로운 사용자 생성
        String encodedPassword = passwordEncoder.encode(trimmedPassword);
        // 디버깅을 위한 로그 (운영 환경에서는 제거 필요)
        System.out.println("=== 회원가입 ===");
        System.out.println("loginId (원본): " + signupRequest.loginId());
        System.out.println("loginId (trim): " + trimmedLoginId);
        System.out.println("입력 비밀번호 길이 (원본): " + signupRequest.password().length());
        System.out.println("입력 비밀번호 길이 (trim): " + trimmedPassword.length());
        System.out.println("인코딩된 비밀번호 해시 전체 길이: " + encodedPassword.length());
        System.out.println("인코딩된 비밀번호 해시 (처음 60자): " + encodedPassword.substring(0, Math.min(60, encodedPassword.length())));
        System.out.println("==================");
        Users users = new Users(trimmedLoginId, encodedPassword, trimmedRealName);
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