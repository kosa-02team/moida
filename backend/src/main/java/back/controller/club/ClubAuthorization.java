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
        // SecurityContextHolder에서 현재 인증된 사용자 정보 가져오기
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            return false;
        }

        Long currentUserId;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal) {
            currentUserId = ((UserPrincipal) principal).getUserId();
        } else {
            return false;
        }

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
