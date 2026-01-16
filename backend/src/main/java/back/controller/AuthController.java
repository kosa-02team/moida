package back.controller;

import back.common.response.SuccessResponse;
import back.dto.LoginRequest;
import back.dto.auth.RefreshTokenRequest;
import back.dto.auth.RefreshTokenResponse;
import back.dto.auth.SignupRequest;
import back.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<String>> login(@RequestBody
                                                         @Valid
                                                         LoginRequest loginRequest) {
        String token = authService.login(loginRequest);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, token));
    }

    @PostMapping("/signup")
    public ResponseEntity<SuccessResponse<Long>> signup(@RequestBody
                                                        @Valid
                                                        SignupRequest signupRequest) {
        Long savedUserId = authService.signup(signupRequest);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, savedUserId));
    }

    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<RefreshTokenResponse>> refresh(@RequestBody
                                                                         RefreshTokenRequest refreshTokenRequest) {
        RefreshTokenResponse refreshTokenResponse = authService.refresh(refreshTokenRequest.refreshToken());
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, refreshTokenResponse));
    }


}
