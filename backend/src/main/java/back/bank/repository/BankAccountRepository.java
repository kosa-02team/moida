package back.bank.repository;

import back.bank.domain.BankAccounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccounts, Long> {
    Optional<BankAccounts> findByClubId(Long clubId);
}
