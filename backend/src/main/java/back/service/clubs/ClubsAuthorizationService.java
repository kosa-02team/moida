package back.service.clubs;

import back.domain.Clubs;
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
        // 모임장 또는 운영진 권한 확인
        
        // 1. 모임장 확인 (Clubs.ownerId)
        Clubs club = clubsRepository.findById(clubId)
                .orElseThrow(ClubAuthException.NotActive::new);
        boolean isOwner = club.getOwnerId().equals(userId);
        
        // 2. 운영진 확인 (ClubMembers.role = "STAFF")
        String role = clubMembersRepository.findActiveRole(clubId, userId)
                .orElseThrow(ClubAuthException.NotActive::new);
        boolean isStaff = "STAFF".equals(role);
        
        // 3. 모임장 또는 운영진만 허용
        if (!isOwner && !isStaff) {
            throw new ClubAuthException.RoleInsufficient();
        }
    }
}
