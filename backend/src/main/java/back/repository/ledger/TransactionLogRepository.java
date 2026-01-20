package back.repository.ledger;

import back.domain.ledger.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    /**
     * 특정 모임의 가장 최근 거래 조회
     */
    Optional<TransactionLog> findFirstByClubIdOrderByCreatedAtDesc(Long clubId);

    List<TransactionLog> findByScheduleId(Long scheduleId);

    // 날짜 범위 조회
    List<TransactionLog> findByClubIdAndCreatedAtBetween(Long clubId, LocalDateTime start, LocalDateTime end);

    // 일정별 조회
    List<TransactionLog> findByClubIdAndScheduleId(Long clubId, Long scheduleId);
}
