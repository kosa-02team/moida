package back.bank.repository;

import back.bank.domain.Banks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<Banks, Long> {
    Optional<Banks> findByBankCode(String bankCode);

}
