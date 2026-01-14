package back.config.security;

import back.repository.ClubMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("clubSecurity")
@RequiredArgsConstructor
public class ClubSecurity {
    private final ClubMemberRepository clubMemberRepository;

    public boolean isOwner(Long clubId) {
        Long currentUserId = 1L; //로그인 연동 후 수정 예정
//        Long currentUserId = SecurityUtils.getCurrentUserId();

        return clubMemberRepository.findByClubIdAndUserId(clubId, currentUserId)
                .map(member -> member.hasRole("OWNER"))
                .orElse(false);

    }
}
