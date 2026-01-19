package back.service.ledger;

import back.bank.domain.BankTransactionHistory;
import back.domain.club.Clubs;
import back.domain.ledger.PaymentRequest;
import back.bank.repository.BankTransactionHistoryRepository;
import back.domain.ledger.TransactionLog;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import back.repository.ledger.PaymentRequestRepository;
import back.repository.ledger.TransactionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 거래내역 매칭 서비스
 * - 입금요청과 거래내역을 자동으로 매칭
 */
@Service
public class TransactionMatchingService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final BankTransactionHistoryRepository transactionHistoryRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;
    private final TransactionLogRepository transactionLogRepository;

    public TransactionMatchingService(PaymentRequestRepository paymentRequestRepository,
            BankTransactionHistoryRepository transactionHistoryRepository,
            ClubMemberRepository clubMemberRepository,
            ClubRepository clubRepository,
            TransactionLogRepository transactionLogRepository) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.clubRepository = clubRepository;
        this.transactionLogRepository = transactionLogRepository;
    }

    /**
     * 자동 매칭 수행
     * - 새로운 거래내역이 들어올 때 호출
     */
    @Transactional
    public void autoMatchTransactions(Long clubId, List<BankTransactionHistory> newTransactions,
            Map<Long, TransactionLog> newTransactionLogs) {
        // 매칭 가능한 입금요청 조회 (PENDING 상태 + 만료되지 않음)
        List<PaymentRequest> matchableRequests = paymentRequestRepository.findMatchableRequests(clubId);

        // 클럽 정보 조회 (운영 타입 확인용)
        Clubs club = clubRepository.findById(clubId).orElse(null);
        boolean isFairSettlement = club != null && club.getType() == Clubs.Type.FAIR_SETTLEMENT;

        for (BankTransactionHistory transaction : newTransactions) {

            // 이미 매칭된 거래는 스킵
            if (isAlreadyMatched(transaction, matchableRequests)) {
                continue;
            }

            // 매칭 시도
            tryMatch(transaction, matchableRequests, isFairSettlement, newTransactionLogs);
        }
    }

    /**
     * 거래내역과 입금요청 매칭 시도
     */
    private void tryMatch(BankTransactionHistory transaction, List<PaymentRequest> requests, boolean isFairSettlement,
            Map<Long, TransactionLog> newTransactionLogs) {
        for (PaymentRequest request : requests) {
            if (!request.isMatchable()) {
                continue;
            }

            if (isMatched(transaction, request)) {
                // 자동 매칭 처리
                request.autoMatch(transaction.getHistoryId());
                paymentRequestRepository.save(request);

                // FAIR_SETTLEMENT 타입인 경우 TransactionLog에 scheduleId 저장
                if (isFairSettlement && newTransactionLogs != null
                        && newTransactionLogs.containsKey(transaction.getHistoryId())) {
                    TransactionLog log = newTransactionLogs.get(transaction.getHistoryId());
                    if (log != null && request.getScheduleId() != null) {
                        log.updateScheduleId(request.getScheduleId());
                        transactionLogRepository.save(log); // 변경사항 저장
                    }
                }

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
        System.out.println("은행거래내역 금액 : " + tx.getAmount() + ", 지불 요청 금액 : " + req.getExpectedAmount());

        // 1) 금액 먼저 (절대값 비교)
        if (tx.getAmount().abs().compareTo(req.getExpectedAmount().abs()) != 0)
            return false;

        System.out.println("타입매칭");

        // 1-1) 타입 매칭 확인
        String txType = extractTransactionType(tx);

        if ("DEPOSIT".equalsIgnoreCase(txType)) {
            // 입금 트랜잭션은 DEPOSIT, MEMBERSHIP_FEE 등과 매칭
            if (req.getRequestType() == PaymentRequest.RequestType.SETTLEMENT)
                return false;
        } else if ("WITHDRAW".equalsIgnoreCase(txType)) {
            // 출금 트랜잭션은 SETTLEMENT와 매칭
            if (req.getRequestType() != PaymentRequest.RequestType.SETTLEMENT)
                return false;
        }

        System.out.println("날짜 범위");

        // 2) 날짜 범위
        LocalDate txDate = tx.getBankTransactionAt().toLocalDate();
        LocalDate expected = req.getExpectedDate();
        int range = req.getMatchDaysRange() != null ? req.getMatchDaysRange() : 10;

        if (txDate.isBefore(expected.minusDays(range)) || txDate.isAfter(expected.plusDays(range))) {
            System.out.println("입금 날짜: txDate : " + txDate + " 가 " + expected.minusDays(range) + "와" +
                    expected.plusDays(range) + "사이?");
            return false;
        }

        System.out.println("적요");

        // 3) 적요
        String content = normalize(tx.getPrintContent());
        if (content.isBlank())
            return false;

        // 4) memberId로 실명/닉네임 조회 (방법 A면 member 가져와서 member.realName() 써도 됨)
        System.out.println("지불 요청 클럽, 멤버 아이디 : " + req.getClubId() + " , " + req.getMemberId());

        var viewOpt = clubMemberRepository.findNameView(req.getClubId(), req.getMemberId());
        if (viewOpt.isEmpty()) {
            System.out.println("사람을 찾을 수 없습니다");
            return false;
        }

        String realNameRaw = viewOpt.get().getRealName();
        String nickRaw = viewOpt.get().getClubNickname();

        String realName = normalize(realNameRaw);
        String nick = normalize(nickRaw);

        System.out.println("멤버 실명 : " + realName);

        // 5) 실명/닉네임이 클럽 내 유일할 때만 매칭 허용
        boolean realNameUnique = !realName.isBlank()
                && clubMemberRepository.countByClubIdAndRealName(req.getClubId(), realNameRaw) == 1;

        boolean nickUnique = !nick.isBlank()
                && clubMemberRepository.countByClubIdAndClubNickname(req.getClubId(), nickRaw) == 1;

        // 6) 유일한 경우에만 contains 허용
        if (realNameUnique && content.contains(realName))
            return true;
        if (nickUnique && content.contains(nick))
            return true;

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
        // 현재는 금액이 양수면 DEPOSIT, 음수면 WITHDRAW로 가정
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
        if (s == null)
            return "";
        return s.replaceAll("\\s+", "")
                .replaceAll("[^0-9a-zA-Z가-힣]", "")
                .toLowerCase();
    }

}
