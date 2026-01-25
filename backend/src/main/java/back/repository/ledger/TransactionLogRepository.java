package back.repository.ledger;

import back.domain.ledger.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    /**
     * 특정 모임의 가장 최근 거래 조회
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.clubId = :clubId ORDER BY tl.createdAt DESC LIMIT 1")
    Optional<TransactionLog> findLatestByClubId(@Param("clubId") Long clubId);

    List<TransactionLog> findByScheduleId(Long scheduleId);

    // 날짜 범위 조회 (최신순 정렬: createdAt DESC, transactionId DESC)
    List<TransactionLog> findByClubIdAndCreatedAtBetweenOrderByCreatedAtDescTransactionIdDesc(Long clubId, LocalDateTime start, LocalDateTime end);

    // 일정별 조회 (최신순 정렬: createdAt DESC, transactionId DESC)
    List<TransactionLog> findByClubIdAndScheduleIdOrderByCreatedAtDescTransactionIdDesc(Long clubId, Long scheduleId);

    // 스냅샷 이후의 특정 타입(WITHDRAW) 거래 조회
    List<TransactionLog> findByClubIdAndTransactionIdGreaterThanAndType(Long clubId, Long transactionId, String type);

    // bankHistoryId로 TransactionLog 조회
    Optional<TransactionLog> findByBankHistoryId(Long bankHistoryId);

    // 수동 거래(BankHistoryId IS NULL) 중 특정 시점 이전의 최신 거래 조회
    Optional<TransactionLog> findFirstByClubIdAndBankHistoryIdIsNullAndCreatedAtBeforeOrderByCreatedAtDescTransactionIdDesc(
            Long clubId, LocalDateTime createdAt);
}
