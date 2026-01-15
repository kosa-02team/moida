package back.repository.vote;

import back.domain.vote.VoteOptions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteOptionRepository extends JpaRepository<VoteOptions, Long> {

    /**
     * 특정 투표의 모든 옵션을 순서대로 조회합니다.
     */
    List<VoteOptions> findByVoteIdOrderByOptionOrderAsc(Long voteId);

    /**
     * 특정 투표의 옵션 개수를 조회합니다.
     */
    long countByVoteId(Long voteId);
}
