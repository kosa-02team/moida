package back.bank.repository;

import back.bank.domain.BankTransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransactionHistoryRepository extends JpaRepository<BankTransactionHistory, Long> {
}
