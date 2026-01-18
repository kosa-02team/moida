package back.controller.club;

import back.config.security.UserPrincipal;
import back.exception.ClubException;
import back.service.club.ClubAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Component;

@Component("clubSecurity")
@RequiredArgsConstructor
public class ClubAuthorization {
    private final ClubAuthService clubAuthService;

    public boolean isOwner(Long clubId) {
        Long currentUserId = 1L; 
        // 로그인 연동 시 아래 주석 해제하고 위 코드 삭제
        // Long currentUserId = SecurityUtils.getCurrentUserId();

        return clubAuthService.isOwner(clubId, currentUserId);
    }

    public Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new ClubException.AuthLoginRequired();
        }
        return principal.getUserId();
    }

    public Long requireOwner(Long clubId, UserPrincipal principal) {
        Long userId = requireUserId(principal);
        if (!clubAuthService.isOwner(clubId, userId)) {
            throw new ClubException.AuthNotOwner();
        }
        return userId;
    }

    public void assertOwner(Long clubId, Long userId) {
        if (!clubAuthService.isOwner(clubId, userId)) {
            throw new ClubException.AuthNotOwner();
        }
    }
}
