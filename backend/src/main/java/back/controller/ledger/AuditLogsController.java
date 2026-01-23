package back.controller.ledger;

import back.domain.ledger.AuditLogs;
import back.repository.ledger.AuditLogsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs/{clubId}/audit-logs")
@RequiredArgsConstructor
public class AuditLogsController {

    private final AuditLogsRepository auditLogsRepository;

    /**
     * 특정 거래내역의 변경 이력 조회
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<List<AuditLogs>> getAuditLogs(
            @PathVariable Long clubId,
            @PathVariable Long transactionId) {
        return ResponseEntity.ok(auditLogsRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId));
    }
}
