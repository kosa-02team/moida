package back.service.ledger;

import back.bank.domain.BankTransactionHistory;
import back.domain.ClubMembers;
import back.domain.ledger.PaymentRequest;
import back.bank.repository.BankTransactionHistoryRepository;
import back.repository.ClubMemberRepository;
import back.repository.clubs.ClubMembersRepository;
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
    private final ClubMemberRepository clubMemberRepository;

    public TransactionMatchingService(PaymentRequestRepository paymentRequestRepository,
            BankTransactionHistoryRepository transactionHistoryRepository,
          ClubMemberRepository clubMemberRepository) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.clubMemberRepository = clubMemberRepository;
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
     * 1. 금액이 예상 금액과 일치 
     * 2. 거래 날짜가 예상 날짜 ±N일 이내 (N=match_days_range)툴바 사용자 지정…
     * 3. print_content에 회원 이름, 닉네임 포함
     */
    private boolean isMatched(BankTransactionHistory tx, PaymentRequest req) {

        // 1) 금액 먼저
        if (tx.getAmount().compareTo(req.getExpectedAmount()) != 0) return false;

        // 2) 날짜 범위
        LocalDate txDate = tx.getBankTransactionAt().toLocalDate();
        LocalDate expected = req.getExpectedDate();
        int range = req.getMatchDaysRange() != null ? req.getMatchDaysRange() : 10;

        if (txDate.isBefore(expected.minusDays(range)) || txDate.isAfter(expected.plusDays(range))) return false;

        // 3) 적요
        String content = normalize(tx.getPrintContent());
        if (content.isBlank()) return false;

        // 4) memberId로 실명/닉네임 조회 (방법 A면 member 가져와서 member.realName() 써도 됨)
        var viewOpt = clubMemberRepository.findNameView(req.getClubId(), req.getMemberId());
        if (viewOpt.isEmpty()) return false;

        String realNameRaw = viewOpt.get().getRealName();
        String nickRaw = viewOpt.get().getClubNickname();

        String realName = normalize(realNameRaw);
        String nick = normalize(nickRaw);

        // 5) 실명/닉네임이 클럽 내 유일할 때만 매칭 허용
        boolean realNameUnique = !realName.isBlank()
                && clubMemberRepository.countByClubIdAndRealName(req.getClubId(), realNameRaw) == 1;

        boolean nickUnique = !nick.isBlank()
                && clubMemberRepository.countByClubIdAndClubNickname(req.getClubId(), nickRaw) == 1;

        // 6) 유일한 경우에만 contains 허용
        if (realNameUnique && content.contains(realName)) return true;
        if (nickUnique && content.contains(nick)) return true;

        return false;
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

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", "")
                .replaceAll("[^0-9a-zA-Z가-힣]", "")
                .toLowerCase();
    }

}
