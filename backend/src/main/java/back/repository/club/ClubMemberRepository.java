package back.repository.club;

import back.domain.club.ClubMembers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMembers, Long> {

    boolean existsByClubIdAndUserId(Long clubId, Long userId);
    boolean existsByClubIdAndUserIdAndStatus(Long clubId, Long userId, ClubMembers.Status status);
    boolean existsByClubIdAndNickname(Long clubId, String nickname);

    Optional<ClubMembers> findByClubIdAndUserId(Long clubId, Long userId);
    Optional<ClubMembers> findByClubIdAndMemberId(Long clubId, Long memberId);
    Optional<ClubMembers> findByClubIdAndUserIdAndStatus(Long clubId, Long userId, ClubMembers.Status status);

    default Optional<ClubMembers.Role> findActiveRole(Long clubId, Long userId) {
        return findByClubIdAndUserIdAndStatus(clubId, userId, ClubMembers.Status.ACTIVE)
                .map(ClubMembers::getRole);
    }

    long countByClubIdAndStatus(Long clubId, ClubMembers.Status status);

    List<ClubMembers> findByClubIdAndStatus(Long clubId, ClubMembers.Status status);
}