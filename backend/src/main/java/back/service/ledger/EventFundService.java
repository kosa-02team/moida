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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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


    @Transactional
    public void collectEntryFees(Long clubId, Long scheduleId, Long userId) {
        // 권한 체크: 총무 이상
        clubAuthService.assertAtLeastAccountant(clubId, userId);

        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        // clubId 일치 검증
        if (!schedule.getClubId().equals(clubId)) {
            throw new ScheduleException.NotFound();
        }

        BigDecimal entryFee = schedule.getEntryFee();
        if (entryFee == null || entryFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ScheduleException.NotFound();
        }

        List<ScheduleParticipants> attendingParticipants = participantRepository.findByScheduleId(scheduleId)
                .stream()
                .filter(p -> "ATTENDING".equals(p.getAttendanceStatus()))
                .toList();

        if (attendingParticipants.isEmpty()) {
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
        try {
            bankService.syncTransactionsStub(clubId, 1L, null, null);
        } catch (Exception e) {
            // 동기화 실패 시 로깅만 하고 계속 진행
            System.err.println("Bank sync failed during attendance closure: " + e.getMessage());
        }

        // PaymentRequest 생성
        for (ScheduleParticipants p : attendingParticipants) {
            boolean alreadyRequested = paymentRequestRepository.existsByScheduleIdAndMemberId(
                    scheduleId, p.getUserId());
            if (alreadyRequested) {
                continue;
            }

            Users user = userMap.get(p.getUserId());
            String realName = (user != null) ? user.getRealName() : "알수없음";

            PaymentRequest req = new PaymentRequest(
                    clubId,
                    p.getUserId(),
                    realName,
                    PaymentRequest.RequestType.DEPOSIT,
                    entryFee,
                    schedule.getEventDate().toLocalDate(),
                    10, // ±10일 범위
                    schedule.getEventDate().plusDays(1),
                    scheduleId,
                    null);

            paymentRequestRepository.save(req);

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

        PaymentRequest req = new PaymentRequest(
                clubId,
                userId,
                user.getRealName(),
                PaymentRequest.RequestType.DEPOSIT,
                entryFee,
                schedule.getEventDate().toLocalDate(),
                7,
                schedule.getEventDate().plusDays(1),
                scheduleId,
                null);

        paymentRequestRepository.save(req);

        String formattedAmount = entryFee.stripTrailingZeros().toPlainString();
        String message = String.format("참가비 %s을 입금 해주세요", formattedAmount);
        Notifications notification = new Notifications(
                req.getMemberId(),
                message,
                scheduleId,
                NotificationType.SCHEDULE.name());
        Notifications savedNotification = notificationsRepository.save(notification);

        // SSE로 실시간 알림 전송
        NotificationResponse notificationResponse = NotificationResponse.from(savedNotification, clubId);
        notificationService.send(req.getMemberId(), notificationResponse);
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

        // [2] 총 수입 계산 (실제 납부액 기준)
        BigDecimal totalIncome = paidRequests.stream()
                .map(PaymentRequest::getExpectedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // [3] 지출 계산
        BigDecimal totalSpent;
        if (inputTotalSpent != null && inputTotalSpent.compareTo(BigDecimal.ZERO) >= 0) {
            // 수동 입력 지출
            totalSpent = inputTotalSpent;
        } else {
            // 자동 계산: 참석 마감 시점 이후 ~ 일정 종료일 + 1일까지의 출금 내역 합산
            LocalDateTime attendanceClosedAt = schedule.getAttendanceClosedAt();
            if (attendanceClosedAt == null) {
                // 참석 마감이 되지 않은 경우, 일정 시작일 사용
                attendanceClosedAt = schedule.getEventDate();
            }

            LocalDateTime settlementEnd = schedule.getEndDate().plusDays(1).with(java.time.LocalTime.MAX);

            // 해당 기간의 TransactionLog 조회 (WITHDRAW만)
            List<TransactionLog> expenses = transactionLogRepository
                    .findByClubIdAndCreatedAtBetween(clubId, attendanceClosedAt, settlementEnd)
                    .stream()
                    .filter(tx -> "WITHDRAW".equalsIgnoreCase(tx.getType()))
                    .toList();

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

        if (balance.compareTo(BigDecimal.ZERO) > 0 && !paidRequests.isEmpty()) {
            // N빵 계산 (소수점 버림)
            refundPerPerson = balance.divide(BigDecimal.valueOf(paidRequests.size()), 0, RoundingMode.FLOOR);
            BigDecimal totalRefund = refundPerPerson.multiply(BigDecimal.valueOf(paidRequests.size()));
            remainder = balance.subtract(totalRefund);

            // [6] 환급 요청 생성 (실제 납부자에게만)
            for (PaymentRequest originalReq : paidRequests) {
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
            }
        }

        // [7] 잔액 귀속 처리 (나머지 금액)
        if (remainder.compareTo(BigDecimal.ZERO) > 0) {
            Optional<BankAccounts> accountOpt = bankAccountRepository.findByClubId(clubId);
            Long accountId = accountOpt.map(BankAccounts::getAccountId).orElse(null);

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

        // [8] 일정 정산 완료 처리
        schedule.updateSettlement(totalSpent, refundPerPerson);
        schedule.close();
        scheduleRepository.save(schedule);
    }

    @Transactional
    public void settleAndRefund(Long clubId, Long scheduleId, Long userId) {
        settleAndRefund(clubId, scheduleId, null, userId);
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

            PaymentRequest paymentRequest = new PaymentRequest(
                    clubId,
                    user.getUserId(),
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
                    NotificationType.PAYMENT_REQUEST.name()
            );
            Notifications savedNotification = notificationsRepository.save(notification);

            // SSE로 실시간 알림 전송
            NotificationResponse notificationResponse = NotificationResponse.from(savedNotification, clubId);
            notificationService.send(attendee.getUserId(), notificationResponse);
        }
    }
}