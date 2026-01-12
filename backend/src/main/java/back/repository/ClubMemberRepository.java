package back.repository;

import back.domain.ClubMembers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMembers, Long> {

    boolean existsByClubIdAndUserId(Long clubId, Long userId);

    Optional<ClubMembers> findByClubIdAndUserId(Long clubId, Long userId);
    Optional<ClubMembers> findByClubIdAndMemberId(Long clubId, Long memberId);

}
