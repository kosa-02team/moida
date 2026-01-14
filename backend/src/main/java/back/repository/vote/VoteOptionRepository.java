package back.repository.vote;

import back.domain.vote.VoteOptions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteOptionRepository extends JpaRepository<VoteOptions, Long> {
}
