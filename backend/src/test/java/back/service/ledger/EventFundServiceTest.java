package back.service.ledger;

import back.domain.Users;
import back.domain.ledger.PaymentRequest;
import back.domain.ledger.TransactionLog;
import back.domain.schedule.ScheduleParticipants;
import back.domain.schedule.Schedules;
import back.repository.UserRepository;
import back.repository.ledger.PaymentRequestRepository;
import back.repository.ledger.TransactionLogRepository;
import back.repository.schedule.ScheduleParticipantRepository;
import back.repository.schedule.ScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class EventFundServiceTest {

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ScheduleParticipantRepository participantRepository;
    @Mock private PaymentRequestRepository paymentRequestRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private EventFundService eventFundService;

    // ✨ [핵심] Protected 생성자 우회를 위한 리플렉션 헬퍼 메서드
    private static <T> T newEntity(Class<T> type) {
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ✨ [핵심] 참가자 생성 헬퍼
    private ScheduleParticipants createParticipant(Long scheduleId, Long memberId, Long userId) {
        ScheduleParticipants p = newEntity(ScheduleParticipants.class);
        ReflectionTestUtils.setField(p, "scheduleId", scheduleId);
        ReflectionTestUtils.setField(p, "userId", userId);
        return p;
    }

    private Schedules createSchedule(Long id, BigDecimal fee) {
        Schedules s = new Schedules(1L, "모임", LocalDateTime.now(), LocalDateTime.now(), "장소", "설명", fee);
        ReflectionTestUtils.setField(s, "scheduleId", id);
        return s;
    }

    private Users createUser(Long id, String name) {
        Users u = new Users("loginId", "pw", name);
        ReflectionTestUtils.setField(u, "userId", id);
        return u;
    }

    @Nested
    @DisplayName("참가비 일괄 걷기")
    class CollectEntryFees {

        @Test
        @DisplayName("성공: 참가자 실명을 포함하여 PaymentRequest 생성")
        void collect_fees_success() {
            // given
            Long clubId = 1L;
            Long scheduleId = 100L;
            BigDecimal entryFee = BigDecimal.valueOf(10000);

            Schedules schedule = createSchedule(scheduleId, entryFee);

            // ✨ 리플렉션 헬퍼를 사용하여 객체 생성
            ScheduleParticipants p1 = createParticipant(scheduleId, 1L, 10L); // user 10
            ScheduleParticipants p2 = createParticipant(scheduleId, 2L, 11L); // user 11

            Users user1 = createUser(10L, "홍길동");
            Users user2 = createUser(11L, "김철수");

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(participantRepository.findByScheduleId(scheduleId)).willReturn(List.of(p1, p2));
            given(userRepository.findAllById(any())).willReturn(List.of(user1, user2));

            // when
            eventFundService.collectEntryFees(clubId, scheduleId);

            // then
            ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
            then(paymentRequestRepository).should(times(2)).save(captor.capture());

            List<PaymentRequest> capturedRequests = captor.getAllValues();
            assertThat(capturedRequests).hasSize(2);

            // 실명이 잘 들어갔는지 확인
            assertThat(capturedRequests).extracting("memberName")
                    .containsExactlyInAnyOrder("홍길동", "김철수");
        }
    }

    @Nested
    @DisplayName("정산 및 환급")
    class SettleAndRefund {

        @Test
        @DisplayName("성공: 수입 > 지출일 때 환급 요청 생성")
        void refund_success() {
            // given
            Long clubId = 1L;
            Long scheduleId = 100L;
            Schedules schedule = createSchedule(scheduleId, BigDecimal.valueOf(10000));

            // PaymentRequest 생성 (테스트용)
            PaymentRequest req1 = new PaymentRequest(clubId, 1L, "홍길동", PaymentRequest.RequestType.DEPOSIT, BigDecimal.valueOf(10000), null, null, null, scheduleId, null);
            PaymentRequest req2 = new PaymentRequest(clubId, 2L, "김철수", PaymentRequest.RequestType.DEPOSIT, BigDecimal.valueOf(10000), null, null, null, scheduleId, null);

            // TransactionLog 생성 (테스트용)
            TransactionLog log1 = new TransactionLog(clubId, scheduleId, 1L, "EXPENSE", BigDecimal.valueOf(5000), BigDecimal.ZERO, "간식", null);

            // ✨ [누락된 부분 추가] "DB에서 일정을 찾아달라고 하면, 방금 만든 schedule 객체를 줘라"
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

            // 나머지 Mocking
            given(paymentRequestRepository.findByScheduleIdAndStatus(scheduleId, PaymentRequest.RequestStatus.MATCHED))
                    .willReturn(List.of(req1, req2));
            given(transactionLogRepository.findByScheduleId(scheduleId))
                    .willReturn(List.of(log1));

            // when
            eventFundService.settleAndRefund(clubId, scheduleId);

            // then
            // 환급액: (20000 - 5000) / 2 = 7500원
            ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
            then(paymentRequestRepository).should(times(2)).save(captor.capture());

            PaymentRequest refundReq = captor.getAllValues().get(0);
            assertThat(refundReq.getExpectedAmount()).isEqualByComparingTo(BigDecimal.valueOf(7500));
            assertThat(refundReq.getRequestType()).isEqualTo(PaymentRequest.RequestType.SETTLEMENT);

            assertThat(schedule.getStatus()).isEqualTo("CLOSED");
        }
    }
}