package back.bank.service;

import back.bank.domain.BankAccounts;
import back.bank.domain.BankTransactionHistory;
import back.bank.domain.Banks;
import back.bank.dto.request.AccountCreateRequest;
import back.bank.dto.response.AccountCreateResponse;
import back.bank.dto.response.BankTransaction;
import back.bank.provider.BankProvider;
import back.bank.provider.BankProviderRegistry;
import back.bank.repository.BankAccountRepository;
import back.bank.repository.BankRepository;
import back.bank.repository.BankTransactionHistoryRepository;
import back.domain.ledger.TransactionLog;
import back.dto.ledger.request.RefundRequest;
import back.dto.ledger.response.RefundResponse;
import back.repository.ledger.TransactionLogRepository;
import back.service.ledger.TransactionMatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.junit.jupiter.api.AfterEach;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class BankServiceTests {

        @InjectMocks
        private BankService bankService;

        @Mock
        private BankProviderRegistry registry;

        @Mock
        private BankAccountRepository bankAccountRepository;

        @Mock
        private BankTransactionHistoryRepository transactionHistoryRepository;

        @Mock
        private BankRepository bankRepository;

        @Mock
        private TransactionLogRepository transactionLogRepository;

        @Mock
        private TransactionMatchingService transactionMatchingService;

        @Mock
        private BankProvider bankProvider;

        @BeforeEach
        void setUp() {
                TransactionSynchronizationManager.initSynchronization();
        }

        @AfterEach
        void tearDown() {
                TransactionSynchronizationManager.clearSynchronization();
        }

        private void triggerAfterCommit() {
                List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager
                                .getSynchronizations();
                for (TransactionSynchronization synchronization : synchronizations) {
                        synchronization.afterCommit();
                }
        }

        @Test
        @DisplayName("계좌 생성 성공")
        void createAccount_Success() {
                Banks bank = mock(Banks.class);
                Long clubId = 1L;

                AccountCreateRequest request = new AccountCreateRequest(1L, "STUB", "123456", "홍길동");

                given(registry.get("STUB")).willReturn(bankProvider);
                given(bankProvider.createAccount(any()))
                                .willReturn(new AccountCreateResponse(true, "110-1234", "성공"));
                given(bankRepository.findByBankCode("STUB"))
                                .willReturn(Optional.of(bank));

                BankAccounts savedAccount = new BankAccounts(clubId, "STUB", 1L, bank, "110-1234", "홍길동");

                given(bankAccountRepository.save(any()))
                                .willReturn(savedAccount);

                BankAccounts result = bankService.createAccount(clubId, request);

                assertThat(result).isNotNull();
                assertThat(result.getAccountNumber()).isEqualTo("110-1234");
        }

        @Test
        @DisplayName("계좌 생성 실패 - 은행 코드 미지원")
        void createAccount_Fail_InvalidBankCode() {
                Long clubId = 1L;

                AccountCreateRequest request = new AccountCreateRequest(1L, "INVALID", "123456", "홍길동");

                given(registry.get("INVALID")).willReturn(bankProvider);
                given(bankProvider.createAccount(any()))
                                .willReturn(new AccountCreateResponse(true, "110-1234", "성공"));
                given(bankRepository.findByBankCode("INVALID"))
                                .willReturn(Optional.empty());

                assertThatThrownBy(() -> bankService.createAccount(clubId, request))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("지원하지 않는 은행 코드");
        }

        @Test
        @DisplayName("거래내역 동기화 - 날짜 지정")
        void syncTransactions_WithDates() {
                Banks bank = mock(Banks.class);
                Long clubId = 1L;

                LocalDate from = LocalDate.of(2026, 1, 1);
                LocalDate to = LocalDate.of(2026, 1, 31);

                BankAccounts account = new BankAccounts(clubId, "STUB", 1L, bank, "110-1234", "홍길동");

                BankTransaction tx = new BankTransaction(
                                "TX1",
                                LocalDateTime.now(),
                                "DEPOSIT",
                                BigDecimal.valueOf(10000),
                                BigDecimal.valueOf(10000),
                                "입금");

                BankTransactionHistory savedHistory = mock(BankTransactionHistory.class);
                given(savedHistory.getHistoryId()).willReturn(1L);

                given(bankAccountRepository.findByClubId(clubId))
                                .willReturn(Optional.of(account));
                given(registry.get("STUB"))
                                .willReturn(bankProvider);
                given(bankProvider.getTransactions(anyString(), eq(from), eq(to)))
                                .willReturn(new java.util.ArrayList<>(List.of(tx)));
                given(transactionHistoryRepository.save(any()))
                                .willReturn(savedHistory);
                given(transactionLogRepository.save(any()))
                                .willReturn(mock(TransactionLog.class));

                bankService.syncTransactions(clubId, from, to);

                triggerAfterCommit();

                verify(transactionHistoryRepository).save(any());
                verify(transactionLogRepository).save(any());
                verify(transactionMatchingService)
                                .autoMatchTransactions(eq(clubId), any(), any());
        }

        @Test
        @DisplayName("거래내역 동기화 - 날짜 자동 (첫 동기화)")
        void syncTransactions_AutoDates_FirstTime() {
                Banks bank = mock(Banks.class);
                Long clubId = 1L;

                BankAccounts account = new BankAccounts(clubId, "STUB", 1L, bank, "110-1234", "홍길동");

                given(transactionLogRepository.findLatestByClubId(clubId))
                                .willReturn(Optional.empty());
                given(bankAccountRepository.findByClubId(clubId))
                                .willReturn(Optional.of(account));
                given(registry.get("STUB"))
                                .willReturn(bankProvider);
                given(bankProvider.getTransactions(anyString(), any(), any()))
                                .willReturn(new java.util.ArrayList<>(List.of()));

                bankService.syncTransactions(clubId, null, null);

                triggerAfterCommit();

                verify(bankProvider).getTransactions(
                                anyString(),
                                eq(LocalDate.now().minusDays(30)),
                                eq(LocalDate.now()));
        }

        @Test
        @DisplayName("모임 정산 환급 성공")
        void refundToMember_Success() {
                Banks bank = mock(Banks.class);

                RefundRequest request = new RefundRequest(
                                1L,
                                100L,
                                "홍길동",
                                "KB",
                                "123-456",
                                BigDecimal.valueOf(5000),
                                "환급");

                RefundResponse response = new RefundResponse(
                                true,
                                "TX123",
                                "성공",
                                BigDecimal.valueOf(5000),
                                "홍길동",
                                "123-***");

                BankAccounts account = new BankAccounts(1L, "STUB", 1L, bank, "110-1234", "모임장");

                given(bankAccountRepository.findByClubId(1L))
                                .willReturn(Optional.of(account));
                given(registry.get("STUB"))
                                .willReturn(bankProvider);
                given(bankProvider.refund(request))
                                .willReturn(response);
                given(transactionLogRepository.save(any()))
                                .willReturn(mock(TransactionLog.class));
                given(transactionHistoryRepository.save(any()))
                                .willReturn(mock(BankTransactionHistory.class));

                bankService.refundToMember(request);

                verify(transactionLogRepository).save(any());
                verify(transactionHistoryRepository).save(any());
        }
}
