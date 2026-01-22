package back.service.ledger;

import back.domain.Notifications;
import back.domain.NotificationType;
import back.domain.Users;
import back.domain.ledger.PaymentRequest;
import back.domain.ledger.TransactionLog;
import back.domain.schedule.Schedules;
import back.domain.schedule.ScheduleParticipants;
import back.dto.NotificationResponse;
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
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

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

        if (balance.compareTo(BigDecimal.ZERO) > 0 && !paidRequests.isEmpty()) {
            refundPerPerson = balance.divide(BigDecimal.valueOf(paidRequests.size()), 0, RoundingMode.FLOOR);

            // D. 환급 데이터 생성 (여기서는 PaymentRequest로 환급 대기 내역 생성 예시)
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
                // 환급은 보통 상태를 다르게 가져가거나 별도 로직 필요
                paymentRequestRepository.save(refundReq);
            }
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