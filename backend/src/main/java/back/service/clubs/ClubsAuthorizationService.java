package back.service.clubs;

import back.domain.ClubMembers;
import back.domain.Clubs;
import back.exception.ClubAuthException;
import back.repository.clubs.ClubMembersRepository;
import back.repository.clubs.ClubsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubsAuthorizationService {

    private final ClubsRepository clubsRepository;
    private final ClubMembersRepository clubMembersRepository;

    private Clubs getClubOrThrow(Long clubId) {
        return clubsRepository.findById(clubId)
                .orElseThrow(ClubAuthException.NotFound::new);
    }

    //모임 활성화 멤버인지
    public void assertActiveMember(Long clubId, Long userId) {
        if (!clubMembersRepository.existsByClubIdAndUserIdAndStatus(clubId, userId, ClubMembers.Status.ACTIVE)) {
            throw new ClubAuthException.NotActive(); // 또는 NotActive로 통일
        }
    }

    public void assertAtLeastManager(Long clubId, Long userId) {

        Clubs club = getClubOrThrow(clubId);

        // 모임장 또는 운영진 권한 확인
        boolean isOwner = club.getOwnerId().equals(userId);
        
        // 2. 운영진 확인 (ClubMembers.role = "STAFF")
        List<String> roles = clubMembersRepository.findActiveRoles(clubId, userId)
                .orElseThrow(ClubAuthException.NotActive::new);
        boolean isStaff = roles.contains("STAFF");
        
        // 3. 모임장 또는 운영진만 허용
        if (!isOwner && !isStaff) {
            throw new ClubAuthException.RoleInsufficient();
        }
    }

    public void validateAndGetClubForReadPosts(Long clubId, Long viewerId) {
        Clubs club = getClubOrThrow(clubId);

        if ("PUBLIC".equals(club.getVisibility())) {
            return;
        }

        if (viewerId == null) {
            throw new ClubAuthException.LoginRequired();
        }

        assertActiveMember(clubId, viewerId); // 비공개면 멤버만 허용
    }

    public void validateAndGetClubForUpdatePosts(Long clubId, Long updateId) {
        if (updateId == null) {
            throw new ClubAuthException.LoginRequired();
        }

        assertAtLeastManager(clubId, updateId);
    }

}
