package back.repository.club;

import back.domain.club.Clubs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Clubs, Long> {
    boolean existsByClubName(String clubName);

    Optional<Clubs> findByClubName(String clubName);

    long countByStatus(Clubs.Status status);

    Page<Clubs> findByStatus(Clubs.Status status, Pageable pageable);

    Page<Clubs> findByClubNameContaining(String clubName, Pageable pageable);
}