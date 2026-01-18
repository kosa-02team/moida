package back.controller.ledger;

import back.domain.ledger.TransactionLog;
import back.service.ledger.LedgerService;
import back.controller.ledger.ManualTransactionRequest;
import back.controller.ledger.TransactionUpdateRequest;
// import back.common.response.SuccessResponse; // Removed if not exists
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/clubs/{clubId}/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    /**
     * 장부 내역 조회 (필터링 포함)
     * GET /clubs/{clubId}/ledger?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping
    public ResponseEntity<List<TransactionLog>> getLedger(
            @PathVariable Long clubId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long scheduleId) {

        // 날짜 없으면 이번 달 1일 ~ 오늘로 기본 설정
        if (startDate == null)
            startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null)
            endDate = LocalDate.now();

        List<TransactionLog> logs = ledgerService.getTransactions(clubId, startDate, endDate, scheduleId);
        return ResponseEntity.ok(logs);
    }

    /**
     * 수동 장부 기록 (현금 사용 등)
     * POST /clubs/{clubId}/ledger/manual
     */
    @PostMapping("/manual")
    public ResponseEntity<Void> createManualRecord(
            @PathVariable Long clubId,
            @RequestBody ManualTransactionRequest request) {
        // TODO: 로그인된 사용자 ID 가져오기
        Long editorId = 1L;
        ledgerService.createManualTransaction(clubId, request, editorId);
        return ResponseEntity.ok().build();
    }

    /**
     * 장부 내역 수정 (메모 수정 등)
     */
    @PatchMapping("/{transactionId}")
    public ResponseEntity<Void> updateTransaction(
            @PathVariable Long clubId,
            @PathVariable Long transactionId,
            @RequestBody TransactionUpdateRequest request) {
        ledgerService.updateTransaction(transactionId, request);
        return ResponseEntity.ok().build();
    }
}