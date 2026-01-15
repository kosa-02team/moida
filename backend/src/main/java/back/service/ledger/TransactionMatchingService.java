package back.service.ledger;

import back.bank.domain.BankTransactionHistory;
import back.domain.ledger.PaymentRequest;
import back.bank.repository.BankTransactionHistoryRepository;
import back.repository.ledger.PaymentRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 거래내역 매칭 서비스
 * - 입금요청과 거래내역을 자동으로 매칭
 */
@Service
public class TransactionMatchingService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final BankTransactionHistoryRepository transactionHistoryRepository;

    public TransactionMatchingService(PaymentRequestRepository paymentRequestRepository,
            BankTransactionHistoryRepository transactionHistoryRepository) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
    }

    /**
     * 자동 매칭 수행
     * - 새로운 거래내역이 들어올 때 호출
     */
    @Transactional
    public void autoMatchTransactions(Long clubId, List<BankTransactionHistory> newTransactions) {
        // 매칭 가능한 입금요청 조회 (PENDING 상태 + 만료되지 않음)
        List<PaymentRequest> matchableRequests = paymentRequestRepository.findMatchableRequests(clubId);

        for (BankTransactionHistory transaction : newTransactions) {
            // DEPOSIT만 매칭 대상
            if (!"DEPOSIT".equalsIgnoreCase(extractTransactionType(transaction))) {
                continue;
            }

            // 이미 매칭된 거래는 스킵
            if (isAlreadyMatched(transaction, matchableRequests)) {
                continue;
            }

            // 매칭 시도
            tryMatch(transaction, matchableRequests);
        }
    }

    /**
     * 거래내역과 입금요청 매칭 시도
     */
    private void tryMatch(BankTransactionHistory transaction, List<PaymentRequest> requests) {
        for (PaymentRequest request : requests) {
            if (!request.isMatchable()) {
                continue;
            }

            if (isMatched(transaction, request)) {
                // 자동 매칭 처리
                request.autoMatch(transaction.getHistoryId());
                paymentRequestRepository.save(request);
                return; // 하나의 거래내역은 하나의 요청에만 매칭
            }
        }
    }

    /**
     * 매칭 조건 확인
     * 1. print_content에 회원 이름 포함
     * 2. 금액이 예상 금액과 일치
     * 3. 거래 날짜가 예상 날짜 ±N일 이내 (N=match_days_range)
     */
    private boolean isMatched(BankTransactionHistory transaction, PaymentRequest request) {
        // 조건 1: 이름 확인 (대소문자 구분 없이)
        String senderName = transaction.getSenderName();
        if (senderName == null || !senderName.contains(request.getMemberName())) {
            return false;
        }

        // 조건 2: 금액 확인
        BigDecimal transactionAmount = transaction.getAmount();
        if (transactionAmount.compareTo(request.getExpectedAmount()) != 0) {
            return false;
        }

        // 조건 3: 날짜 범위 확인
        LocalDate transactionDate = transaction.getBankTransactionAt().toLocalDate();
        LocalDate expectedDate = request.getExpectedDate();
        int daysRange = request.getMatchDaysRange() != null ? request.getMatchDaysRange() : 10;

        LocalDate fromDate = expectedDate.minusDays(daysRange);
        LocalDate toDate = expectedDate.plusDays(daysRange);

        return !transactionDate.isBefore(fromDate) && !transactionDate.isAfter(toDate);
    }

    /**
     * 이미 매칭된 거래인지 확인
     */
    private boolean isAlreadyMatched(BankTransactionHistory transaction, List<PaymentRequest> requests) {
        Long historyId = transaction.getHistoryId();
        return requests.stream()
                .anyMatch(req -> historyId.equals(req.getMatchedHistoryId()));
    }

    /**
     * 거래 타입 추출 (임시 - BankTransactionHistory에 type 필드가 없는 경우)
     */
    private String extractTransactionType(BankTransactionHistory transaction) {
        // TODO: BankTransactionHistory에 type 필드가 있으면 그걸 사용
        // 현재는 금액이 양수면 DEPOSIT으로 가정
        return transaction.getAmount().compareTo(BigDecimal.ZERO) > 0 ? "DEPOSIT" : "WITHDRAW";
    }

    /**
     * 수동 매칭 처리
     */
    @Transactional
    public void manualMatch(Long requestId, Long historyId, Long matchedBy) {
        PaymentRequest request = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("입금요청을 찾을 수 없습니다. requestId: " + requestId));

        if (!request.isMatchable()) {
            throw new IllegalStateException("이미 매칭되었거나 만료된 요청입니다.");
        }

        // 수동 매칭 처리
        request.confirmMatch(historyId, matchedBy);
        paymentRequestRepository.save(request);
    }

    /**
     * 만료된 입금요청 처리
     */
    @Transactional
    public void expireOldRequests(Long clubId) {
        List<PaymentRequest> pendingRequests = paymentRequestRepository.findByClubIdAndStatus(
                clubId,
                PaymentRequest.RequestStatus.PENDING);

        LocalDateTime now = LocalDateTime.now();
        for (PaymentRequest request : pendingRequests) {
            if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(now)) {
                request.expire();
                paymentRequestRepository.save(request);
            }
        }
    }
}
