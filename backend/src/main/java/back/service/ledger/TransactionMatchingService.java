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

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 거래내역 매칭 서비스 (리팩토링 버전)
 * 
 * [핵심 변경점]
 * - 기존: 모든 PaymentRequest 순회 → 이름 비교 → 중복 판단 (복잡)
 * - 변경: 거래 이름으로 club_members에서 멤버 먼저 확정 → 해당 멤버 요청만 매칭 (단순)
 *
 * [흐름]
 * 1. 거래내역에서 이름 추출 (normalize)
 * 2. club_members에서 해당 이름의 멤버 조회
 * - 1명 → 멤버 확정 → 그 멤버의 PaymentRequest만 매칭 시도
 * - 0명 → 이름 없는 거래 → 기존 전체 스캔 (금액+날짜 유일 시 매칭)
 * - 2명+ → 동명이인 → 자동 매칭 금지, 후보만 저장
 */
@Service
public class TransactionMatchingService {

    // 실패 사유 상수
    public static final String REASON_NAME_DUPLICATE = "NAME_DUPLICATE";
    public static final String REASON_NAME_NOT_FOUND = "NAME_NOT_FOUND";
    public static final String REASON_AMBIGUOUS = "AMBIGUOUS_CANDIDATES";
    public static final String REASON_AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    public static final String REASON_DATE_OUT_OF_RANGE = "DATE_OUT_OF_RANGE";
    public static final String REASON_NO_PENDING = "NO_PENDING_REQUESTS";

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

    // ========================================================================
    // 1단계: 자동 매칭 수행
    // ========================================================================
    @Transactional
    public void autoMatchTransactions(Long clubId, List<BankTransactionHistory> newTransactions,
            Map<Long, TransactionLog> newTransactionLogs) {

        // 전체 PaymentRequest (fallback용)
        List<PaymentRequest> allRequests = paymentRequestRepository.findMatchableRequests(clubId);

        // 클럽 멤버 전체 조회
        List<back.repository.club.projection.MemberNameView> allMembers = clubMemberRepository
                .findAllNameViewsByClubId(clubId);

        Clubs club = clubRepository.findById(clubId).orElse(null);
        boolean isFairSettlement = club != null && club.getType() == Clubs.Type.FAIR_SETTLEMENT;

        List<MatchingResult> results = new java.util.ArrayList<>();

        for (BankTransactionHistory tx : newTransactions) {
            if (Boolean.TRUE.equals(tx.getIsMatched()))
                continue;

            MatchingResult result = matchTransaction(tx, allRequests, allMembers);
            results.add(result);

            // 매칭 성공 시 다음 거래가 중복 매칭되지 않도록 제거
            if (result.isMatched && result.matchedRequest != null) {
                allRequests.remove(result.matchedRequest);
            }
        }

        // DB 업데이트
        processMatchingResults(results, isFairSettlement, newTransactionLogs);
    }

    // ========================================================================
    // 핵심 매칭 로직 (리팩토링됨)
    // ========================================================================
    private MatchingResult matchTransaction(BankTransactionHistory tx,
            List<PaymentRequest> allRequests,
            List<back.repository.club.projection.MemberNameView> allMembers) {

        // --- Step 1: 거래 내용에서 이름 추출 ---
        String extractedName = extractNameFromContent(tx.getPrintContent());

        if (extractedName.isEmpty()) {
            // 이름 추출 실패 → 기존 fallback 로직 (금액+날짜 유일 시 매칭)
            return fallbackMatch(tx, allRequests);
        }

        // --- Step 2: club_members에서 해당 이름의 멤버 조회 ---
        List<back.repository.club.projection.MemberNameView> matchedMembers = findMembersByName(extractedName,
                allMembers);

        // --- Step 3: 분기 처리 ---
        if (matchedMembers.size() == 1) {
            // Case A: 유일한 멤버 → 해당 멤버의 요청만 매칭 시도
            return matchForUniqueMember(tx, matchedMembers.get(0), allRequests);
        } else if (matchedMembers.isEmpty()) {
            // Case B: 이름 매칭되는 멤버 없음 → fallback
            return fallbackMatch(tx, allRequests);
        } else {
            // Case C: 동명이인 (2명 이상) → 자동 매칭 금지
            return handleDuplicateNames(tx, matchedMembers, allRequests);
        }
    }

    // ========================================================================
    // Helper: 거래 내용에서 이름 추출
    // ========================================================================
    private String extractNameFromContent(String content) {
        if (content == null || content.isBlank())
            return "";

        // 정규화 (공백, 특수문자 제거 + 접미어 제거)
        String normalized = Normalizer.normalize(content, Normalizer.Form.NFC);
        String cleaned = normalized.replaceAll("\\s+", "")
                .replaceAll("[^0-9a-zA-Z가-힣]", "");

        // 접미어 제거 (어디에 있든 제거)
        cleaned = cleaned.replaceAll("(월회비|회비|입금)", "");

        return cleaned.toLowerCase();
    }

    // ========================================================================
    // Helper: 이름으로 멤버 조회 (realName 또는 nickname 매칭)
    // ========================================================================
    private List<back.repository.club.projection.MemberNameView> findMembersByName(
            String nameToFind,
            List<back.repository.club.projection.MemberNameView> allMembers) {

        return allMembers.stream()
                .filter(m -> {
                    String realName = extractNameFromContent(m.getRealName());
                    String nickname = extractNameFromContent(m.getClubNickname());
                    return nameToFind.equals(realName) || nameToFind.equals(nickname);
                })
                .collect(Collectors.toList());
    }

    // ========================================================================
    // Case A: 유일한 멤버 → 해당 멤버의 요청만 매칭
    // ========================================================================
    private MatchingResult matchForUniqueMember(BankTransactionHistory tx,
            back.repository.club.projection.MemberNameView member,
            List<PaymentRequest> allRequests) {

        Long memberId = member.getMemberId();

        // 해당 멤버의 미매칭 요청만 필터링
        List<PaymentRequest> memberRequests = allRequests.stream()
                .filter(r -> r.getMemberId().equals(memberId) && r.isMatchable())
                .collect(Collectors.toList());

        if (memberRequests.isEmpty()) {
            return new MatchingResult(tx, REASON_NO_PENDING, null);
        }

        // 금액 + 날짜 일치하는 요청 찾기
        PaymentRequest matched = null;
        for (PaymentRequest req : memberRequests) {
            if (isBasicMatch(tx, req)) {
                if (matched != null) {
                    // 같은 멤버의 요청이 여러 건 매칭됨 → AMBIGUOUS
                    return new MatchingResult(tx, REASON_AMBIGUOUS, memberRequests);
                }
                matched = req;
            }
        }

        if (matched != null) {
            System.out.println("[Matching] ✓ 유일 멤버 매칭: TxId=" + tx.getHistoryId() +
                    " → ReqId=" + matched.getRequestId() +
                    ", Member=" + member.getRealName());
            return new MatchingResult(tx, matched);
        }

        return new MatchingResult(tx, REASON_AMOUNT_MISMATCH, null);
    }

    // ========================================================================
    // Case B: 이름 매칭 없음 → Fallback (금액+날짜 유일 시 매칭)
    // ========================================================================
    private MatchingResult fallbackMatch(BankTransactionHistory tx, List<PaymentRequest> allRequests) {
        List<PaymentRequest> candidates = allRequests.stream()
                .filter(r -> r.isMatchable() && isBasicMatch(tx, r))
                .collect(Collectors.toList());

        if (candidates.size() == 1) {
            System.out.println("[Matching] ✓ Fallback 매칭 (후보 유일): TxId=" + tx.getHistoryId());
            return new MatchingResult(tx, candidates.get(0));
        } else if (candidates.isEmpty()) {
            return new MatchingResult(tx, determineFailureReason(tx, allRequests), null);
        } else {
            System.out.println("[Matching] ⚠️ Fallback 실패 (후보 다수): " + candidates.size());
            return new MatchingResult(tx, REASON_AMBIGUOUS, candidates);
        }
    }

    // ========================================================================
    // Case C: 동명이인 → 자동 매칭 금지, 후보만 저장
    // ========================================================================
    private MatchingResult handleDuplicateNames(BankTransactionHistory tx,
            List<back.repository.club.projection.MemberNameView> duplicateMembers,
            List<PaymentRequest> allRequests) {

        // 동명이인 멤버들의 요청만 후보로 저장
        List<Long> memberIds = duplicateMembers.stream()
                .map(back.repository.club.projection.MemberNameView::getMemberId)
                .collect(Collectors.toList());

        List<PaymentRequest> candidateRequests = allRequests.stream()
                .filter(r -> memberIds.contains(r.getMemberId()) && r.isMatchable())
                .collect(Collectors.toList());

        System.out.println("[Matching] ⚠️ 동명이인 감지: TxId=" + tx.getHistoryId() +
                ", 멤버 수=" + duplicateMembers.size() +
                ", 후보 요청 수=" + candidateRequests.size());

        return new MatchingResult(tx, REASON_NAME_DUPLICATE, candidateRequests);
    }

    // ========================================================================
    // 기본 매칭 조건 (금액 + 날짜 + 타입)
    // ========================================================================
    private boolean isBasicMatch(BankTransactionHistory tx, PaymentRequest req) {
        // 금액 비교
        if (tx.getAmount().abs().compareTo(req.getExpectedAmount().abs()) != 0)
            return false;

        // 타입 매칭
        String txType = tx.getInoutType();
        if ("DEPOSIT".equalsIgnoreCase(txType)) {
            if (req.getRequestType() == PaymentRequest.RequestType.SETTLEMENT)
                return false;
        } else if ("WITHDRAW".equalsIgnoreCase(txType)) {
            if (req.getRequestType() != PaymentRequest.RequestType.SETTLEMENT)
                return false;
        }

        // 날짜 범위
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        LocalDate txDate = tx.getBankTransactionAt().atZone(koreaZone).toLocalDate();
        LocalDate expected = req.getExpectedDate();
        int range = req.getMatchDaysRange() != null ? req.getMatchDaysRange() : 10;

        return !txDate.isBefore(expected.minusDays(range)) && !txDate.isAfter(expected.plusDays(range));
    }

    private String determineFailureReason(BankTransactionHistory tx, List<PaymentRequest> requests) {
        if (requests.isEmpty())
            return REASON_NO_PENDING;

        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        LocalDate txDate = tx.getBankTransactionAt().atZone(koreaZone).toLocalDate();

        boolean amountFound = false, dateFound = false;
        for (PaymentRequest req : requests) {
            if (tx.getAmount().abs().compareTo(req.getExpectedAmount().abs()) == 0)
                amountFound = true;
            int range = req.getMatchDaysRange() != null ? req.getMatchDaysRange() : 10;
            if (!txDate.isBefore(req.getExpectedDate().minusDays(range))
                    && !txDate.isAfter(req.getExpectedDate().plusDays(range)))
                dateFound = true;
        }

        if (!amountFound)
            return REASON_AMOUNT_MISMATCH;
        if (!dateFound)
            return REASON_DATE_OUT_OF_RANGE;
        return "UNKNOWN";
    }

    // ========================================================================
    // 매칭 결과 홀더
    // ========================================================================
    private static class MatchingResult {
        BankTransactionHistory transaction;
        boolean isMatched;
        PaymentRequest matchedRequest;
        List<PaymentRequest> candidates;
        String failureReason;

        public MatchingResult(BankTransactionHistory transaction, PaymentRequest matchedRequest) {
            this.transaction = transaction;
            this.isMatched = true;
            this.matchedRequest = matchedRequest;
            this.candidates = Collections.emptyList();
        }

        public MatchingResult(BankTransactionHistory transaction, String failureReason,
                List<PaymentRequest> candidates) {
            this.transaction = transaction;
            this.isMatched = false;
            this.failureReason = failureReason;
            this.candidates = candidates != null ? candidates : Collections.emptyList();
        }
    }

    // ========================================================================
    // DB 업데이트 처리
    // ========================================================================
    @Transactional
    protected void processMatchingResults(List<MatchingResult> results,
            boolean isFairSettlement, Map<Long, TransactionLog> newTransactionLogs) {

        for (MatchingResult result : results) {
            BankTransactionHistory tx = result.transaction;

            if (result.isMatched && result.matchedRequest != null) {
                PaymentRequest req = result.matchedRequest;
                if (req.getStatus() == PaymentRequest.RequestStatus.MATCHED)
                    continue;

                req.autoMatch(tx.getHistoryId());
                paymentRequestRepository.save(req);

                tx.markAsMatched();
                tx.updateUnmatchReason(null);
                tx.updateCandidateRequestIds(null);
                transactionHistoryRepository.save(tx);

                updateScheduleParticipantStatus(req, tx.getHistoryId());

                if (isFairSettlement && newTransactionLogs != null
                        && newTransactionLogs.containsKey(tx.getHistoryId())) {
                    TransactionLog log = newTransactionLogs.get(tx.getHistoryId());
                    if (log != null && req.getScheduleId() != null) {
                        log.updateScheduleId(req.getScheduleId());
                        transactionLogRepository.save(log);
                    }
                }
            } else {
                // 매칭 실패 → 후보 저장
                String candidateIds = result.candidates.isEmpty() ? null
                        : result.candidates.stream()
                                .map(r -> String.valueOf(r.getRequestId()))
                                .collect(Collectors.joining(","));

                tx.updateUnmatchReason(result.failureReason);
                tx.updateCandidateRequestIds(candidateIds);
                transactionHistoryRepository.save(tx);
            }
        }
    }

    // ========================================================================
    // 3단계: 재매칭 (후보군 재평가)
    // ========================================================================
    @Transactional
    public void retryNameMatching(Long historyId) {
        BankTransactionHistory tx = transactionHistoryRepository.findById(historyId).orElse(null);
        if (tx == null || Boolean.TRUE.equals(tx.getIsMatched()))
            return;

        String candidateIdsStr = tx.getCandidateRequestIds();
        if (candidateIdsStr == null || candidateIdsStr.isBlank())
            return;

        List<Long> candidateIds = java.util.Arrays.stream(candidateIdsStr.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<PaymentRequest> candidates = paymentRequestRepository.findAllById(candidateIds);
        List<PaymentRequest> validCandidates = candidates.stream()
                .filter(PaymentRequest::isMatchable)
                .collect(Collectors.toList());

        if (validCandidates.isEmpty()) {
            tx.updateCandidateRequestIds(null);
            tx.updateUnmatchReason(REASON_NAME_NOT_FOUND);
            transactionHistoryRepository.save(tx);
            return;
        }

        if (validCandidates.size() == 1) {
            PaymentRequest matchedReq = validCandidates.get(0);
            matchedReq.autoMatch(tx.getHistoryId());
            paymentRequestRepository.save(matchedReq);

            tx.markAsMatched();
            tx.updateUnmatchReason(null);
            tx.updateCandidateRequestIds(null);
            transactionHistoryRepository.save(tx);

            updateScheduleParticipantStatus(matchedReq, tx.getHistoryId());
        } else {
            String updatedIds = validCandidates.stream()
                    .map(r -> String.valueOf(r.getRequestId()))
                    .collect(Collectors.joining(","));
            tx.updateCandidateRequestIds(updatedIds);
            transactionHistoryRepository.save(tx);
        }
    }

    @Transactional
    public void matchRequestsWithExistingTransactions(Long clubId, List<PaymentRequest> newRequests) {
        // 미매칭 거래내역 조회
        List<BankTransactionHistory> pendingHistories = transactionHistoryRepository
                .findByClubIdAndIsMatchedFalse(clubId);

        if (pendingHistories.isEmpty() || newRequests.isEmpty()) {
            System.out.println("[Matching] 미매칭 거래 또는 새 요청 없음. 스킵.");
            return;
        }

        // 클럽 멤버 전체 조회
        List<back.repository.club.projection.MemberNameView> allMembers = clubMemberRepository
                .findAllNameViewsByClubId(clubId);

        // 전체 matchable 요청 조회 (새 요청 포함)
        List<PaymentRequest> allRequests = paymentRequestRepository.findMatchableRequests(clubId);

        System.out.println(
                "[Matching] 미매칭 거래 " + pendingHistories.size() + "건과 새 요청 " + newRequests.size() + "건 매칭 시도...");

        for (BankTransactionHistory tx : pendingHistories) {
            // 이미 매칭된 건 스킵
            if (Boolean.TRUE.equals(tx.getIsMatched()))
                continue;

            MatchingResult result = matchTransaction(tx, allRequests, allMembers);

            if (result.isMatched && result.matchedRequest != null) {
                PaymentRequest req = result.matchedRequest;
                if (req.getStatus() == PaymentRequest.RequestStatus.MATCHED)
                    continue;

                req.autoMatch(tx.getHistoryId());
                paymentRequestRepository.save(req);

                tx.markAsMatched();
                tx.updateUnmatchReason(null);
                tx.updateCandidateRequestIds(null);
                transactionHistoryRepository.save(tx);

                updateScheduleParticipantStatus(req, tx.getHistoryId());

                // 매칭된 요청은 다음 거래와 중복 매칭되지 않도록 제거
                allRequests.remove(req);

                System.out.println("[Matching] ✓ 매칭 성공: TxId=" + tx.getHistoryId() +
                        " → ReqId=" + req.getRequestId() + ", Member=" + req.getMemberName());
            } else {
                // 매칭 실패 → 후보 저장
                String candidateIds = result.candidates.isEmpty() ? null
                        : result.candidates.stream()
                                .map(r -> String.valueOf(r.getRequestId()))
                                .collect(Collectors.joining(","));

                tx.updateUnmatchReason(result.failureReason);
                tx.updateCandidateRequestIds(candidateIds);
                transactionHistoryRepository.save(tx);
            }
        }
    }

    // ========================================================================
    // 수동 매칭 메서드들 (기존 유지)
    // ========================================================================
    @Transactional
    public void manualMatch(Long requestId, Long historyId, Long matchedBy) {
        PaymentRequest request = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Not found"));
        if (!request.isMatchable())
            throw new IllegalStateException("Already matched");

        request.confirmMatch(historyId, matchedBy);
        paymentRequestRepository.save(request);

        if (historyId != null) {
            BankTransactionHistory tx = transactionHistoryRepository.findById(historyId).orElseThrow();
            tx.markAsMatched();
            tx.updateUnmatchReason(null);
            tx.updateCandidateRequestIds(null);
            transactionHistoryRepository.save(tx);
        }

        updateScheduleParticipantStatus(request, historyId);
        auditLogsRepository.save(new back.domain.ledger.AuditLogs(
                historyId != null ? historyId : -1L, matchedBy, "PENDING", "MATCHED (Manual)"));
    }

    @Transactional
    public void confirmPaymentWithoutHistory(Long requestId, Long matchedBy) {
        PaymentRequest request = paymentRequestRepository.findById(requestId).orElseThrow();
        request.confirmManualCashPayment(matchedBy);
        paymentRequestRepository.save(request);
        updateScheduleParticipantStatus(request, null);
    }

    @Transactional
    public void cancelMatch(Long requestId, Long adminId) {
        PaymentRequest request = paymentRequestRepository.findById(requestId).orElseThrow();
        Long historyId = request.getMatchedHistoryId();

        request.unmatch();
        paymentRequestRepository.save(request);

        if (historyId != null) {
            transactionHistoryRepository.findById(historyId).ifPresent(tx -> {
                tx.unmarkAsMatched();
                transactionHistoryRepository.save(tx);
            });
        }
    }

    @Transactional
    public void manualMatchMultipleRequests(List<Long> requestIds, Long historyId, Long adminId) {
        BankTransactionHistory tx = transactionHistoryRepository.findById(historyId).orElseThrow();
        List<PaymentRequest> requests = paymentRequestRepository.findAllById(requestIds);

        for (PaymentRequest req : requests) {
            req.confirmMatch(historyId, adminId);
            paymentRequestRepository.save(req);
            updateScheduleParticipantStatus(req, historyId);
        }
        tx.markAsMatched();
        tx.updateUnmatchReason(null);
        tx.updateCandidateRequestIds(null);
        transactionHistoryRepository.save(tx);
    }

    @Transactional
    public void manualMatchMultipleTransactions(Long requestId, List<Long> historyIds, Long adminId) {
        PaymentRequest request = paymentRequestRepository.findById(requestId).orElseThrow();
        Long primary = historyIds.get(0);
        request.confirmMatch(primary, adminId);
        paymentRequestRepository.save(request);

        List<BankTransactionHistory> txs = transactionHistoryRepository.findAllById(historyIds);
        for (BankTransactionHistory tx : txs) {
            tx.markAsMatched();
            tx.updateUnmatchReason(null);
            tx.updateCandidateRequestIds(null);
            transactionHistoryRepository.save(tx);
        }
        updateScheduleParticipantStatus(request, primary);
    }

    private void updateScheduleParticipantStatus(PaymentRequest request, Long historyId) {
        if (request.getScheduleId() != null) {
            clubMemberRepository.findById(request.getMemberId()).ifPresent(member -> {
                scheduleParticipantRepository.findByScheduleIdAndUserId(request.getScheduleId(), member.getUserId())
                        .ifPresent(participant -> {
                            participant.matchTransaction(historyId);
                            scheduleParticipantRepository.save(participant);
                        });
            });
        }
    }
}
