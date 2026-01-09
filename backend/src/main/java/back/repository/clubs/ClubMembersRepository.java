package back.repository.clubs;

import back.domain.ClubMembers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ClubMembersRepository extends JpaRepository<ClubMembers, Long> {
    @Query("""
    select cm.role from ClubMembers cm
    where cm.clubId = :clubId and cm.userId = :userId and cm.status = 'ACTIVE'
    """)
    Optional<String> findActiveRole(Long clubId, Long userId);

    boolean existsByClubIdAndUserIdAndStatus(Long clubId, Long userId, String status);
}

