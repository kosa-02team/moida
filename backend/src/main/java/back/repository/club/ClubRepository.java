package back.repository.club;

import back.domain.club.Clubs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Clubs, Long> {
    boolean existsByClubName(String clubName);
    Optional<Clubs> findByClubName(String clubName);
}