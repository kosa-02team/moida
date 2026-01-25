package back.service.ledger;

import back.bank.domain.BankAccounts;
import back.bank.repository.BankAccountRepository;
import back.domain.Notifications;
import back.domain.Users;
import back.domain.NotificationType;
import back.domain.ledger.PaymentRequest;
import back.domain.ledger.TransactionLog;
import back.domain.schedule.Schedules;
import back.domain.schedule.ScheduleParticipants;
import back.dto.NotificationResponse;
import back.dto.ledger.request.AdditionalFeeRequest;
import back.exception.ScheduleException;
import back.repository.UserRepository;
import back.repository.club.ClubMemberRepository;
import back.repository.ledger.PaymentRequestRepository;
import back.repository.ledger.TransactionLogRepository;
import back.repository.notifications.NotificationsRepository;
import back.repository.schedule.ScheduleParticipantRepository;
import back.repository.schedule.ScheduleRepository;
import back.service.club.ClubAuthService;
import back.service.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import back.bank.domain.BankTransactionHistory;
import back.bank.repository.BankTransactionHistoryRepository;

@Service
@RequiredArgsConstructor
public class EventFundService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleParticipantRepository participantRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BankAccountRepository bankAccountRepository;
    private final NotificationsRepository notificationsRepository;
    private final back.bank.service.BankService bankService;
    private final ClubAuthService clubAuthService;
    private final ClubMemberRepository clubMemberRepository;
    private final BankTransactionHistoryRepository bankTransactionHistoryRepository;
    private final TransactionMatchingService transactionMatchingService;

    @Transactional
    public void collectEntryFees(Long clubId, Long scheduleId, Long userId) {
        System.out.println("💰 [참가비 요청 생성 시작] clubId=" + clubId + ", scheduleId=" + scheduleId + ", userId=" + userId);

        // 권한 체크: 총무 이상
        try {
            clubAuthService.assertAtLeastAccountant(clubId, userId);
            System.out.println("  ✓ 권한 체크 통과 (총무 이상)");
        } catch (Exception e) {
            System.err.println("  ❌ 권한 체크 실패: " + e.getMessage());
            throw e;
        }

        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        // clubId 일치 검증
        if (!schedule.getClubId().equals(clubId)) {
            System.err.println("  ❌ clubId 불일치: schedule.clubId=" + schedule.getClubId() + ", 요청 clubId=" + clubId);
            throw new ScheduleException.NotFound();
        }

        BigDecimal entryFee = schedule.getEntryFee();
        if (entryFee == null || entryFee.compareTo(BigDecimal.ZERO) <= 0) {
            System.err.println("  ❌ 참가비가 없거나 0원: entryFee=" + entryFee);
            throw new ScheduleException.NotFound();
        }

        System.out.println("  ✓ 일정 조회 성공: entryFee=" + entryFee);

        List<ScheduleParticipants> attendingParticipants = participantRepository.findByScheduleId(scheduleId)
                .stream()
                .filter(p -> "ATTENDING".equals(p.getAttendanceStatus()))
                .toList();

        System.out.println("  → 참석자 수: " + attendingParticipants.size() + "명");

        if (attendingParticipants.isEmpty()) {
            System.out.println("  ⚠️ 참석자가 없어서 요청 생성 안 함");
            return;
        }

        List<Long> userIds = attendingParticipants.stream()
                .map(ScheduleParticipants::getUserId)
                .toList();

        Map<Long, Users> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(Users::getUserId, user -> user));

        // 참석 마감 처리
        schedule.closeAttendance();

        // 은행 거래내역 동기화 (입금 내역 조회)
        // 은행 계좌가 있는 경우에만 동기화 시도
        try {
            if (bankAccountRepository.findByClubId(clubId).isPresent()) {
                bankService.syncTransactionsStub(clubId, 1L, null, null);
            }
        } catch (Exception e) {
            // 동기화 실패 시 로깅만 하고 계속 진행
            org.slf4j.LoggerFactory.getLogger(EventFundService.class)
                    .warn("Bank sync failed during collectEntryFees: clubId={}, scheduleId={}, error={}",
                            clubId, scheduleId, e.getMessage(), e);
        }

        // PaymentRequest 생성
        List<PaymentRequest> createdRequests = new ArrayList<>();

        for (ScheduleParticipants p : attendingParticipants) {
            // userId를 club_members.member_id로 변환
            Long memberId = clubMemberRepository.findByClubIdAndUserIdAndStatus(
                    clubId, p.getUserId(), back.domain.club.ClubMembers.Status.ACTIVE)
                    .map(back.domain.club.ClubMembers::getMemberId)
                    .orElse(null);

            if (memberId == null) {
                // 활성 멤버가 아니면 스킵
                continue;
            }

            boolean alreadyRequested = paymentRequestRepository.existsByScheduleIdAndMemberId(
                    scheduleId, memberId);
            if (alreadyRequested) {
                continue;
            }

            Users user = userMap.get(p.getUserId());
            String realName = (user != null) ? user.getRealName() : "알수없음";

            // expectedDate는 일정 날짜로 설정 (거래 날짜와의 매칭 범위를 넓히기 위해)
            // matchDaysRange가 ±10일이므로, 일정 날짜 기준으로 넓은 범위에서 매칭 가능
            LocalDate expectedDate = schedule.getEventDate().toLocalDate();

            PaymentRequest req = new PaymentRequest(
                    clubId,
                    memberId, // userId 대신 memberId 사용
                    realName,
                    PaymentRequest.RequestType.DEPOSIT,
                    entryFee,
                    expectedDate, // 일정 날짜로 설정
                    10, // ±10일 범위
                    schedule.getEventDate().plusDays(1),
                    scheduleId,
                    null);

            PaymentRequest savedReq = paymentRequestRepository.save(req);
            createdRequests.add(savedReq);

            // 디버깅: 입금 요청 생성 로그
            System.out.println("💰 [참가비 요청 생성] requestId=" + savedReq.getRequestId() +
                    ", memberName=" + realName + ", amount=" + entryFee +
                    ", expectedDate=" + expectedDate + ", scheduleId=" + scheduleId);

            // 알림 발송
            String formattedAmount = entryFee.stripTrailingZeros().toPlainString();
            String message = String.format("참가비 %s을 입금 해주세요", formattedAmount);
            Notifications notification = new Notifications(
                    p.getUserId(),
                    message,
                    scheduleId,
                    NotificationType.SCHEDULE.name());
            Notifications savedNotification = notificationsRepository.save(notification);

            // SSE로 실시간 알림 전송
            NotificationResponse notificationResponse = NotificationResponse.from(savedNotification, clubId);
            notificationService.send(p.getUserId(), notificationResponse);
        }

        scheduleRepository.save(schedule);

        // 생성된 요청들을 기존 미매칭 거래내역과 자동 매칭 시도
        if (!createdRequests.isEmpty()) {
            System.out.println("🔄 [자동 매칭 시작] clubId=" + clubId + ", 생성된 요청 수=" + createdRequests.size());
            transactionMatchingService.matchRequestsWithExistingTransactions(clubId, createdRequests);
            System.out.println("🔄 [자동 매칭 완료]");
        }
    }

    // 수동 처리용
    @Transactional
    public void createFeeRequestForMember(Long clubId, Long scheduleId, Long userId, Long requestingUserId) {
        // 권한 체크: 총무 이상
        clubAuthService.assertAtLeastAccountant(clubId, requestingUserId);

        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        // clubId 일치 검증
        if (!schedule.getClubId().equals(clubId)) {
            throw new ScheduleException.NotFound();
        }

        BigDecimal entryFee = schedule.getEntryFee();
        if (entryFee == null || entryFee.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(ScheduleException.NotFound::new);

        // userId를 club_members.member_id로 변환
        Long memberId = clubMemberRepository.findByClubIdAndUserIdAndStatus(
                clubId, userId, back.domain.club.ClubMembers.Status.ACTIVE)
                .map(back.domain.club.ClubMembers::getMemberId)
                .orElseThrow(() -> new ScheduleException.NotFound());

        PaymentRequest req = new PaymentRequest(
                clubId,
                memberId, // userId 대신 memberId 사용
                user.getRealName(),
                PaymentRequest.RequestType.DEPOSIT,
                entryFee,
                LocalDate.now(), // 현재 날짜로 설정 (실제 거래 날짜와 맞추기 위해)
                7,
                schedule.getEventDate().plusDays(1),
                scheduleId,
                null);

        paymentRequestRepository.save(req);

        String formattedAmount = entryFee.stripTrailingZeros().toPlainString();
        String message = String.format("참가비 %s을 입금 해주세요", formattedAmount);
        Notifications notification = new Notifications(
                userId, // 알림은 userId로 전송
                message,
                scheduleId,
                NotificationType.SCHEDULE.name());
        Notifications savedNotification = notificationsRepository.save(notification);

        // SSE로 실시간 알림 전송
        NotificationResponse notificationResponse = NotificationResponse.from(savedNotification, clubId);
        notificationService.send(userId, notificationResponse);
    }

    @Transactional
    public void settleAndRefund(Long clubId, Long scheduleId, BigDecimal inputTotalSpent, Long userId) {
        // 권한 체크: 총무 이상
        clubAuthService.assertAtLeastAccountant(clubId, userId);

        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        // clubId 일치 검증
        if (!schedule.getClubId().equals(clubId)) {
            throw new ScheduleException.NotFound();
        }

        if ("CANCELLED".equals(schedule.getStatus())) {
            throw new ScheduleException.AlreadyCancelled();
        }

        // 환급 전 은행 거래내역 동기화 (출금 내역 조회)
        try {
            bankService.syncTransactionsStub(clubId, 2L, null, null);
        } catch (Exception e) {
            // 동기화 실패 시 로깅만 하고 계속 진행
            System.err.println("Bank sync failed during settlement: " + e.getMessage());
        }

        // [1] 실제 납부된 요청(MATCHED)만 조회하여 환급 대상 및 인원 수(N) 확정
        List<PaymentRequest> paidRequests = paymentRequestRepository.findByScheduleIdAndStatus(
                scheduleId, PaymentRequest.RequestStatus.MATCHED);

        if (paidRequests.isEmpty()) {
            // 수입이 없으면 정산할 것도 없음
            schedule.updateSettlement(BigDecimal.ZERO, BigDecimal.ZERO);
            schedule.close();
            scheduleRepository.save(schedule);
            return;
        }

        // [1-1] 중복 제거: 같은 memberId의 PaymentRequest는 하나만 계산
        Map<Long, PaymentRequest> uniquePaidRequests = new HashMap<>();
        for (PaymentRequest req : paidRequests) {
            Long memberId = req.getMemberId();
            if (!uniquePaidRequests.containsKey(memberId)) {
                uniquePaidRequests.put(memberId, req);
            } else {
                // 이미 있으면 더 큰 금액을 선택
                PaymentRequest existing = uniquePaidRequests.get(memberId);
                if (req.getExpectedAmount().compareTo(existing.getExpectedAmount()) > 0) {
                    uniquePaidRequests.put(memberId, req);
                }
            }
        }

        List<PaymentRequest> deduplicatedRequests = new ArrayList<>(uniquePaidRequests.values());
        System.out
                .println("💰 [정산] 총 요청 수: " + paidRequests.size() + "건, 중복 제거 후: " + deduplicatedRequests.size() + "건");

        // [2] 총 수입 계산 (중복 제거 후)
        BigDecimal totalIncome = deduplicatedRequests.stream()
                .map(PaymentRequest::getExpectedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // [3] 지출 계산
        BigDecimal totalSpent;
        if (inputTotalSpent != null && inputTotalSpent.compareTo(BigDecimal.ZERO) >= 0) {
            // 수동 입력 지출
            totalSpent = inputTotalSpent;
        } else {
            // 자동 계산: 일정 시작일 7일 전 ~ 일정 종료일 7일 후까지의 출금 내역 합산 (더 넓은 범위)
            LocalDateTime settlementStart = schedule.getEventDate().minusDays(7); // 일정 시작 7일 전부터
            LocalDateTime settlementEnd = schedule.getEndDate().plusDays(7).with(java.time.LocalTime.MAX); // 일정 종료 7일
                                                                                                           // 후까지

            // 해당 기간의 TransactionLog 조회 (WITHDRAW만)
            // 주의: createdAt이 아닌 실제 거래 날짜(bankTransactionAt) 기준으로 필터링
            // 넓은 범위로 조회한 후 실제 거래 날짜로 필터링
            List<TransactionLog> allWithdraws = transactionLogRepository
                    .findByClubIdAndCreatedAtBetweenOrderByCreatedAtDescTransactionIdDesc(
                            clubId,
                            settlementStart.minusDays(30), // 넓은 범위로 조회 (동기화 지연 고려)
                            settlementEnd.plusDays(30))
                    .stream()
                    .filter(tx -> "WITHDRAW".equalsIgnoreCase(tx.getType()))
                    .toList();

            // 실제 거래 날짜 기준으로 필터링
            List<TransactionLog> expenses = new ArrayList<>();
            System.out.println("💰 [지출 자동 계산] 조회된 WITHDRAW 거래 수: " + allWithdraws.size() + "건");
            System.out.println("  → 정산 기간: " + settlementStart + " ~ " + settlementEnd);
            System.out.println("  → 일정 기간: " + schedule.getEventDate() + " ~ " + schedule.getEndDate());

            for (TransactionLog tx : allWithdraws) {
                LocalDateTime actualTransactionDate;
                if (tx.getBankHistoryId() != null) {
                    // bankHistoryId가 있으면 실제 거래 날짜 조회
                    Optional<BankTransactionHistory> history = bankTransactionHistoryRepository
                            .findById(tx.getBankHistoryId());
                    if (history.isPresent()) {
                        actualTransactionDate = history.get().getBankTransactionAt();
                    } else {
                        // BankTransactionHistory를 찾을 수 없으면 createdAt 사용
                        actualTransactionDate = tx.getCreatedAt();
                    }
                } else {
                    // bankHistoryId가 없으면 수동 입력 거래이므로 createdAt 사용
                    actualTransactionDate = tx.getCreatedAt();
                }

                // 실제 거래 날짜가 정산 기간 내에 있는지 확인
                if (!actualTransactionDate.isBefore(settlementStart)
                        && !actualTransactionDate.isAfter(settlementEnd)) {
                    expenses.add(tx);
                    System.out.println("  ✓ 지출 포함: " + tx.getDescription() + " (" + tx.getAmount().abs() + "원, 거래일: "
                            + actualTransactionDate + ")");
                } else {
                    System.out.println("  ✗ 지출 제외: " + tx.getDescription() + " (" + tx.getAmount().abs() + "원, 거래일: "
                            + actualTransactionDate + " - 기간 밖)");
                }
            }

            System.out.println("  → 정산 기간 내 지출 거래 수: " + expenses.size() + "건");

            // [4] 환급 거래 제외: bankHistoryId를 통해 PaymentRequest(SETTLEMENT)와 매칭된 거래 필터링
            List<Long> settlementHistoryIds = paymentRequestRepository
                    .findByClubIdAndStatus(clubId, PaymentRequest.RequestStatus.MATCHED)
                    .stream()
                    .filter(req -> req.getRequestType() == PaymentRequest.RequestType.SETTLEMENT)
                    .map(PaymentRequest::getMatchedHistoryId)
                    .filter(id -> id != null)
                    .toList();

            totalSpent = expenses.stream()
                    .filter(tx -> tx.getBankHistoryId() == null
                            || !settlementHistoryIds.contains(tx.getBankHistoryId()))
                    .map(tx -> tx.getAmount().abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // [5] 잔액 및 환급액 계산
        BigDecimal balance = totalIncome.subtract(totalSpent);
        BigDecimal refundPerPerson = BigDecimal.ZERO;
        BigDecimal remainder = balance;

        // 잔액이 있고 납부자가 있으면 환급 처리
        if (balance.compareTo(BigDecimal.ZERO) > 0 && !deduplicatedRequests.isEmpty()) {
            // N빵 계산 (소수점 버림)
            refundPerPerson = balance.divide(BigDecimal.valueOf(deduplicatedRequests.size()), 0, RoundingMode.FLOOR);
            BigDecimal totalRefund = refundPerPerson.multiply(BigDecimal.valueOf(deduplicatedRequests.size()));
            remainder = balance.subtract(totalRefund);
        }

        // [6] 환급 처리 (실제 납부자에게만, 잔액이 있으면 반드시 실행)
        if (balance.compareTo(BigDecimal.ZERO) > 0 && !deduplicatedRequests.isEmpty()
                && refundPerPerson.compareTo(BigDecimal.ZERO) > 0) {
            Optional<BankAccounts> accountOpt = bankAccountRepository.findByClubId(clubId);
            Long accountId = accountOpt.map(BankAccounts::getAccountId).orElse(null);

            // 환급 전 최신 잔액 조회 (한 번만)
            Optional<TransactionLog> initialLatestLog = transactionLogRepository.findLatestByClubId(clubId);
            BigDecimal runningBalance = initialLatestLog.map(TransactionLog::getBalanceAfter).orElse(BigDecimal.ZERO);

            for (PaymentRequest originalReq : deduplicatedRequests) {
                // 환급 요청 생성
                PaymentRequest refundReq = new PaymentRequest(
                        clubId,
                        originalReq.getMemberId(),
                        originalReq.getMemberName(),
                        PaymentRequest.RequestType.SETTLEMENT,
                        refundPerPerson,
                        java.time.LocalDate.now().plusDays(3), // 환급 예상일
                        10,
                        null,
                        scheduleId,
                        null);
                paymentRequestRepository.save(refundReq);

                // 장부에 환급 내역 기록 (잔액 누적 계산) - 반드시 기록
                runningBalance = runningBalance.subtract(refundPerPerson); // 환급은 출금

                TransactionLog refundLog = new TransactionLog(
                        clubId,
                        scheduleId,
                        accountId,
                        "WITHDRAW",
                        refundPerPerson.negate(), // 음수로 저장
                        runningBalance,
                        String.format("환급: %s (%s)", originalReq.getMemberName(), schedule.getScheduleName()),
                        null);
                // 중요: 환급 로그가 기존 지출보다 나중에(상단에) 오도록 시간 보정 (Sleep 대신 나노초 추가 불가하므로 그대로 저장하되 ID 역순
                // 믿음, 필요시 수정)
                // 만약 정렬이 꼬인다면 DB 트리거 확인 필요. 일단 여기서는 그대로 저장.
                // 사용자가 "출금 -> 환급 -> 입금" 나온다고 했으므로 환급이 더 과거로 인식됨? -> 아님.
                // 확실히 하기 위해 flush 호출 고려했으나 성능상 제외.
                transactionLogRepository.save(refundLog);

                // 참가자 환급 상태 자동 업데이트
                Optional<ScheduleParticipants> participantOpt = participantRepository
                        .findByScheduleIdAndUserId(scheduleId, originalReq.getMemberId());
                participantOpt.ifPresent(participant -> {
                    participant.markRefunded();
                    participantRepository.save(participant);
                });
            }
        }

        // [7] 입금하지 않은 참가자를 불참으로 자동 변경
        List<ScheduleParticipants> allParticipants = participantRepository.findByScheduleId(scheduleId);
        Set<Long> paidMemberIds = deduplicatedRequests.stream()
                .map(PaymentRequest::getMemberId)
                .collect(Collectors.toSet());

        // memberId 리스트로 ClubMembers 조회하여 userId 추출
        List<back.domain.club.ClubMembers> paidClubMembers = clubMemberRepository.findAllById(paidMemberIds);
        Set<Long> paidUserIds = paidClubMembers.stream()
                .map(back.domain.club.ClubMembers::getUserId)
                .collect(Collectors.toSet());

        for (ScheduleParticipants participant : allParticipants) {
            // 입금하지 않은 참가자는 불참으로 변경 (userId 기준 비교)
            if (!paidUserIds.contains(participant.getUserId())) {
                participant.notAttend();
                participantRepository.save(participant);
            }
        }

        // [8] 잔액 귀속 처리 (나머지 금액)
        if (remainder.compareTo(BigDecimal.ZERO) > 0) {
            Optional<BankAccounts> accountOpt = bankAccountRepository.findByClubId(clubId);
            Long accountId = accountOpt.map(BankAccounts::getAccountId).orElse(null);

            // 환급 처리 후 최신 잔액 조회
            Optional<TransactionLog> latestLog = transactionLogRepository.findLatestByClubId(clubId);
            BigDecimal previousBalance = latestLog.map(TransactionLog::getBalanceAfter).orElse(BigDecimal.ZERO);
            BigDecimal currentBalance = previousBalance.add(remainder);

            TransactionLog remainderLog = new TransactionLog(
                    clubId,
                    scheduleId,
                    accountId,
                    "DEPOSIT",
                    remainder,
                    currentBalance,
                    String.format("일정 정산 잔액 귀속: %s", schedule.getScheduleName()),
                    null);
            transactionLogRepository.save(remainderLog);
        }

        // [9] 일정 정산 완료 처리
        schedule.updateSettlement(totalSpent, refundPerPerson);
        schedule.close();
        scheduleRepository.save(schedule);
    }

    @Transactional
    public void settleAndRefund(Long clubId, Long scheduleId, Long userId) {
        settleAndRefund(clubId, scheduleId, null, userId);
    }

    /**
     * 정산 미리보기 (실제 저장하지 않고 계산 결과만 반환)
     */
    @Transactional(readOnly = true)
    public back.dto.schedule.SettlementPreviewResponse previewSettlement(Long clubId, Long scheduleId) {
        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        if (!schedule.getClubId().equals(clubId)) {
            throw new ScheduleException.NotFound();
        }

        // [1] 실제 납부된 요청(MATCHED)만 조회
        List<PaymentRequest> paidRequests = paymentRequestRepository.findByScheduleIdAndStatus(
                scheduleId, PaymentRequest.RequestStatus.MATCHED);

        System.out.println("💰 [정산 미리보기] 시작: scheduleId=" + scheduleId + ", clubId=" + clubId);
        System.out.println("  → 조회된 MATCHED PaymentRequest 수: " + paidRequests.size() + "건");

        if (paidRequests.isEmpty()) {
            System.out.println("  ⚠️ 납부된 요청이 없어 정산할 수 없음");
            return new back.dto.schedule.SettlementPreviewResponse(
                    0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // 각 PaymentRequest 상세 정보 로깅
        System.out.println("  → PaymentRequest 상세:");
        for (PaymentRequest req : paidRequests) {
            System.out.println("    - requestId: " + req.getRequestId() + ", memberId: " + req.getMemberId()
                    + ", memberName: " + req.getMemberName() + ", amount: " + req.getExpectedAmount() + "원");
        }

        // [1-1] 중복 제거: 같은 memberId의 PaymentRequest는 하나만 계산
        // 가장 최근에 매칭된 것만 사용 (또는 가장 큰 금액만 사용)
        Map<Long, PaymentRequest> uniquePaidRequests = new HashMap<>();
        for (PaymentRequest req : paidRequests) {
            Long memberId = req.getMemberId();
            if (!uniquePaidRequests.containsKey(memberId)) {
                uniquePaidRequests.put(memberId, req);
            } else {
                // 이미 있으면 더 큰 금액 또는 더 최근 것을 선택
                PaymentRequest existing = uniquePaidRequests.get(memberId);
                if (req.getExpectedAmount().compareTo(existing.getExpectedAmount()) > 0) {
                    System.out.println("    → 중복 발견: memberId=" + memberId + ", 기존: " + existing.getExpectedAmount()
                            + "원, 새로운: " + req.getExpectedAmount() + "원 (더 큰 금액 선택)");
                    uniquePaidRequests.put(memberId, req);
                } else {
                    System.out.println("    → 중복 발견: memberId=" + memberId + ", 기존: " + existing.getExpectedAmount()
                            + "원, 새로운: " + req.getExpectedAmount() + "원 (기존 유지)");
                }
            }
        }

        List<PaymentRequest> deduplicatedRequests = new ArrayList<>(uniquePaidRequests.values());
        System.out.println(
                "💰 [정산 미리보기] 총 요청 수: " + paidRequests.size() + "건, 중복 제거 후: " + deduplicatedRequests.size() + "건");

        // 중복 제거 후 상세 정보 로깅
        System.out.println("  → 중복 제거 후 PaymentRequest 상세:");
        for (PaymentRequest req : deduplicatedRequests) {
            System.out.println("    - memberId: " + req.getMemberId() + ", memberName: " + req.getMemberName()
                    + ", amount: " + req.getExpectedAmount() + "원");
        }

        // [2] 총 수입 계산 (중복 제거 후)
        BigDecimal totalIncome = deduplicatedRequests.stream()
                .map(PaymentRequest::getExpectedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // [3] 지출 자동 계산
        // 정산 기간: 일정 시작일 ~ 일정 종료일 + 7일 (더 넓은 범위로 설정하여 모든 관련 지출 포함)
        LocalDateTime settlementStart = schedule.getEventDate().minusDays(7); // 일정 시작 7일 전부터
        LocalDateTime settlementEnd = schedule.getEndDate().plusDays(7).with(java.time.LocalTime.MAX); // 일정 종료 7일 후까지

        // 해당 기간의 TransactionLog 조회 (WITHDRAW만)
        // 주의: createdAt이 아닌 실제 거래 날짜(bankTransactionAt) 기준으로 필터링
        // 넓은 범위로 조회한 후 실제 거래 날짜로 필터링
        List<TransactionLog> allWithdraws = transactionLogRepository
                .findByClubIdAndCreatedAtBetweenOrderByCreatedAtDescTransactionIdDesc(
                        clubId,
                        settlementStart.minusDays(30), // 넓은 범위로 조회 (동기화 지연 고려)
                        settlementEnd.plusDays(30))
                .stream()
                .filter(tx -> "WITHDRAW".equalsIgnoreCase(tx.getType()))
                .toList();

        // 실제 거래 날짜 기준으로 필터링
        List<TransactionLog> expenses = new ArrayList<>();
        System.out.println("💰 [지출 자동 계산] 조회된 WITHDRAW 거래 수: " + allWithdraws.size() + "건");
        System.out.println("  → 정산 기간: " + settlementStart + " ~ " + settlementEnd);
        System.out.println("  → 일정 기간: " + schedule.getEventDate() + " ~ " + schedule.getEndDate());

        for (TransactionLog tx : allWithdraws) {
            LocalDateTime actualTransactionDate;
            if (tx.getBankHistoryId() != null) {
                // bankHistoryId가 있으면 실제 거래 날짜 조회
                Optional<BankTransactionHistory> history = bankTransactionHistoryRepository
                        .findById(tx.getBankHistoryId());
                if (history.isPresent()) {
                    actualTransactionDate = history.get().getBankTransactionAt();
                } else {
                    // BankTransactionHistory를 찾을 수 없으면 createdAt 사용
                    actualTransactionDate = tx.getCreatedAt();
                }
            } else {
                // bankHistoryId가 없으면 수동 입력 거래이므로 createdAt 사용
                actualTransactionDate = tx.getCreatedAt();
            }

            // 실제 거래 날짜가 정산 기간 내에 있는지 확인
            if (!actualTransactionDate.isBefore(settlementStart)
                    && !actualTransactionDate.isAfter(settlementEnd)) {
                expenses.add(tx);
                System.out.println("  ✓ 지출 포함: " + tx.getDescription() + " (" + tx.getAmount().abs() + "원, 거래일: "
                        + actualTransactionDate + ")");
            } else {
                System.out.println("  ✗ 지출 제외: " + tx.getDescription() + " (" + tx.getAmount().abs() + "원, 거래일: "
                        + actualTransactionDate + " - 기간 밖)");
            }
        }

        System.out.println("  → 정산 기간 내 지출 거래 수: " + expenses.size() + "건");

        List<Long> settlementHistoryIds = paymentRequestRepository
                .findByClubIdAndStatus(clubId, PaymentRequest.RequestStatus.MATCHED)
                .stream()
                .filter(req -> req.getRequestType() == PaymentRequest.RequestType.SETTLEMENT)
                .map(PaymentRequest::getMatchedHistoryId)
                .filter(id -> id != null)
                .toList();

        System.out.println("  → 환급 거래로 제외할 bankHistoryId 수: " + settlementHistoryIds.size() + "건");
        if (!settlementHistoryIds.isEmpty()) {
            System.out.println("    - 제외할 bankHistoryId: " + settlementHistoryIds);
        }

        // 지출 거래 필터링 (환급 거래 제외)
        List<TransactionLog> finalExpenses = new ArrayList<>();
        BigDecimal totalSpent = BigDecimal.ZERO;
        for (TransactionLog tx : expenses) {
            boolean isSettlement = tx.getBankHistoryId() != null
                    && settlementHistoryIds.contains(tx.getBankHistoryId());
            if (!isSettlement) {
                finalExpenses.add(tx);
                totalSpent = totalSpent.add(tx.getAmount().abs());
                System.out.println("  ✓ 최종 지출 포함: " + tx.getDescription() + " (" + tx.getAmount().abs()
                        + "원, bankHistoryId: " + tx.getBankHistoryId() + ")");
            } else {
                System.out.println("  ✗ 환급 거래로 제외: " + tx.getDescription() + " (" + tx.getAmount().abs()
                        + "원, bankHistoryId: " + tx.getBankHistoryId() + ")");
            }
        }

        System.out.println("  → 최종 지출 거래 수: " + finalExpenses.size() + "건");

        // [4] 잔액 및 환급액 계산
        BigDecimal balance = totalIncome.subtract(totalSpent);
        BigDecimal refundPerPerson = BigDecimal.ZERO;

        System.out.println("💰 [정산 미리보기] 계산 시작:");
        System.out.println("  → 납부 인원: " + deduplicatedRequests.size() + "명");
        System.out.println("  → 총 수입: " + totalIncome + "원");
        System.out.println("  → 총 지출: " + totalSpent + "원");
        System.out.println("  → 잔액: " + balance + "원");

        if (balance.compareTo(BigDecimal.ZERO) > 0 && !deduplicatedRequests.isEmpty()) {
            refundPerPerson = balance.divide(BigDecimal.valueOf(deduplicatedRequests.size()), 0, RoundingMode.FLOOR);
            System.out.println("  → 잔액이 양수이므로 환급 계산: 1인당 " + refundPerPerson + "원");
        } else {
            System.out.println("  → 잔액이 0 이하이거나 납부자가 없으므로 환급 없음");
        }

        BigDecimal totalRefund = refundPerPerson.multiply(BigDecimal.valueOf(deduplicatedRequests.size()));

        System.out.println("💰 [정산 미리보기] 최종 결과:");
        System.out.println("  → 납부 인원: " + deduplicatedRequests.size() + "명");
        System.out.println("  → 총 수입: " + totalIncome + "원");
        System.out.println("  → 총 지출: " + totalSpent + "원");
        System.out.println("  → 잔액: " + balance + "원");
        System.out.println("  → 1인당 환급: " + refundPerPerson + "원");
        System.out.println("  → 총 환급액: " + totalRefund + "원");

        return new back.dto.schedule.SettlementPreviewResponse(
                deduplicatedRequests.size(), // 중복 제거된 납부 인원
                totalIncome,
                totalSpent,
                balance,
                refundPerPerson,
                totalRefund);
    }

    @Transactional
    public void requestAdditionalFee(
            Long clubId,
            Long scheduleId,
            AdditionalFeeRequest request,
            Long userId) {

        // 1. 권한 체크: 총무 이상
        clubAuthService.assertAtLeastAccountant(clubId, userId);

        // 2. 일정 조회 및 상태 확인
        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        // clubId 일치 검증
        if (!schedule.getClubId().equals(clubId)) {
            throw new ScheduleException.NotFound();
        }

        if (!"OPEN".equals(schedule.getStatus())) {
            throw new ScheduleException.NotOpen();
        }

        LocalDate now = LocalDate.now();
        LocalDate eventDate = schedule.getEventDate().toLocalDate();
        if (now.isBefore(eventDate)) {
            throw new ScheduleException.NotStarted();
        }

        // 3. 참석자 조회 (ATTENDING만)
        List<ScheduleParticipants> attendees = participantRepository
                .findByScheduleId(scheduleId)
                .stream()
                .filter(p -> "ATTENDING".equals(p.getAttendanceStatus()))
                .toList();

        if (attendees.isEmpty()) {
            throw new ScheduleException.NoAttendees();
        }

        // 4. 각 참석자에 대해 PaymentRequest 생성
        LocalDate expectedDate = now.plusDays(7); // 7일 후까지
        List<PaymentRequest> requests = new ArrayList<>();

        for (ScheduleParticipants attendee : attendees) {
            Users user = userRepository.findById(attendee.getUserId())
                    .orElseThrow(ScheduleException.NotFound::new);

            // userId를 club_members.member_id로 변환
            Long memberId = clubMemberRepository.findByClubIdAndUserIdAndStatus(
                    clubId, attendee.getUserId(), back.domain.club.ClubMembers.Status.ACTIVE)
                    .map(back.domain.club.ClubMembers::getMemberId)
                    .orElse(null);

            if (memberId == null) {
                // 활성 멤버가 아니면 스킵
                continue;
            }

            PaymentRequest paymentRequest = new PaymentRequest(
                    clubId,
                    memberId, // userId 대신 memberId 사용
                    user.getRealName(),
                    PaymentRequest.RequestType.SETTLEMENT,
                    request.amountPerPerson(),
                    expectedDate,
                    10,
                    null,
                    scheduleId,
                    null);

            requests.add(paymentRequest);
        }

        paymentRequestRepository.saveAll(requests);

        // 5. 알림 전송
        String reason = request.reason() != null && !request.reason().isBlank()
                ? request.reason()
                : "추가 회비";

        String formattedAmount = request.amountPerPerson().stripTrailingZeros().toPlainString();

        for (ScheduleParticipants attendee : attendees) {
            String message = String.format("[%s] %s 요청: %s원",
                    schedule.getScheduleName(),
                    reason,
                    formattedAmount);

            Notifications notification = new Notifications(
                    attendee.getUserId(),
                    message,
                    scheduleId,
                    NotificationType.PAYMENT_REQUEST.name());
            Notifications savedNotification = notificationsRepository.save(notification);

            // SSE로 실시간 알림 전송
            NotificationResponse notificationResponse = NotificationResponse.from(savedNotification, clubId);
            notificationService.send(attendee.getUserId(), notificationResponse);
        }
    }
}