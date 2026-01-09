package back.service.clubs;

import back.exception.ClubAuthException;
import back.repository.clubs.ClubMembersRepository;
import back.repository.clubs.ClubsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubsAuthorizationService {

    private final ClubsRepository clubsRepository;
    private final ClubMembersRepository clubMembersRepository;

    //모임 활성화 멤버인지
    public void assertActiveMember(Long clubId, Long userId) {
        if (!clubMembersRepository.existsByClubIdAndUserIdAndStatus(clubId, userId, "ACTIVE")) {
            throw new ClubAuthException.NotActive(); // 또는 NotActive로 통일
        }
    }

    public void assertAtLeastManager(Long clubId, Long userId) {
        String role = clubMembersRepository.findActiveRole(clubId, userId)
                .orElseThrow(ClubAuthException.NotActive::new);

        if (!role.equals("MANAGER") && !role.equals("OWNER")) {
            throw new ClubAuthException.RoleInsufficient(); // (권장) Forbidden보다 의미 정확
        }
    }
}
