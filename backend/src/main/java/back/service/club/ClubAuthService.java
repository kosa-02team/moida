package back.service.club;

import back.domain.club.ClubMembers;
import back.domain.club.Clubs;
import back.exception.ClubException;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubAuthService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;

    private Clubs getClubOrThrow(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);
    }

    public void assertActiveMember(Long clubId, Long userId) {
        if (!clubMemberRepository.existsByClubIdAndUserIdAndStatus(clubId, userId, ClubMembers.Status.ACTIVE)) {
            throw new ClubException.AuthNotActive();
        }
    }

    public void assertAtLeastManager(Long clubId, Long userId) {
        Clubs club = getClubOrThrow(clubId);

        if (club.getOwnerId().equals(userId)) {
            return;
        }
        
        ClubMembers.Role role = clubMemberRepository.findActiveRole(clubId, userId)
                .orElseThrow(ClubException.AuthNotActive::new);
        
        if (!role.isAtLeast(ClubMembers.Role.STAFF)) {
            throw new ClubException.AuthStaffRequired();
        }
    }

    public void assertAtLeastAccountant(Long clubId, Long userId) {
        Clubs club = getClubOrThrow(clubId);

        if (club.getOwnerId().equals(userId)) {
            return;
        }
        
        ClubMembers.Role role = clubMemberRepository.findActiveRole(clubId, userId)
                .orElseThrow(ClubException.AuthNotActive::new);
        
        if (!role.isAtLeast(ClubMembers.Role.ACCOUNTANT)) {
            throw new ClubException.AuthAccountantRequired();
        }
    }

    public void assertAtLeastStaffOrAccountant(Long clubId, Long userId) {
        Clubs club = getClubOrThrow(clubId);

        boolean isOwner = club.getOwnerId().equals(userId);
        
        ClubMembers.Role role = clubMemberRepository.findActiveRole(clubId, userId)
                .orElseThrow(ClubException.AuthNotActive::new);
        boolean isManagerLevel = role.isAtLeast(ClubMembers.Role.STAFF);
        
        if (!isOwner && !isManagerLevel) {
            throw new ClubException.AuthStaffRequired();
        }
    }

    public void assertAtLeastManager(Long clubId, Long userId, boolean dummy) {
        assertAtLeastStaffOrAccountant(clubId, userId);
    }

    public void validateAndGetClubForReadPosts(Long clubId, Long viewerId) {
        Clubs club = getClubOrThrow(clubId);

        if (club.getVisibility() == Clubs.Visibility.PUBLIC) {
            return;
        }

        if (viewerId == null) {
            throw new ClubException.AuthLoginRequired();
        }

        assertActiveMember(clubId, viewerId);
    }

    public void validateAndGetClubForUpdatePosts(Long clubId, Long updateId) {
        if (updateId == null) {
            throw new ClubException.AuthLoginRequired();
        }

        assertAtLeastManager(clubId, updateId);
    }

    public boolean isOwner(Long clubId, Long userId) {
        Clubs club = getClubOrThrow(clubId);
        boolean isOwner = club.getOwnerId().equals(userId);
        
        if (isOwner) {
            return true;
        }
        
        return clubMemberRepository.findActiveRole(clubId, userId)
                .map(role -> role == ClubMembers.Role.OWNER)
                .orElse(false);
    }
}
