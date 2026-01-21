package back.service.member;

import back.domain.club.ClubMembers;
import back.domain.club.Clubs;
import back.dto.club.MyClubResponse;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;

    /**
     * 내 모임 목록 조회
     */
    public List<MyClubResponse> getMyClubs(Long userId) {
        List<ClubMembers> memberships = clubMemberRepository.findByUserIdAndStatus(
                userId, ClubMembers.Status.ACTIVE);
        
        return memberships.stream()
                .map(member -> {
                    Clubs club = clubRepository.findById(member.getClubId())
                            .orElse(null);
                    if (club == null) return null;
                    return MyClubResponse.from(club, member);
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }
}
