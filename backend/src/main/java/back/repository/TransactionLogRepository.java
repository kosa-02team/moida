package back.repository;

import back.domain.ledger.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    /**
     * 특정 모임의 가장 최근 거래 조회
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.clubId = :clubId ORDER BY tl.createdAt DESC LIMIT 1")
    Optional<TransactionLog> findLatestByClubId(@Param("clubId") Long clubId);
}
