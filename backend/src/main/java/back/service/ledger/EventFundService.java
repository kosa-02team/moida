package back.service.ledger;

import back.domain.Users;
import back.domain.ledger.PaymentRequest;
import back.domain.ledger.TransactionLog;
import back.domain.schedule.Schedules;
import back.domain.schedule.ScheduleParticipants;
import back.repository.UserRepository;
import back.repository.ledger.PaymentRequestRepository;
import back.repository.ledger.TransactionLogRepository;
import back.repository.schedule.ScheduleParticipantRepository;
import back.repository.schedule.ScheduleRepository;
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

    /**
     * 1. 참가비 일괄 요청 (Collect)
     * - 일정의 참가자 전원에게 입금 요청 생성
     */
    @Transactional
    public void collectEntryFees(Long clubId, Long scheduleId) {
        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        BigDecimal entryFee = schedule.getEntryFee();
        if (entryFee == null || entryFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("설정된 참가비가 없습니다.");
        }

        List<ScheduleParticipants> participants = participantRepository.findByScheduleId(scheduleId);

        // 참가자들의 userId만 뽑아서 리스트로 만듦
        List<Long> userIds = participants.stream()
                .map(ScheduleParticipants::getUserId) // 혹은 ClubMembers 조회라면 .getUserId()
                .toList();

        //  UserRepository에서 Map<UserId, Users> 형태로 변환
        Map<Long, Users> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(Users::getUserId, user -> user));

        for (ScheduleParticipants p : participants) {
            // 이미 요청했는지 중복 체크 로직이 있으면 좋음
            Users user = userMap.get(p.getUserId()); // 메모리에서 꺼냄 (DB 조회 X)
            String realName = (user != null) ? user.getRealName() : "알수없음";

            PaymentRequest req = new PaymentRequest(
                    clubId,
                    p.getParticipantId(),
                    realName, // 실제 이름 조회 필요 시 MemberRepository 연동
                    PaymentRequest.RequestType.DEPOSIT,
                    entryFee,
                    schedule.getEventDate().toLocalDate(),
                    7,
                    schedule.getEventDate().plusDays(1),
                    scheduleId, // ✨ 일정 연결
                    null
            );
            paymentRequestRepository.save(req);
        }
    }

    /**
     * 2. 정산 및 환급 (Refund & Settle)
     * - (걷은 돈 - 쓴 돈) / 인원수 로 환급액 계산 후 처리
     */
    @Transactional
    public void settleAndRefund(Long clubId, Long scheduleId) {
        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        // A. 총 수입 (입금 완료된 건만)
        List<PaymentRequest> paidRequests = paymentRequestRepository.findByScheduleIdAndStatus(
                scheduleId, PaymentRequest.RequestStatus.MATCHED);

        BigDecimal totalIncome = paidRequests.stream()
                .map(PaymentRequest::getExpectedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // B. 총 지출 (해당 일정에 매핑된 지출 내역)
        List<TransactionLog> expenses = transactionLogRepository.findByScheduleId(scheduleId);

        BigDecimal totalSpent = expenses.stream()
                .map(TransactionLog::getAmount) // 지출은 음수라 가정 시 abs() 필요할 수 있음
                .reduce(BigDecimal.ZERO, BigDecimal::add); // 보통 지출이 음수면 더하면 됨 (잔액 = 수입 + 지출)

        // *만약 TransactionLog에 지출이 양수로 기록된다면 subtract 해야 함.
        // 여기서는 "수입 - 지출" 로직을 위해 지출을 양수(절대값) 합으로 계산한다고 가정
        // BigDecimal balance = totalIncome.subtract(totalSpent.abs());

        // C. 잔액 및 환급 계산
        BigDecimal balance = totalIncome.subtract(totalSpent);

        if (balance.compareTo(BigDecimal.ZERO) > 0 && !paidRequests.isEmpty()) {
            BigDecimal refundPerPerson = balance.divide(BigDecimal.valueOf(paidRequests.size()), 0, RoundingMode.FLOOR);

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

            // 일정에 정산 결과 업데이트
            schedule.updateSettlement(totalSpent, refundPerPerson);
        }

        schedule.close(); // 일정 마감 처리
    }
}