package back.bank.repository;

import back.bank.domain.BankTransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransactionHistoryRepository extends JpaRepository<BankTransactionHistory, Long> {
        List<BankTransactionHistory> findByClubIdAndBankTransactionAtBetweenOrderByBankTransactionAtDescHistoryIdDesc(
                        Long clubId,
                        LocalDateTime fromDate,
                        LocalDateTime toDate);

        List<BankTransactionHistory> findByClubIdAndIsMatchedFalse(Long clubId);

        boolean existsByUniqueTxKey(String uniqueTxKey);

        Optional<BankTransactionHistory> findByUniqueTxKey(String uniqueTxKey);

        // 실제 거래 날짜 기준 이전 거래 조회 (같은 날짜면 ID 역순)
        @org.springframework.data.jpa.repository.Query("SELECT h FROM BankTransactionHistory h " +
                        "WHERE h.clubId = :clubId " +
                        "AND (h.bankTransactionAt < :date OR (h.bankTransactionAt = :date AND h.historyId < :historyId)) "
                        +
                        "ORDER BY h.bankTransactionAt DESC, h.historyId DESC")
        List<BankTransactionHistory> findPreviousHistory(
                        @org.springframework.data.repository.query.Param("clubId") Long clubId,
                        @org.springframework.data.repository.query.Param("date") LocalDateTime date,
                        @org.springframework.data.repository.query.Param("historyId") Long historyId,
                        org.springframework.data.domain.Pageable pageable);
}
