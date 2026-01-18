package back.bank.controller;

import back.bank.domain.BankAccounts;
import back.bank.domain.BankTransactionHistory;
import back.domain.ledger.PaymentRequest;
import back.bank.dto.request.AccountCreateRequest;
import back.dto.ledger.request.RefundRequest;
import back.dto.ledger.response.ProcessedTransactionResponse;
import back.dto.ledger.response.RefundResponse;
import back.bank.repository.BankTransactionHistoryRepository;
import back.repository.ledger.PaymentRequestRepository;
import back.bank.service.BankService;
import back.domain.ledger.TransactionLog;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clubs/{clubId}/bank")
public class BankAccountController {

    private final BankService bankService;
    private final BankTransactionHistoryRepository transactionHistoryRepository;
    private final PaymentRequestRepository paymentRequestRepository;

    public BankAccountController(BankService bankService,
            BankTransactionHistoryRepository transactionHistoryRepository,
            PaymentRequestRepository paymentRequestRepository) {
        this.bankService = bankService;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.paymentRequestRepository = paymentRequestRepository;
    }

    /**
     * 모임 가상계좌 생성
     * POST /clubs/{clubId}/bank/accounts
     */
    @PostMapping("/accounts")
    public ResponseEntity<BankAccounts> createAccount(
            @PathVariable Long clubId,
            @RequestBody AccountCreateRequest request) {
        BankAccounts account = bankService.createAccount(clubId, request);
        return ResponseEntity.ok(account);
    }

    /**
     * 모임 가상계좌 거래내역 조회 및 동기화
     * - 오픈뱅킹 API를 호출하여 실제 은행 거래내역을 가져옴
     * - BankTransactionHistory와 TransactionLog에 저장
     * - from/to가 없으면 마지막 거래 이후 ~ 현재까지 자동 동기화
     * <p>
     * GET /api/clubs/{clubId}/bank/sync (자동 날짜 범위)
     * GET /api/clubs/{clubId}/bank/sync?from=2026-01-01&to=2026-01-31 (수동 날짜 범위)
     */
    @PostMapping("/sync")
    public ResponseEntity<List<TransactionLog>> syncTransactions(
            @PathVariable Long clubId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<TransactionLog> transactionLogs = bankService.syncTransactions(clubId, from, to);
        return ResponseEntity.ok(transactionLogs);
    }

    @PostMapping("/sync/{stubId}")
    public ResponseEntity<List<TransactionLog>> syncTransactionsStub(
            @PathVariable Long clubId,
            @PathVariable Long stubId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<TransactionLog> transactionLogs = bankService.syncTransactionsStub(clubId, stubId, from, to);
        return ResponseEntity.ok(transactionLogs);
    }

    /**
     * 처리된 거래내역 조회 (매칭 정보 포함)
     * - 오픈뱅킹 원본 + 매칭 정보 반환
     * <p>
     * GET /clubs/{clubId}/bank/transactions/processed?from=2026-01-01&to=2026-01-31
     */
    @GetMapping("/transactions/processed")
    public ResponseEntity<List<ProcessedTransactionResponse>> getProcessedTransactions(
            @PathVariable Long clubId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        // 1. 거래내역 조회
        List<BankTransactionHistory> histories = transactionHistoryRepository
                .findByClubIdAndBankTransactionAtBetween(
                        clubId,
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay());

        // 2. 매칭된 입금요청 조회
        List<PaymentRequest> matchedRequests = paymentRequestRepository
                .findByClubIdAndStatus(clubId, PaymentRequest.RequestStatus.MATCHED);

        Map<Long, PaymentRequest> requestMap = matchedRequests.stream()
                .filter(r -> r.getMatchedHistoryId() != null)
                .collect(Collectors.toMap(PaymentRequest::getMatchedHistoryId, r -> r));

        // 3. ProcessedTransactionResponse 생성
        List<ProcessedTransactionResponse> processed = new ArrayList<>();
        for (BankTransactionHistory history : histories) {
            PaymentRequest matchedRequest = requestMap.get(history.getHistoryId());

            if (matchedRequest != null) {
                // 매칭된 경우
                if (matchedRequest.getMatchType() == PaymentRequest.MatchType.AUTO_MATCHED) {
                    processed.add(ProcessedTransactionResponse.autoMatched(
                            history.getHistoryId(),
                            history.getUniqueTxKey(),
                            history.getBankTransactionAt(),
                            extractType(history),
                            history.getAmount(),
                            java.math.BigDecimal.ZERO, // balance_after
                            history.getPrintContent(),
                            matchedRequest));
                } else {
                    processed.add(ProcessedTransactionResponse.confirmed(
                            history.getHistoryId(),
                            history.getUniqueTxKey(),
                            history.getBankTransactionAt(),
                            extractType(history),
                            history.getAmount(),
                            java.math.BigDecimal.ZERO,
                            history.getPrintContent(),
                            matchedRequest));
                }
            } else {
                // 매칭되지 않은 경우
                processed.add(ProcessedTransactionResponse.unmatched(
                        history.getHistoryId(),
                        history.getUniqueTxKey(),
                        history.getBankTransactionAt(),
                        extractType(history),
                        history.getAmount(),
                        java.math.BigDecimal.ZERO,
                        history.getPrintContent()));
            }
        }

        return ResponseEntity.ok(processed);
    }

    /**
     * 미매칭 거래내역 조회
     * GET /clubs/{clubId}/bank/transactions/unmatched
     */
    @GetMapping("/transactions/unmatched")
    public ResponseEntity<UnmatchedTransactionsResponse> getUnmatchedTransactions(
            @PathVariable Long clubId) {
        // 미매칭 거래내역 조회 (최근 30일)
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(30);

        List<BankTransactionHistory> histories = transactionHistoryRepository
                .findByClubIdAndBankTransactionAtBetween(
                        clubId,
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay());

        // 매칭된 history_id 수집
        List<PaymentRequest> matchedRequests = paymentRequestRepository
                .findByClubIdAndStatus(clubId, PaymentRequest.RequestStatus.MATCHED);

        List<Long> matchedHistoryIds = matchedRequests.stream()
                .map(PaymentRequest::getMatchedHistoryId)
                .filter(Objects::nonNull)
                .toList();

        // 미매칭 거래내역 필터링
        List<BankTransactionHistory> unmatched = histories.stream()
                .filter(h -> !matchedHistoryIds.contains(h.getHistoryId()))
                .collect(Collectors.toList());

        // 매칭 가능한 입금요청 조회
        List<PaymentRequest> availableRequests = paymentRequestRepository.findMatchableRequests(clubId);

        return ResponseEntity.ok(new UnmatchedTransactionsResponse(unmatched, availableRequests));
    }

    /**
     * 모임 정산 환급
     * - 모임장/총무가 남은 돈을 회원들에게 돌려주기
     * - 오픈뱅킹 API 출금/이체 호출
     * <p>
     * POST /clubs/{clubId}/bank/refund
     */
    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> refundToMember(
            @PathVariable Long clubId,
            @RequestBody RefundRequest request) {
        // clubId 검증
        if (!clubId.equals(request.clubId())) {
            return ResponseEntity.badRequest().build();
        }

        RefundResponse response = bankService.refundToMember(request);
        return ResponseEntity.ok(response);
    }

    // Helper 메서드

    private String extractType(BankTransactionHistory history) {
        return history.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0 ? "DEPOSIT" : "WITHDRAW";
    }

    // 응답 DTO

    public record UnmatchedTransactionsResponse(
            List<BankTransactionHistory> unmatchedTransactions,
            List<PaymentRequest> availableRequests) {
    }
}
