package back.repository.vote;

import back.domain.vote.VoteRecords;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteRecordRepository extends JpaRepository<VoteRecords, Long> {

    /**
     * 특정 투표에서 사용자가 선택한 옵션들을 조회합니다.
     */
    List<VoteRecords> findByVoteIdAndUserId(Long voteId, Long userId);

    /**
     * 특정 투표에서 특정 옵션을 선택한 사용자 수를 조회합니다.
     */
    long countByVoteIdAndOptionId(Long voteId, Long optionId);

    /**
     * 특정 투표의 총 투표 기록 수를 조회합니다 (중복 포함).
     */
    long countByVoteId(Long voteId);

    /**
     * 특정 투표에서 투표한 고유 사용자 수를 조회합니다.
     */
    @Query("SELECT COUNT(DISTINCT vr.userId) FROM VoteRecords vr WHERE vr.voteId = :voteId")
    long countDistinctUsersByVoteId(@Param("voteId") Long voteId);

    /**
     * 특정 옵션을 선택한 사용자 수를 조회합니다.
     */
    long countByOptionId(Long optionId);
}
