package back.repository;

import back.domain.Votes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotesRepository extends JpaRepository<Votes, Long> {
}
