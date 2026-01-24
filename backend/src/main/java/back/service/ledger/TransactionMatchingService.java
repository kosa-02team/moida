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
import back.repository.ledger.AuditLogsRepository;
import back.repository.schedule.ScheduleParticipantRepository;
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
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final AuditLogsRepository auditLogsRepository;

    public TransactionMatchingService(PaymentRequestRepository paymentRequestRepository,
            BankTransactionHistoryRepository transactionHistoryRepository,
            ClubMemberRepository clubMemberRepository,
            ClubRepository clubRepository,
            TransactionLogRepository transactionLogRepository,
            ScheduleParticipantRepository scheduleParticipantRepository,
            AuditLogsRepository auditLogsRepository) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.clubRepository = clubRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.scheduleParticipantRepository = scheduleParticipantRepository;
        this.auditLogsRepository = auditLogsRepository;
    }

    /**
     * 자동 매칭 수행
     * - 새로운 거래내역이 들어올 때 호출
     */
    @Transactional
    public void autoMatchTransactions(Long clubId, List<BankTransactionHistory> newTransactions,
            Map<Long, TransactionLog> newTransactionLogs) {
        synchronized (clubId.toString().intern()) {
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

                // 거래내역 매칭 상태 업데이트
                transaction.markAsMatched();
                transactionHistoryRepository.save(transaction);

                // 일정 참가자 상태 업데이트
                updateScheduleParticipantStatus(request, transaction.getHistoryId());

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

        // 매칭 실패 시 사유 추적 (가장 근접한 실패 사유 기록)
        String failureReason = determineFailureReason(transaction, requests);
        transaction.updateUnmatchReason(failureReason);
        transactionHistoryRepository.save(transaction);
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

        if (nickUnique && content.contains(nick))
            return true;

        return false;
    }

    private String determineFailureReason(BankTransactionHistory tx, List<PaymentRequest> requests) {
        if (requests.isEmpty())
            return "NO_PENDING_REQUESTS";

        boolean amountMatchFound = false;
        boolean dateMatchFound = false;

        for (PaymentRequest req : requests) {
            boolean amountMatch = tx.getAmount().abs().compareTo(req.getExpectedAmount().abs()) == 0;
            if (amountMatch)
                amountMatchFound = true;

            LocalDate txDate = tx.getBankTransactionAt().toLocalDate();
            LocalDate expected = req.getExpectedDate();
            int range = req.getMatchDaysRange() != null ? req.getMatchDaysRange() : 10;
            boolean dateMatch = !txDate.isBefore(expected.minusDays(range))
                    && !txDate.isAfter(expected.plusDays(range));
            if (dateMatch)
                dateMatchFound = true;

            if (amountMatch && dateMatch) {
                // 이름 문제일 가능성 높음
                return "NAME_MISMATCH_OR_DUPLICATE";
            }
        }

        if (!amountMatchFound)
            return "AMOUNT_MISMATCH";
        if (!dateMatchFound)
            return "DATE_OUT_OF_RANGE";

        return "UNKNOWN_MISMATCH";
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
     * 거래 타입 추출 (BankTransactionHistory의 inoutType 사용)
     */
    private String extractTransactionType(BankTransactionHistory transaction) {
        return transaction.getInoutType();
    }

    /**
     * 수동 매칭 처리
     */
    @Transactional
    public void manualMatch(Long requestId, Long historyId, Long matchedBy) {
        PaymentRequest request = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("입금요청을 찾을 수 없습니다. requestId: " + requestId));

        if (!request.isMatchable()) {
            throw new IllegalStateException("이미 매칭되었거나 만료된 요청입니다."); // TODO: isMatchable이 EXPIRED를 허용했으므로 문구 수정 필요할 수도
                                                                     // 있음
        }

        // 수동 매칭 처리
        request.confirmMatch(historyId, matchedBy);
        paymentRequestRepository.save(request);

        // 거래내역 매칭 상태 업데이트
        if (historyId != null) {
            transactionHistoryRepository.findById(historyId).ifPresent(transaction -> {
                transaction.markAsMatched();
                transaction.updateUnmatchReason(null); // 매칭되었으므로 사유 제거
                transactionHistoryRepository.save(transaction);
            });
        }

        // 일정 참가자 상태 업데이트
        updateScheduleParticipantStatus(request, historyId);

        // 감사 로그 기록
        auditLogsRepository.save(new back.domain.ledger.AuditLogs(
                historyId != null ? historyId : -1L,
                matchedBy,
                "PENDING",
                "MATCHED (Manual)"));
    }

    /**
     * 거래내역 없이 수동 확인 (현금 수령 등)
     */
    @Transactional
    public void confirmPaymentWithoutHistory(Long requestId, Long matchedBy) {
        PaymentRequest request = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("입금요청을 찾을 수 없습니다. requestId: " + requestId));

        if (!request.isMatchable()) {
            throw new IllegalStateException("이미 매칭되어 처리할 수 없습니다.");
        }

        // 수동 확인 처리 (현금)
        request.confirmManualCashPayment(matchedBy);
        paymentRequestRepository.save(request);

        // 일정 참가자 상태 업데이트 (historyId는 null)
        updateScheduleParticipantStatus(request, null);

        // 감사 로그 기록
        auditLogsRepository.save(new back.domain.ledger.AuditLogs(
                -1L,
                matchedBy,
                "PENDING",
                "MATCHED (Manual Cash)"));
    }

    /**
     * 매칭 취소 처리
     */
    @Transactional
    public void cancelMatch(Long requestId, Long adminId) {
        PaymentRequest request = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("입금요청을 찾을 수 없습니다. requestId: " + requestId));

        if (request.getStatus() != PaymentRequest.RequestStatus.MATCHED) {
            throw new IllegalStateException("매칭된 요청만 취소할 수 있습니다.");
        }

        Long historyId = request.getMatchedHistoryId();

        // 1. 입금요청 원복
        request.unmatch();
        paymentRequestRepository.save(request);

        // 2. 거래내역 원복
        if (historyId != null) {
            transactionHistoryRepository.findById(historyId).ifPresent(history -> {
                history.unmarkAsMatched();
                transactionHistoryRepository.save(history);
            });
        }

        // 3. 일정 참가자 상태 원복
        if (request.getScheduleId() != null) {
            clubMemberRepository.findById(request.getMemberId()).ifPresent(member -> {
                scheduleParticipantRepository.findByScheduleIdAndUserId(request.getScheduleId(), member.getUserId())
                        .ifPresent(participant -> {
                            participant.resetPayment();
                            scheduleParticipantRepository.save(participant);
                        });
            });
        }

        // 감사 로그 기록
        auditLogsRepository.save(new back.domain.ledger.AuditLogs(
                historyId != null ? historyId : -1L,
                adminId,
                "MATCHED",
                "CANCELLED (Unmatched)"));
    }

    /**
     * 다중 매칭 처리 (하나의 거래내역에 여러 요청 매칭)
     */
    @Transactional
    public void manualMatchMultipleRequests(List<Long> requestIds, Long historyId, Long adminId) {
        BankTransactionHistory transaction = transactionHistoryRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("거래내역을 찾을 수 없습니다."));

        List<PaymentRequest> requests = paymentRequestRepository.findAllById(requestIds);
        if (requests.size() != requestIds.size()) {
            throw new IllegalArgumentException("일부 입금요청을 찾을 수 없습니다.");
        }

        // 금액 검증
        BigDecimal totalExpected = requests.stream()
                .map(PaymentRequest::getExpectedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (transaction.getAmount().abs().compareTo(totalExpected.abs()) != 0) {
            throw new IllegalStateException(
                    "선택한 요청들의 합계 금액(" + totalExpected + ")이 거래 금액(" + transaction.getAmount().abs() + ")과 일치하지 않습니다.");
        }

        for (PaymentRequest request : requests) {
            if (!request.isMatchable()) {
                throw new IllegalStateException("이미 매칭되었거나 만료된 요청이 포함되어 있습니다. ID: " + request.getRequestId());
            }

            request.confirmMatch(historyId, adminId);
            paymentRequestRepository.save(request);
            updateScheduleParticipantStatus(request, historyId);
        }

        // 거래내역 상태 업데이트
        transaction.markAsMatched();
        transaction.updateUnmatchReason(null);
        transactionHistoryRepository.save(transaction);

        // 감사 로그 기록
        auditLogsRepository.save(new back.domain.ledger.AuditLogs(
                historyId,
                adminId,
                "PENDING",
                "MULTI_MATCHED (" + requests.size() + " requests)"));
    }

    /**
     * 새로운 입금 요청들을 기존의 미매칭 거래내역과 매칭 시도
     */
    @Transactional
    public void matchRequestsWithExistingTransactions(Long clubId, List<PaymentRequest> newRequests) {
        synchronized (clubId.toString().intern()) {
            // 1. 미매칭 거래내역 조회 (오래된 순)
            List<BankTransactionHistory> unmatchedHistories = transactionHistoryRepository
                    .findByClubIdAndIsMatchedFalse(clubId);
            unmatchedHistories.sort((h1, h2) -> h1.getBankTransactionAt().compareTo(h2.getBankTransactionAt()));

            if (unmatchedHistories.isEmpty()) {
                return;
            }

            // 2. 새로운 요청들도 오래된 날짜 순으로 정렬
            newRequests.sort((r1, r2) -> r1.getExpectedDate().compareTo(r2.getExpectedDate()));

            // 3. 각 요청에 대해 매칭 시도
            for (PaymentRequest request : newRequests) {
                if (!request.isMatchable()) {
                    continue;
                }

                for (BankTransactionHistory transaction : unmatchedHistories) {
                    // 이미 다른 요청에 매칭되었을 수 있으므로 체크
                    if (transaction.getIsMatched()) {
                        continue;
                    }

                    if (isMatched(transaction, request)) {
                        request.autoMatch(transaction.getHistoryId());
                        paymentRequestRepository.save(request);

                        transaction.markAsMatched();
                        transaction.updateUnmatchReason(null);
                        transactionHistoryRepository.save(transaction);

                        updateScheduleParticipantStatus(request, transaction.getHistoryId());
                        break; // 다음 요청으로
                    }
                }
            }
        }

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

    /**
     * 일정 참가자 상태 업데이트 헬퍼 메서드
     */
    private void updateScheduleParticipantStatus(PaymentRequest request, Long historyId) {
        if (request.getScheduleId() != null) {
            // memberId를 통해 userId를 가져와야 함 (PaymentRequest에는 memberId=ClubMemberId 인지 UserId
            // 인지 확인 필요)
            // PaymentRequest.java 를 다시 보니 memberId 가 있음.
            // ClubMemberRepository에서 해당 member의 userId를 가져옴
            clubMemberRepository.findById(request.getMemberId()).ifPresent(member -> {
                scheduleParticipantRepository.findByScheduleIdAndUserId(request.getScheduleId(), member.getUserId())
                        .ifPresent(participant -> {
                            participant.matchTransaction(historyId);
                            scheduleParticipantRepository.save(participant);
                        });
            });
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
