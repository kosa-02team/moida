package back.repository.ledger;

import back.domain.ledger.AuditLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogsRepository extends JpaRepository<AuditLogs, Long> {
    List<AuditLogs> findByTransactionIdOrderByCreatedAtDesc(Long transactionId);

    // 클럽별 로그 조회를 위해 TransactionLog와 조인이 필요할 수도 있으나,
    // 우선 transactionId 기반 조회를 기본으로 합니다.
}
