package back.service.ledger;

import back.bank.domain.BankTransactionHistory;
import back.domain.ledger.PaymentRequest;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import back.repository.club.projection.NameView;
import back.bank.repository.BankTransactionHistoryRepository;
import back.repository.ledger.PaymentRequestRepository;
import back.repository.ledger.TransactionLogRepository;
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
        when(tx.getInoutType()).thenReturn("WITHDRAW"); // ★ 필수

        // PaymentRequest
        PaymentRequest request = mock(PaymentRequest.class);
        when(request.getExpectedAmount()).thenReturn(amount);
        when(request.getRequestType()).thenReturn(PaymentRequest.RequestType.SETTLEMENT);
        when(request.isMatchable()).thenReturn(true);
        when(request.getClubId()).thenReturn(clubId);
        when(request.getMemberId()).thenReturn(memberId);
        when(request.getExpectedDate()).thenReturn(now.toLocalDate());
        when(request.getMatchDaysRange()).thenReturn(10); // ★ 필수

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
}
