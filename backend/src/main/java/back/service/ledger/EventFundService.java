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
import back.exception.ScheduleException;
import back.repository.UserRepository;
import back.repository.ledger.PaymentRequestRepository;
import back.repository.ledger.TransactionLogRepository;
import back.repository.notifications.NotificationsRepository;
import back.repository.schedule.ScheduleParticipantRepository;
import back.repository.schedule.ScheduleRepository;
import back.service.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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


    @Transactional
    public void collectEntryFees(Long clubId, Long scheduleId) {
        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        BigDecimal entryFee = schedule.getEntryFee();
        if (entryFee == null || entryFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("설정된 참가비가 없습니다.");
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

        for (ScheduleParticipants p : attendingParticipants) {
            boolean alreadyRequested = paymentRequestRepository.existsByScheduleIdAndMemberId(
                    scheduleId, p.getParticipantId());
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
                    7,
                    schedule.getEventDate().plusDays(1),
                    scheduleId,
                    null);

            paymentRequestRepository.save(req);

            // 알림 발송: "참가비 {금액}을 입금 해주세요"
            // 숫자 포맷팅 (예: 30000 -> "30000")
            String formattedAmount = entryFee.stripTrailingZeros().toPlainString();
            String message = String.format("참가비 %s을 입금 해주세요", formattedAmount);
            Notifications notification = new Notifications(
                    p.getUserId(),
                    message,
                    scheduleId,
                    NotificationType.SCHEDULE.name()
            );
            Notifications savedNotification = notificationsRepository.save(notification);

            // SSE로 실시간 알림 전송
            NotificationResponse notificationResponse = NotificationResponse.from(savedNotification, clubId);
            notificationService.send(p.getUserId(), notificationResponse);
        }


    }

    //수동 처리용
    @Transactional
    public void createFeeRequestForMember(Long clubId, Long scheduleId, Long userId) {
        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        BigDecimal entryFee = schedule.getEntryFee();
        if (entryFee == null || entryFee.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

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
                NotificationType.SCHEDULE.name()
        );
        Notifications savedNotification = notificationsRepository.save(notification);

        // SSE로 실시간 알림 전송
        NotificationResponse notificationResponse = NotificationResponse.from(savedNotification, clubId);
        notificationService.send(req.getMemberId(), notificationResponse);
    }

    @Transactional
    public void settleAndRefund(Long clubId, Long scheduleId, BigDecimal inputTotalSpent) {
        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        if (!schedule.getClubId().equals(clubId)) {
            throw new ScheduleException.NotFound();
        }

        if ("CLOSED".equals(schedule.getStatus())) {
            throw new ScheduleException.AlreadyClosed();
        }

        if ("CANCELLED".equals(schedule.getStatus())) {
            throw new ScheduleException.AlreadyCancelled();
        }

        List<PaymentRequest> paidRequests = paymentRequestRepository.findByScheduleIdAndStatus(
                scheduleId, PaymentRequest.RequestStatus.MATCHED);

        BigDecimal totalIncome = paidRequests.stream()
                .map(PaymentRequest::getExpectedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent;
        if (inputTotalSpent != null && inputTotalSpent.compareTo(BigDecimal.ZERO) >= 0) {
            totalSpent = inputTotalSpent;
        } else {
            List<TransactionLog> expenses = transactionLogRepository.findByScheduleId(scheduleId);
            totalSpent = expenses.stream()
                    .filter(tx -> "WITHDRAW".equalsIgnoreCase(tx.getType()))
                    .map(tx -> tx.getAmount().abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal balance = totalIncome.subtract(totalSpent);
        BigDecimal refundPerPerson = BigDecimal.ZERO;
        BigDecimal remainder = balance;

        if (balance.compareTo(BigDecimal.ZERO) > 0 && !paidRequests.isEmpty()) {
            refundPerPerson = balance.divide(BigDecimal.valueOf(paidRequests.size()), 0, RoundingMode.FLOOR);
            BigDecimal totalRefund = refundPerPerson.multiply(BigDecimal.valueOf(paidRequests.size()));
            remainder = balance.subtract(totalRefund);

            for (PaymentRequest originalReq : paidRequests) {
                PaymentRequest refundReq = new PaymentRequest(
                        clubId,
                        originalReq.getMemberId(),
                        originalReq.getMemberName(),
                        PaymentRequest.RequestType.SETTLEMENT,
                        refundPerPerson,
                        java.time.LocalDate.now().plusDays(3),
                        10,
                        null,
                        scheduleId,
                        null);
                paymentRequestRepository.save(refundReq);
            }
        }

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

        schedule.updateSettlement(totalSpent, refundPerPerson);
        schedule.close();
        scheduleRepository.save(schedule);
    }

    @Transactional
    public void settleAndRefund(Long clubId, Long scheduleId) {
        settleAndRefund(clubId, scheduleId, null);
    }
}