package back.repository;

import back.domain.VoteOptions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteOptionsRepository extends JpaRepository<VoteOptions, Long> {
}
