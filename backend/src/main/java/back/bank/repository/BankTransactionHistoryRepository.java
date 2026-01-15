package back.bank.repository;

import back.bank.domain.BankTransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BankTransactionHistoryRepository extends JpaRepository<BankTransactionHistory, Long> {
    List<BankTransactionHistory> findByClubIdAndBankTransactionAtBetween(
            Long clubId,
            LocalDateTime fromDate,
            LocalDateTime toDate);

    boolean existsByUniqueTxKey(String uniqueTxKey);
}
