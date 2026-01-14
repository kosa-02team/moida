package back.bank.repository;

import back.bank.domain.Banks;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankRepository extends JpaRepository<Banks, Long> {
    Optional<Banks> findByBankCode(String bankCode);

}
