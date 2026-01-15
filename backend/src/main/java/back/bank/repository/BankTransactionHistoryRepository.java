package back.bank.repository;

import back.bank.domain.BankTransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BankTransactionHistoryRepository extends JpaRepository<BankTransactionHistory, Long> {
    List<BankTransactionHistory> findByClubIdAndBankTransactionAtBetween(
            Long clubId,
            LocalDateTime fromDate,
            LocalDateTime toDate);

    boolean existsByUniqueTxKey(String uniqueTxKey);
}
