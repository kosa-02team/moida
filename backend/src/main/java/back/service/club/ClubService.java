package back.service.club;

import back.domain.club.ClubMembers;
import back.domain.club.Clubs;
import back.dto.club.ClubRequest;
import back.dto.club.ClubResponse;
import back.exception.ClubException;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;

    @Transactional
    public ClubResponse createClub(ClubRequest request, Long ownerId) {
        if (clubRepository.existsByClubName(request.getClubName())) {
            throw new ClubException.AlreadyExists();
        }

        Clubs club = new Clubs(
                request.getClubName(),
                ownerId,
                request.getTypeEnum(),
                request.getMaxMembers() != null ? request.getMaxMembers() : 100
        );
        club.setVisibility(request.getVisibilityEnum());
        Clubs savedClub = clubRepository.save(club);
        return ClubResponse.from(savedClub, 0);
    }

    public ClubResponse getClub(Long clubId, Long viewerId) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        boolean isPublic = club.getVisibility() == Clubs.Visibility.PUBLIC;
        boolean isMember = viewerId != null && 
                clubMemberRepository.existsByClubIdAndUserIdAndStatus(clubId, viewerId, ClubMembers.Status.ACTIVE);

        if (isPublic || isMember) {
            Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE);
            return ClubResponse.full(club, currentMembers);
        } else {
            return ClubResponse.limited(club);
        }
    }

    public ClubResponse getClub(Long clubId) {
        return getClub(clubId, null);
    }

    @Transactional
    public ClubResponse updateClub(Long clubId, ClubRequest request, Long ownerId) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        if (!club.getClubName().equals(request.getClubName()) 
                && clubRepository.existsByClubName(request.getClubName())) {
            throw new ClubException.AlreadyExists();
        }

        club.updateName(request.getClubName());
        club.setVisibility(request.getVisibilityEnum());
        if (request.getType() != null) {
            club.setType(request.getTypeEnum());
        }
        if (request.getMaxMembers() != null) {
            club.setMaxMembers(request.getMaxMembers());
        }

        Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE);
        return ClubResponse.from(club, currentMembers);
    }

    @Transactional
    public void closeClub(Long clubId, Long ownerId) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        club.close();
    }

    @Transactional
    public void activateClub(Long clubId, Long ownerId) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        club.activate();
    }
}
