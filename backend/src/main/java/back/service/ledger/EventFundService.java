package back.service.ledger;

import back.bank.domain.BankAccounts;
import back.bank.repository.BankAccountRepository;
import back.domain.Notifications;
import back.domain.NotificationType;
import back.domain.Users;
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
    private final NotificationsRepository notificationsRepository;
    private final BankAccountRepository bankAccountRepository;

    /**
     * 1. 참가비 일괄 요청 (Collect)
     * - 참석 투표를 한 참가자에게만 입금 요청 생성 및 알림 발송
     */
    @Transactional
    public void collectEntryFees(Long clubId, Long scheduleId) {
        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        BigDecimal entryFee = schedule.getEntryFee();
        if (entryFee == null || entryFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("설정된 참가비가 없습니다.");
        }

        // 참석 투표를 한 참가자만 필터링 (ATTENDING만)
        List<ScheduleParticipants> attendingParticipants = participantRepository.findByScheduleId(scheduleId)
                .stream()
                .filter(p -> "ATTENDING".equals(p.getAttendanceStatus()))
                .toList();

        if (attendingParticipants.isEmpty()) {
            // 참석자가 없으면 요청 생성하지 않음
            return;
        }

        // 참가자들의 userId만 뽑아서 리스트로 만듦
        List<Long> userIds = attendingParticipants.stream()
                .map(ScheduleParticipants::getUserId)
                .toList();

        //  UserRepository에서 Map<UserId, Users> 형태로 변환
        Map<Long, Users> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(Users::getUserId, user -> user));

        for (ScheduleParticipants p : attendingParticipants) {
            // 이미 요청했는지 중복 체크
            boolean alreadyRequested = paymentRequestRepository.existsByScheduleIdAndMemberId(
                    scheduleId, p.getParticipantId());
            if (alreadyRequested) {
                continue; // 이미 요청이 있으면 스킵
            }

            Users user = userMap.get(p.getUserId());
            String realName = (user != null) ? user.getRealName() : "알수없음";

            PaymentRequest req = new PaymentRequest(
                    clubId,
                    p.getParticipantId(),
                    realName,
                    PaymentRequest.RequestType.DEPOSIT,
                    entryFee,
                    schedule.getEventDate().toLocalDate(),
                    7,
                    schedule.getEventDate().plusDays(1),
                    scheduleId,
                    null
            );
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

    /**
     * 2. 정산 및 환급 (Refund & Settle)
     * - (걷은 돈 - 쓴 돈) / 인원수 로 환급액 계산 후 처리
     * @param clubId 모임 ID
     * @param scheduleId 일정 ID
     * @param inputTotalSpent 사용자가 입력한 총 지출 금액 (null이면 TransactionLog에서 자동 계산)
     */
    @Transactional
    public void settleAndRefund(Long clubId, Long scheduleId, BigDecimal inputTotalSpent) {
        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleException.NotFound::new);

        // 일정이 해당 모임에 속하는지 확인
        if (!schedule.getClubId().equals(clubId)) {
            throw new ScheduleException.NotFound();
        }

        // 이미 마감된 일정인지 확인
        if ("CLOSED".equals(schedule.getStatus())) {
            throw new ScheduleException.AlreadyClosed();
        }

        // 이미 취소된 일정인지 확인
        if ("CANCELLED".equals(schedule.getStatus())) {
            throw new ScheduleException.AlreadyCancelled();
        }

        // A. 총 수입 (입금 완료된 건만)
        List<PaymentRequest> paidRequests = paymentRequestRepository.findByScheduleIdAndStatus(
                scheduleId, PaymentRequest.RequestStatus.MATCHED);

        BigDecimal totalIncome = paidRequests.stream()
                .map(PaymentRequest::getExpectedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // B. 총 지출 결정: 사용자 입력값이 있으면 사용, 없으면 TransactionLog에서 계산
        BigDecimal totalSpent;
        if (inputTotalSpent != null && inputTotalSpent.compareTo(BigDecimal.ZERO) >= 0) {
            totalSpent = inputTotalSpent;
        } else {
            // 해당 일정에 매핑된 지출 내역에서 자동 계산
            List<TransactionLog> expenses = transactionLogRepository.findByScheduleId(scheduleId);
            totalSpent = expenses.stream()
                    .filter(tx -> "WITHDRAW".equalsIgnoreCase(tx.getType()))
                    .map(tx -> tx.getAmount().abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // C. 잔액 및 환급 계산
        BigDecimal balance = totalIncome.subtract(totalSpent);
        BigDecimal refundPerPerson = BigDecimal.ZERO;
        BigDecimal remainder = BigDecimal.ZERO; // 나머지 금액 (귀속)

        if (balance.compareTo(BigDecimal.ZERO) > 0 && !paidRequests.isEmpty()) {
            // 환급액 계산: 잔액 / 납부 인원수 (FLOOR 처리)
            refundPerPerson = balance.divide(BigDecimal.valueOf(paidRequests.size()), 0, RoundingMode.FLOOR);
            
            // 나머지 금액 계산: 잔액 - (환급액 × 납부 인원수)
            BigDecimal totalRefund = refundPerPerson.multiply(BigDecimal.valueOf(paidRequests.size()));
            remainder = balance.subtract(totalRefund);

            // D. 환급 데이터 생성
            for (PaymentRequest originalReq : paidRequests) {
                PaymentRequest refundReq = new PaymentRequest(
                        clubId,
                        originalReq.getMemberId(),
                        originalReq.getMemberName(),
                        PaymentRequest.RequestType.SETTLEMENT, // 환급/정산 타입
                        refundPerPerson,
                        java.time.LocalDate.now().plusDays(3),
                        10,
                        null,
                        scheduleId,
                        null
                );
                paymentRequestRepository.save(refundReq);
            }
        }

        // E. 나머지 금액을 모임 통장에 귀속 (TransactionLog에 DEPOSIT 기록)
        if (remainder.compareTo(BigDecimal.ZERO) > 0) {
            // 모임의 계좌 조회
            Optional<BankAccounts> accountOpt = bankAccountRepository.findByClubId(clubId);
            Long accountId = accountOpt.map(BankAccounts::getAccountId).orElse(null);
            
            // 이전 잔액 조회
            Optional<TransactionLog> latestLog = transactionLogRepository.findLatestByClubId(clubId);
            BigDecimal previousBalance = latestLog.map(TransactionLog::getBalanceAfter).orElse(BigDecimal.ZERO);
            BigDecimal currentBalance = previousBalance.add(remainder);
            
            // TransactionLog에 나머지 금액을 DEPOSIT으로 기록
            TransactionLog remainderLog = new TransactionLog(
                    clubId,
                    scheduleId,
                    accountId,
                    "DEPOSIT",
                    remainder,
                    currentBalance,
                    String.format("일정 정산 잔액 귀속: %s", schedule.getScheduleName()),
                    null // editorId (시스템 자동 처리)
            );
            transactionLogRepository.save(remainderLog);
        }

        // 일정에 정산 결과 업데이트 (잔액이 0 이하여도 기록)
        schedule.updateSettlement(totalSpent, refundPerPerson);

        schedule.close(); // 일정 마감 처리
    }

    /**
     * 2-1. 정산 및 환급 (기존 호환용 - 자동 계산)
     */
    @Transactional
    public void settleAndRefund(Long clubId, Long scheduleId) {
        settleAndRefund(clubId, scheduleId, null);
    }
}