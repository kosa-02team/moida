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
import back.repository.notifications.NotificationsRepository;
import back.service.notifications.NotificationService;
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
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class EventFundServiceTest {

        @Mock
        private ScheduleRepository scheduleRepository;
        @Mock
        private ScheduleParticipantRepository participantRepository;
        @Mock
        private PaymentRequestRepository paymentRequestRepository;
        @Mock
        private TransactionLogRepository transactionLogRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private NotificationService notificationService;
        @Mock
        private NotificationsRepository notificationsRepository;
        @Mock
        private back.bank.repository.BankAccountRepository bankAccountRepository;
        @Mock
        private back.bank.service.BankService bankService;
        @Mock
        private back.service.club.ClubAuthService clubAuthService;
        @Mock
        private back.repository.club.ClubMemberRepository clubMemberRepository;

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
                ReflectionTestUtils.setField(p, "participantId", memberId);
                ReflectionTestUtils.setField(p, "userId", userId);
                ReflectionTestUtils.setField(p, "attendanceStatus", "ATTENDING");
                return p;
        }

        private Schedules createSchedule(Long id, BigDecimal fee) {
                Schedules s = new Schedules(1L, "모임", LocalDateTime.now(), LocalDateTime.now(), "장소", "설명", fee, null);
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
                        Long userId = 1L; // 요청자 ID
                        BigDecimal entryFee = BigDecimal.valueOf(10000);

                        Schedules schedule = createSchedule(scheduleId, entryFee);

                        // ✨ 리플렉션 헬퍼를 사용하여 객체 생성
                        ScheduleParticipants p1 = createParticipant(scheduleId, 1L, 10L); // user 10
                        ScheduleParticipants p2 = createParticipant(scheduleId, 2L, 11L); // user 11

                        Users user1 = createUser(10L, "홍길동");
                        Users user2 = createUser(11L, "김철수");

                        // Mock 설정
                        willDoNothing().given(clubAuthService).assertAtLeastAccountant(clubId, userId);
                        given(bankService.syncTransactionsStub(any(), any(), any(), any())).willReturn(List.of());
                        given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
                        given(participantRepository.findByScheduleId(scheduleId)).willReturn(List.of(p1, p2));
                        given(userRepository.findAllById(any())).willReturn(List.of(user1, user2));
                        given(paymentRequestRepository.existsByScheduleIdAndMemberId(scheduleId, 10L)).willReturn(false);
                        given(paymentRequestRepository.existsByScheduleIdAndMemberId(scheduleId, 11L)).willReturn(false);
                        given(notificationsRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
                        willDoNothing().given(notificationService).send(any(), any());

                        // when
                        eventFundService.collectEntryFees(clubId, scheduleId, userId);

                        // then - 참석 마감 확인
                        assertThat(schedule.getAttendanceClosedAt()).isNotNull();

                        // then
                        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
                        then(paymentRequestRepository).should(times(2)).save(captor.capture());

                        List<PaymentRequest> capturedRequests = captor.getAllValues();
                        assertThat(capturedRequests).hasSize(2);

                        // 실명이 잘 들어갔는지 확인
                        assertThat(capturedRequests).extracting("memberName")
                                        .containsExactlyInAnyOrder("홍길동", "김철수");

                        // ±10일 범위 확인
                        assertThat(capturedRequests.get(0).getMatchDaysRange()).isEqualTo(10);
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
                        Long userId = 1L; // 요청자 ID
                        Schedules schedule = createSchedule(scheduleId, BigDecimal.valueOf(10000));

                        // 참석 마감 시간 설정
                        ReflectionTestUtils.setField(schedule, "attendanceClosedAt", LocalDateTime.now().minusDays(2));

                        // PaymentRequest 생성 (실제 납부자)
                        PaymentRequest req1 = new PaymentRequest(clubId, 1L, "홍길동", PaymentRequest.RequestType.DEPOSIT,
                                        BigDecimal.valueOf(10000), null, null, null, scheduleId, null);
                        req1.autoMatch(101L);
                        PaymentRequest req2 = new PaymentRequest(clubId, 2L, "김철수", PaymentRequest.RequestType.DEPOSIT,
                                        BigDecimal.valueOf(10000), null, null, null, scheduleId, null);
                        req2.autoMatch(102L);

                        // TransactionLog 생성 (지출 내역)
                        TransactionLog log1 = new TransactionLog(clubId, scheduleId, 1L, "WITHDRAW",
                                        BigDecimal.valueOf(-5000),
                                        BigDecimal.ZERO, "간식", null, 201L);

                        // Mock 설정
                        willDoNothing().given(clubAuthService).assertAtLeastAccountant(clubId, userId);
                        given(bankService.syncTransactionsStub(any(), any(), any(), any())).willReturn(List.of());
                        given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

                        // 실제 납부자 리스트
                        given(paymentRequestRepository.findByScheduleIdAndStatus(scheduleId,
                                        PaymentRequest.RequestStatus.MATCHED))
                                        .willReturn(List.of(req1, req2));

                        // 기간 내 지출 내역
                        given(transactionLogRepository.findByClubIdAndCreatedAtBetween(any(), any(), any()))
                                        .willReturn(List.of(log1));

                        // 환급 필터링용 (환급된 내역 없음)
                        given(paymentRequestRepository.findByClubIdAndStatus(clubId,
                                        PaymentRequest.RequestStatus.MATCHED))
                                        .willReturn(List.of(req1, req2));

                        // when
                        eventFundService.settleAndRefund(clubId, scheduleId, userId);

                        // then
                        // 환급액: (20000 - 5000) / 2명 = 7500원
                        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
                        then(paymentRequestRepository).should(times(2)).save(captor.capture());

                        PaymentRequest refundReq = captor.getAllValues().get(0);
                        assertThat(refundReq.getExpectedAmount()).isEqualByComparingTo(BigDecimal.valueOf(7500));
                        assertThat(refundReq.getRequestType()).isEqualTo(PaymentRequest.RequestType.SETTLEMENT);

                        assertThat(schedule.getStatus()).isEqualTo("CLOSED");
                }

                @Test
                @DisplayName("성공: 환급(SETTLEMENT)된 지출 내역은 정산에서 제외")
                void refund_filtering_settlement_success() {
                        // given
                        Long clubId = 1L;
                        Long scheduleId = 100L;
                        Long userId = 1L; // 요청자 ID
                        Schedules schedule = createSchedule(scheduleId, BigDecimal.valueOf(10000));
                        ReflectionTestUtils.setField(schedule, "attendanceClosedAt", LocalDateTime.now().minusDays(2));

                        // Mock 설정
                        willDoNothing().given(clubAuthService).assertAtLeastAccountant(clubId, userId);
                        given(bankService.syncTransactionsStub(any(), any(), any(), any())).willReturn(List.of());
                        given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

                        // 수입: 20000원 (실제 납부자)
                        PaymentRequest req1 = new PaymentRequest(clubId, 1L, "홍길동", PaymentRequest.RequestType.DEPOSIT,
                                        BigDecimal.valueOf(10000), null, null, null, scheduleId, null);
                        req1.autoMatch(101L);
                        PaymentRequest req2 = new PaymentRequest(clubId, 2L, "김철수", PaymentRequest.RequestType.DEPOSIT,
                                        BigDecimal.valueOf(10000), null, null, null, scheduleId, null);
                        req2.autoMatch(102L);
                        given(paymentRequestRepository.findByScheduleIdAndStatus(scheduleId,
                                        PaymentRequest.RequestStatus.MATCHED))
                                        .willReturn(List.of(req1, req2));

                        // 지출: 정상 지출 5000원 + 환급 지출 3000원 (제외되어야 함)
                        TransactionLog normalExpense = new TransactionLog(clubId, scheduleId, 1L, "WITHDRAW",
                                        BigDecimal.valueOf(-5000), BigDecimal.ZERO, "간식", null, 200L);

                        TransactionLog refundExpense = new TransactionLog(clubId, null, 1L, "WITHDRAW",
                                        BigDecimal.valueOf(-3000),
                                        BigDecimal.ZERO, "환급이체", null, 301L);

                        given(transactionLogRepository.findByClubIdAndCreatedAtBetween(any(), any(), any()))
                                        .willReturn(List.of(normalExpense, refundExpense));

                        // 환급 지출 필터링 설정
                        PaymentRequest settlementReq = new PaymentRequest(clubId, 3L, "이전환급",
                                        PaymentRequest.RequestType.SETTLEMENT,
                                        BigDecimal.valueOf(3000), null, null, null, 99L, null);
                        settlementReq.confirmMatch(301L, 1L); // 301번 BankHistory와 매칭됨

                        given(paymentRequestRepository.findByClubIdAndStatus(clubId,
                                        PaymentRequest.RequestStatus.MATCHED))
                                        .willReturn(List.of(settlementReq));

                        // when
                        eventFundService.settleAndRefund(clubId, scheduleId, userId);

                        // then
                        // 지출 인정: 5000원 (3000원은 제외됨)
                        // 잔액: 20000 - 5000 = 15000원
                        // 환급액: 15000 / 2 = 7500원
                        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
                        then(paymentRequestRepository).should(times(2)).save(captor.capture());

                        PaymentRequest refundReq = captor.getAllValues().get(0);
                        assertThat(refundReq.getExpectedAmount()).isEqualByComparingTo(BigDecimal.valueOf(7500));
                }
        }
}