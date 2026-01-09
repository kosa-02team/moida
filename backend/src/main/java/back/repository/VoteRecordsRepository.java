package back.repository;

import back.domain.VoteRecords;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteRecordsRepository extends JpaRepository<VoteRecords, Long> {

    /**
     * 특정 투표에서 사용자가 선택한 옵션들을 조회합니다.
     */
    List<VoteRecords> findByVoteIdAndUserId(Long voteId, Long userId);

    /**
     * 특정 투표에서 특정 옵션을 선택한 사용자 수를 조회합니다.
     */
    long countByVoteIdAndOptionId(Long voteId, Long optionId);
}
