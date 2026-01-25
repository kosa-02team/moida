package back.service.ledger;

import back.bank.domain.BankTransactionHistory;
import back.domain.ledger.PaymentRequest;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import back.repository.club.projection.NameView;
import back.bank.repository.BankTransactionHistoryRepository;
import back.repository.ledger.PaymentRequestRepository;
import back.repository.ledger.TransactionLogRepository;
import back.repository.ledger.AuditLogsRepository;
import back.repository.schedule.ScheduleParticipantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionMatchingServiceTest {

        @InjectMocks
        private TransactionMatchingService transactionMatchingService;

        @Mock
        private PaymentRequestRepository paymentRequestRepository;
        @Mock
        private BankTransactionHistoryRepository transactionHistoryRepository;
        @Mock
        private ClubMemberRepository clubMemberRepository;
        @Mock
        private ClubRepository clubRepository;
        @Mock
        private TransactionLogRepository transactionLogRepository;
        @Mock
        private ScheduleParticipantRepository scheduleParticipantRepository;
        @Mock
        private AuditLogsRepository auditLogsRepository;

        @Test
        @DisplayName("출금(WITHDRAW) 트랜잭션은 정산(SETTLEMENT) 요청과 매칭되어야 한다")
        void matchWithdrawWithSettlement() {
                Long clubId = 1L;
                Long memberId = 200L;
                BigDecimal amount = new BigDecimal("50000.00");
                LocalDateTime now = LocalDateTime.now();

                // Transaction
                BankTransactionHistory tx = mock(BankTransactionHistory.class);
                when(tx.getAmount()).thenReturn(amount);
                when(tx.getBankTransactionAt()).thenReturn(now);
                when(tx.getPrintContent()).thenReturn("홍길동 환급");
                when(tx.getHistoryId()).thenReturn(999L);
                when(tx.getInoutType()).thenReturn("WITHDRAW");

                // PaymentRequest
                PaymentRequest request = mock(PaymentRequest.class);
                when(request.getExpectedAmount()).thenReturn(amount);
                when(request.getRequestType()).thenReturn(PaymentRequest.RequestType.SETTLEMENT);
                when(request.isMatchable()).thenReturn(true);
                when(request.getClubId()).thenReturn(clubId);
                when(request.getMemberId()).thenReturn(memberId);
                when(request.getExpectedDate()).thenReturn(now.toLocalDate());
                when(request.getMatchDaysRange()).thenReturn(10);

                when(paymentRequestRepository.findMatchableRequests(clubId))
                                .thenReturn(List.of(request));

                // NameView
                NameView memberView = mock(NameView.class);
                when(memberView.getRealName()).thenReturn("홍길동");
                when(memberView.getClubNickname()).thenReturn("길동이");
                when(clubMemberRepository.findNameView(clubId, memberId))
                                .thenReturn(Optional.of(memberView));
                when(clubMemberRepository.countByClubIdAndRealName(clubId, "홍길동"))
                                .thenReturn(1L);

                // when
                transactionMatchingService.autoMatchTransactions(clubId, List.of(tx), new HashMap<>());

                // then
                verify(request).autoMatch(999L);
                verify(paymentRequestRepository).save(request);
        }

        @Test
        @DisplayName("동명이인(중복 이름)이 있어도 금액/날짜가 맞으면 매칭되어야 한다 (Bug Fix Verification)")
        void matchDuplicateNames() {
                Long clubId = 1L;
                Long memberIdA = 100L;
                Long memberIdB = 101L; // Same name
                BigDecimal amount = new BigDecimal("10000.00");
                LocalDateTime now = LocalDateTime.now();

                // Transaction from "홍길동"
                BankTransactionHistory tx = mock(BankTransactionHistory.class);
                when(tx.getAmount()).thenReturn(amount);
                when(tx.getBankTransactionAt()).thenReturn(now);
                when(tx.getPrintContent()).thenReturn("홍길동");
                when(tx.getHistoryId()).thenReturn(888L);
                when(tx.getInoutType()).thenReturn("DEPOSIT");

                // Request A (Correct one that we want to match first or at least one of them)
                PaymentRequest requestA = mock(PaymentRequest.class);
                when(requestA.getExpectedAmount()).thenReturn(amount);
                when(requestA.getRequestType()).thenReturn(PaymentRequest.RequestType.MEMBERSHIP_FEE);
                when(requestA.isMatchable()).thenReturn(true);
                when(requestA.getClubId()).thenReturn(clubId);
                when(requestA.getMemberId()).thenReturn(memberIdA);
                when(requestA.getExpectedDate()).thenReturn(now.toLocalDate());
                when(requestA.getMatchDaysRange()).thenReturn(10);

                // Request B (Another person same name)
                PaymentRequest requestB = mock(PaymentRequest.class);
                // We only mock requestA behavior to verify it matches AT LEAST ONE.

                when(paymentRequestRepository.findMatchableRequests(clubId))
                                .thenReturn(List.of(requestA));

                // NameView for A
                NameView memberViewA = mock(NameView.class);
                when(memberViewA.getRealName()).thenReturn("홍길동");
                when(memberViewA.getClubNickname()).thenReturn("홍홍");
                when(clubMemberRepository.findNameView(clubId, memberIdA))
                                .thenReturn(Optional.of(memberViewA));

                // Count is 2 (Duplicate!)
                when(clubMemberRepository.countByClubIdAndRealName(clubId, "홍길동"))
                                .thenReturn(2L);
                // Nickname uniqueness
                when(clubMemberRepository.countByClubIdAndClubNickname(clubId, "홍홍"))
                                .thenReturn(1L);

                // when
                transactionMatchingService.autoMatchTransactions(clubId, List.of(tx), new HashMap<>());

                // then
                // Should match because duplicate name allows match if amount/date correct
                verify(requestA).autoMatch(888L);
                verify(paymentRequestRepository).save(requestA);
        }
}
