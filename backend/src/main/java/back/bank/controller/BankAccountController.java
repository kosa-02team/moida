package back.bank.controller;

import back.bank.domain.BankAccounts;
import back.bank.domain.BankTransactionHistory;
import back.domain.ledger.PaymentRequest;
import back.bank.dto.request.AccountCreateRequest;
import back.bank.repository.BankAccountRepository;
import back.dto.ledger.request.RefundRequest;
import back.dto.ledger.response.ProcessedTransactionResponse;
import back.dto.ledger.response.RefundResponse;
import back.bank.repository.BankTransactionHistoryRepository;
import back.repository.ledger.PaymentRequestRepository;
import back.repository.ledger.TransactionLogRepository;
import back.repository.club.ClubMemberRepository;
import back.bank.dto.response.BankAccountResponseDTO;
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
    private final BankAccountRepository bankAccountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final ClubMemberRepository clubMemberRepository;

    public BankAccountController(BankService bankService,
            BankTransactionHistoryRepository transactionHistoryRepository,
            PaymentRequestRepository paymentRequestRepository,
            BankAccountRepository bankAccountRepository,
            TransactionLogRepository transactionLogRepository,
            ClubMemberRepository clubMemberRepository) {
        this.bankService = bankService;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.paymentRequestRepository = paymentRequestRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.clubMemberRepository = clubMemberRepository;
    }

    /**
     * 모임 가상계좌 조회
     * GET /clubs/{clubId}/bank/account
     */
    @GetMapping("/account")
    public ResponseEntity<BankAccountResponseDTO> getAccount(@PathVariable Long clubId) {
        BankAccounts account = bankAccountRepository.findByClubId(clubId)
                .orElseThrow(() -> new IllegalArgumentException("모임 계좌를 찾을 수 없습니다."));
        return ResponseEntity.ok(BankAccountResponseDTO.from(account));
    }

    /**
     * 모임 가상계좌 생성
     * POST /clubs/{clubId}/bank/accounts
     */
    @PostMapping("/accounts")
    public ResponseEntity<BankAccountResponseDTO> createAccount(
            @PathVariable Long clubId,
            @RequestBody AccountCreateRequest request) {
        BankAccounts account = bankService.createAccount(clubId, request);
        return ResponseEntity.ok(BankAccountResponseDTO.from(account));
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
     * 일정별 거래내역 조회 (TransactionLog 기준)
     * GET /clubs/{clubId}/schedules/{scheduleId}/transactions
     */
    @GetMapping("/schedules/{scheduleId}/transactions")
    public ResponseEntity<ScheduleTransactionResponse> getScheduleTransactions(
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        // 조회 전 은행 동기화 (최신 거래내역 반영)
        try {
            bankService.syncTransactionsStub(clubId, 1L, null, null); // 입금 내역
            bankService.syncTransactionsStub(clubId, 2L, null, null); // 출금 내역
        } catch (Exception e) {
            // 동기화 실패 시 로깅만 하고 계속 진행
            System.err.println("Bank sync failed during transaction query: " + e.getMessage());
        }
        
        // 일정과 연결된 TransactionLog 조회
        List<TransactionLog> scheduleLogs = transactionLogRepository.findByScheduleId(scheduleId);
        
        // 일정의 모든 PaymentRequest 조회
        List<PaymentRequest> requests = paymentRequestRepository.findByScheduleId(scheduleId);
        
        // 매칭 여부 확인을 위한 Map
        Map<Long, PaymentRequest> historyIdToRequest = requests.stream()
                .filter(r -> r.getMatchedHistoryId() != null)
                .collect(Collectors.toMap(PaymentRequest::getMatchedHistoryId, r -> r));
        
        // TransactionLog를 상세 정보로 변환
        List<TransactionDetail> details = new ArrayList<>();
        for (TransactionLog log : scheduleLogs) {
            BankTransactionHistory history = null;
            PaymentRequest matchedRequest = null;
            
            if (log.getBankHistoryId() != null) {
                history = transactionHistoryRepository.findById(log.getBankHistoryId()).orElse(null);
                if (history != null) {
                    matchedRequest = historyIdToRequest.get(history.getHistoryId());
                }
            }
            
            details.add(new TransactionDetail(
                    log.getTransactionId(),
                    log.getType(),
                    log.getAmount(),
                    log.getBalanceAfter(),
                    log.getDescription(),
                    log.getCreatedAt(),
                    history != null ? history.getHistoryId() : null,
                    history != null ? history.getPrintContent() : null,
                    matchedRequest != null ? matchedRequest.getMemberName() : null,
                    matchedRequest != null ? matchedRequest.getStatus().name() : null
            ));
        }
        
        return ResponseEntity.ok(new ScheduleTransactionResponse(scheduleId, details));
    }

    /**
     * 미매칭 거래내역 조회
     * GET /clubs/{clubId}/bank/transactions/unmatched
     */
    @GetMapping("/transactions/unmatched")
    public ResponseEntity<UnmatchedTransactionsResponse> getUnmatchedTransactions(
            @PathVariable Long clubId,
            @RequestParam(required = false, defaultValue = "true") boolean recalculate) {
        // 조회 전 은행 동기화 (최신 거래내역 반영)
        try {
            bankService.syncTransactionsStub(clubId, 1L, null, null); // 입금 내역
            bankService.syncTransactionsStub(clubId, 2L, null, null); // 출금 내역
        } catch (Exception e) {
            // 동기화 실패 시 로깅만 하고 계속 진행
            System.err.println("Bank sync failed during unmatched query: " + e.getMessage());
        }
        
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

        // 미매칭 거래내역 필터링 - isMatched 필드로 직접 확인
        List<BankTransactionHistory> unmatched = histories.stream()
                .filter(h -> !Boolean.TRUE.equals(h.getIsMatched()))
                .collect(Collectors.toList());

        // 매칭 가능한 입금요청 조회
        List<PaymentRequest> availableRequests = paymentRequestRepository.findMatchableRequests(clubId);
        
        // PaymentRequest를 DTO로 변환 (닉네임 포함)
        List<PaymentRequestWithNickname> requestsWithNickname = availableRequests.stream()
                .map(req -> {
                    String displayName = req.getMemberName();
                    var memberView = clubMemberRepository.findNameView(clubId, req.getMemberId());
                    if (memberView.isPresent()) {
                        String nickname = memberView.get().getClubNickname();
                        if (nickname != null && !nickname.isBlank()) {
                            displayName = req.getMemberName() + " (" + nickname + ")";
                        }
                    }
                    return new PaymentRequestWithNickname(
                            req.getRequestId(),
                            req.getClubId(),
                            req.getMemberId(),
                            displayName, // 닉네임 포함된 이름
                            req.getRequestType().name(),
                            req.getExpectedAmount(),
                            req.getExpectedDate().toString(),
                            req.getStatus().name(),
                            req.getMatchedHistoryId(),
                            req.getScheduleId()
                    );
                })
                .collect(Collectors.toList());
        
        // 미매칭 거래의 실패 사유 재계산 및 업데이트 (recalculate=true인 경우만)
        if (recalculate) {
            System.out.println("🔄 미매칭 사유 재계산 시작...");
            for (BankTransactionHistory tx : unmatched) {
                String newReason = determineFailureReason(tx, availableRequests);
                if (!newReason.equals(tx.getUnmatchReason())) {
                    System.out.println("  → " + tx.getPrintContent() + ": " + tx.getUnmatchReason() + " → " + newReason);
                    tx.updateUnmatchReason(newReason);
                    transactionHistoryRepository.save(tx);
                }
            }
        }

        return ResponseEntity.ok(new UnmatchedTransactionsResponse(unmatched, requestsWithNickname));
    }
    
    /**
     * 매칭 실패 사유 결정 (개선된 버전 - 디버깅 강화)
     */
    private String determineFailureReason(BankTransactionHistory tx, List<PaymentRequest> requests) {
        if (requests.isEmpty())
            return "NO_PENDING_REQUESTS";

        boolean anyPotentialMatch = false;
        boolean amountMatchFound = false;
        boolean dateMatchFound = false;

        System.out.println("  🔍 " + tx.getPrintContent() + " (" + tx.getAmount() + "원) 분석 중...");

        for (PaymentRequest req : requests) {
            // 금액 확인
            boolean amountMatch = tx.getAmount().abs().compareTo(req.getExpectedAmount().abs()) == 0;
            if (amountMatch) {
                amountMatchFound = true;
                System.out.println("    ✓ 금액 일치: " + req.getMemberName() + " request와 매칭 가능");
            }

            // 날짜 확인
            LocalDate txDate = tx.getBankTransactionAt().toLocalDate();
            LocalDate expected = req.getExpectedDate();
            int range = req.getMatchDaysRange() != null ? req.getMatchDaysRange() : 10;
            boolean dateMatch = !txDate.isBefore(expected.minusDays(range))
                    && !txDate.isAfter(expected.plusDays(range));
            if (dateMatch) {
                dateMatchFound = true;
                System.out.println("    ✓ 날짜 일치");
            }

            // 금액과 날짜가 모두 일치하는 경우, 이름도 확인
            if (amountMatch && dateMatch) {
                String content = normalize(tx.getPrintContent());
                System.out.println("    정규화된 거래 내용: [" + content + "]");
                
                if (!content.isBlank()) {
                    System.out.println("    멤버 정보 조회 중: clubId=" + req.getClubId() + ", memberId=" + req.getMemberId());
                    var viewOpt = clubMemberRepository.findNameView(req.getClubId(), req.getMemberId());
                    
                    if (viewOpt.isPresent()) {
                        String realNameRaw = viewOpt.get().getRealName();
                        String nickRaw = viewOpt.get().getClubNickname();
                        
                        String realName = normalize(realNameRaw);
                        String nick = normalize(nickRaw);
                        
                        System.out.println("    정규화된 실명: [" + realName + "], 닉네임: [" + nick + "]");
                        
                        // 거래 내역에 해당 멤버의 이름이 포함되어 있는지 확인
                        boolean nameIncluded = (!realName.isBlank() && content.contains(realName))
                                || (!nick.isBlank() && content.contains(nick));
                        
                        if (nameIncluded) {
                            System.out.println("    ✓ 이름 포함 확인! → 잠재적 매칭 후보");
                            anyPotentialMatch = true;
                        } else {
                            System.out.println("    ✗ 이름 불일치 (거래: '" + content + "', 실명: '" + realName + "', 닉: '" + nick + "')");
                        }
                    } else {
                        System.out.println("    ⚠️ 멤버 정보를 찾을 수 없음!");
                    }
                } else {
                    System.out.println("    ⚠️ 거래 내용이 비어있음");
                }
            }
        }

        String result;
        if (anyPotentialMatch) {
            result = "NAME_MISMATCH_OR_DUPLICATE";
        } else if (!amountMatchFound) {
            result = "AMOUNT_MISMATCH";
        } else if (!dateMatchFound) {
            result = "DATE_OUT_OF_RANGE";
        } else {
            result = "NO_PENDING_REQUESTS";
        }
        
        System.out.println("    → 최종 사유: " + result);
        return result;
    }

    private String normalize(String s) {
        if (s == null)
            return "";
        return s.replaceAll("\\s+", "")
                .replaceAll("[^0-9a-zA-Z가-힣]", "")
                .toLowerCase();
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
        return history.getInoutType();
    }

    // 응답 DTO

    public record UnmatchedTransactionsResponse(
            List<BankTransactionHistory> unmatchedTransactions,
            List<PaymentRequestWithNickname> availableRequests) {
    }

    public record PaymentRequestWithNickname(
            Long requestId,
            Long clubId,
            Long memberId,
            String memberName, // 닉네임 포함된 표시 이름
            String requestType,
            java.math.BigDecimal expectedAmount,
            String expectedDate,
            String status,
            Long matchedHistoryId,
            Long scheduleId) {
    }

    public record ScheduleTransactionResponse(
            Long scheduleId,
            List<TransactionDetail> transactions) {
    }

    public record TransactionDetail(
            Long transactionId,
            String type,
            java.math.BigDecimal amount,
            java.math.BigDecimal balanceAfter,
            String description,
            java.time.LocalDateTime createdAt,
            Long bankHistoryId,
            String printContent,
            String matchedMemberName,
            String matchStatus) {
    }
}
