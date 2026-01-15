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

    /**
     * 모임 활성화 멤버인지 확인
     */
    public void assertActiveMember(Long clubId, Long userId) {
        if (!clubMembersRepository.existsByClubIdAndUserIdAndStatus(clubId, userId, ClubMembers.Status.ACTIVE)) {
            throw new ClubAuthException.NotActive();
        }
    }

    /**
     * 운영진 이상 권한 확인 (모임장, 총무, 운영진)
     * 권한 계층: 멤버 < 운영진(STAFF) < 총무(ACCOUNTANT) < 모임장
     */
    public void assertAtLeastManager(Long clubId, Long userId) {
        Clubs club = getClubOrThrow(clubId);

        // 1. 모임장 확인
        boolean isOwner = club.getOwnerId().equals(userId);
        
        // 2. 운영진 또는 총무 확인 (총무는 운영진 권한 포함)
        List<String> roles = clubMembersRepository.findActiveRoles(clubId, userId)
                .orElseThrow(ClubAuthException.NotActive::new);
        boolean isStaff = roles.contains("STAFF");
        boolean isAccountant = roles.contains("ACCOUNTANT");
        
        // 3. 모임장, 총무, 운영진 중 하나라도 해당되면 허용
        if (!isOwner && !isAccountant && !isStaff) {
            throw new ClubAuthException.StaffRequired();
        }
    }

    /**
     * 총무 이상 권한 확인 (모임장 또는 ACCOUNTANT)
     * - 돈 관련 작업에 사용 (참가비 있는 일정 생성/수정, 정산 등)
     */
    public void assertAtLeastAccountant(Long clubId, Long userId) {
        Clubs club = getClubOrThrow(clubId);

        // 1. 모임장 확인
        boolean isOwner = club.getOwnerId().equals(userId);
        
        // 2. 총무 확인 (ClubMembers.role = "ACCOUNTANT")
        List<String> roles = clubMembersRepository.findActiveRoles(clubId, userId)
                .orElseThrow(ClubAuthException.NotActive::new);
        boolean isAccountant = roles.contains("ACCOUNTANT");
        
        // 3. 모임장 또는 총무만 허용
        if (!isOwner && !isAccountant) {
            throw new ClubAuthException.AccountantRequired();
        }
    }

    /**
     * 운영진 또는 총무 이상 권한 확인 (모임장, STAFF, ACCOUNTANT 모두 가능)
     */
    public void assertAtLeastStaffOrAccountant(Long clubId, Long userId) {
        Clubs club = getClubOrThrow(clubId);

        // 1. 모임장 확인
        boolean isOwner = club.getOwnerId().equals(userId);
        
        // 2. 운영진 또는 총무 확인
        List<String> roles = clubMembersRepository.findActiveRoles(clubId, userId)
                .orElseThrow(ClubAuthException.NotActive::new);
        boolean isStaff = roles.contains("STAFF");
        boolean isAccountant = roles.contains("ACCOUNTANT");
        
        // 3. 모임장, 운영진, 총무 중 하나라도 해당되면 허용
        if (!isOwner && !isStaff && !isAccountant) {
            throw new ClubAuthException.StaffRequired();
        }
    }

    // 기존 호환성 유지
    public void assertAtLeastManager(Long clubId, Long userId, boolean dummy) {
        assertAtLeastStaffOrAccountant(clubId, userId);
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
