package back.bank.repository;

import back.bank.domain.BankAccounts;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccounts, Long> {
}
