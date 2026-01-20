package back.bank.service;

import back.bank.domain.BankAccounts;
import back.bank.domain.BankTransactionHistory;
import back.bank.dto.request.AccountCreateRequest;
import back.dto.ledger.request.RefundRequest;
import back.bank.dto.request.TransferRequest;
import back.bank.dto.response.AccountCreateResponse;
import back.bank.dto.response.AccountOwnerResponse;
import back.bank.dto.response.BankTransaction;
import back.dto.ledger.response.RefundResponse;
import back.bank.dto.response.TransferResponse;
import back.bank.provider.BankProvider;
import back.bank.provider.BankProviderRegistry;
import back.bank.repository.BankAccountRepository;
import back.bank.repository.BankRepository;
import back.bank.repository.BankTransactionHistoryRepository;
import back.domain.ledger.TransactionLog;
import back.repository.ledger.TransactionLogRepository;
import back.service.ledger.TransactionMatchingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class BankService {

        private final BankProviderRegistry registry;
        private final BankAccountRepository bankAccountRepository;
        private final BankTransactionHistoryRepository transactionHistoryRepository;
        private final BankRepository bankRepository;
        private final TransactionLogRepository transactionLogRepository;
        private final TransactionMatchingService transactionMatchingService;

        public BankService(BankProviderRegistry registry,
                        BankAccountRepository bankAccountRepository,
                        BankTransactionHistoryRepository transactionHistoryRepository,
                        BankRepository bankRepository,
                        TransactionLogRepository transactionLogRepository,
                        TransactionMatchingService transactionMatchingService) {
                this.registry = registry;
                this.bankAccountRepository = bankAccountRepository;
                this.transactionHistoryRepository = transactionHistoryRepository;
                this.bankRepository = bankRepository;
                this.transactionLogRepository = transactionLogRepository;
                this.transactionMatchingService = transactionMatchingService;
        }

        public AccountOwnerResponse checkOwner(String bankCode, String accountNumber) {
                return registry.get(bankCode).inquireAccountOwner(accountNumber);
        }

        public boolean confirmDepositByAmount(
                        String bankCode,
                        String accountNumber,
                        BigDecimal expectedAmount,
                        LocalDate from,
                        LocalDate to) {
                // "입금 기능"이 아니라 "입금 확인"만: 거래내역에서 DEPOSIT + 금액 매칭
                return registry.get(bankCode).getTransactions(accountNumber, from, to).stream()
                                .anyMatch(tx -> "DEPOSIT".equalsIgnoreCase(tx.type())
                                                && tx.amount().compareTo(expectedAmount) == 0);
        }

        public TransferResponse sendMoney(String fromBankCode, TransferRequest command) {
                return registry.get(fromBankCode).transfer(command);
        }

        /**
         * 모임 가상계좌 생성
         * 
         * @param clubId  모임 ID
         * @param request 계좌 생성 요청 (bankCode 포함)
         * @return 생성된 계좌 정보
         */
        @Transactional
        public BankAccounts createAccount(Long clubId, AccountCreateRequest request) {
                // 1. bankCode로 Provider 선택
                String bankCode = request.bankCode();
                if (bankCode == null || bankCode.isBlank()) {
                        throw new IllegalArgumentException("bankCode는 필수입니다.");
                }

                BankProvider provider = registry.get(bankCode);

                // 2. Provider를 통해 계좌 생성
                AccountCreateResponse response = provider.createAccount(request);

                if (!response.success()) {
                        throw new RuntimeException("계좌 생성 실패: " + response.message());
                }

                // 3. Banks 엔티티 조회 (bankCode로)
                var bank = bankRepository.findByBankCode(bankCode)
                                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 은행 코드: " + bankCode));

                // 4. DB에 저장
                BankAccounts account = new BankAccounts(
                                clubId,
                                bankCode,
                                request.userId(), // userId는 String으로 넘어오므로 변환
                                bank,
                                response.accountNumber(),
                                request.ownerName());

                return bankAccountRepository.save(account);
        }

        /**
         * 모임 가상계좌 거래내역 동기화
         * - 오픈뱅킹 API를 호출하여 실제 은행 거래내역을 가져옴
         * - BankTransactionHistory에 은행 거래내역 저장
         * - TransactionLog에 회계 원장 저장
         * 
         * @param clubId 모임 ID
         * @param from   조회 시작 날짜
         * @param to     조회 종료 날짜
         * @return 저장된 TransactionLog 목록
         */
        @Transactional
        public List<TransactionLog> syncTransactions(Long clubId, LocalDate from, LocalDate to) {
                // 0. 날짜 범위 자동 설정
                LocalDate actualFrom = from;
                LocalDate actualTo = to;

                if (actualFrom == null || actualTo == null) {
                        // 마지막 거래 날짜 조회
                        var latestTransaction = transactionLogRepository.findLatestByClubId(clubId);

                        // 마지막 거래 다음날부터
                        // 첫 동기화인 경우 30일 전부터
                        actualFrom = latestTransaction
                                        .map(transactionLog -> transactionLog.getCreatedAt().toLocalDate().plusDays(1))
                                        .orElseGet(() -> LocalDate.now().minusDays(30));

                        // 오늘까지
                        actualTo = LocalDate.now();
                }

                // 1. clubId로 계좌 조회
                // [refactor] 예외처리 수정 필요
                BankAccounts account = bankAccountRepository.findByClubId(clubId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "해당 모임의 계좌를 찾을 수 없습니다. clubId: " + clubId));

                // 2. 계좌의 bankCode로 Provider 선택
                String bankCode = account.getBankCode();
                BankProvider provider = registry.get(bankCode);

                // 3. Provider를 통해 오픈뱅킹 API 호출 (거래내역 조회)
                List<BankTransaction> bankTransactions = provider.getTransactions(account.getAccountNumber(),
                                actualFrom,
                                actualTo);

                List<TransactionLog> savedLogs = new ArrayList<>();
                List<BankTransactionHistory> savedHistories = new ArrayList<>();
                Map<Long, TransactionLog> historyToLogMap = new HashMap<>();

                // 4. 각 거래내역을 BankTransactionHistory와 TransactionLog에 저장
                for (BankTransaction tx : bankTransactions) {
                        // 중복 확인 (uniqueTxKey로)
                        if (transactionHistoryRepository.existsByUniqueTxKey(tx.txId())) {
                                continue; // 이미 저장된 거래내역은 스킵
                        }

                        // 4-1. BankTransactionHistory에 저장 (실제 은행 거래내역)
                        BankTransactionHistory history = new BankTransactionHistory(
                                        clubId,
                                        tx.occurredAt(),
                                        tx.printContent(), // senderName
                                        tx.amount().abs(), // 무조건 양수로 저장
                                        tx.txId(), // uniqueTxKey로 사용
                                        tx.type() // inoutType
                        );
                        transactionHistoryRepository.save(history);

                        // 4-2. TransactionLog에 저장 (회계 원장)
                        TransactionLog log = new TransactionLog(
                                        clubId,
                                        account.getAccountId(),
                                        tx.type(), // "DEPOSIT" or "WITHDRAW"
                                        tx.amount(),
                                        tx.balanceAfter(),
                                        tx.printContent(),
                                        null // editorId는 시스템 자동 동기화이므로 null
                        );
                        TransactionLog savedLog = transactionLogRepository.save(log);
                        savedLogs.add(savedLog);

                        // 새로 저장된 내역 수집 및 매핑
                        savedHistories.add(history);
                        historyToLogMap.put(history.getHistoryId(), savedLog);
                }

                // 5. 자동 매칭 수행 (새로 저장된 거래내역과 입금요청 매칭)
                transactionMatchingService.autoMatchTransactions(clubId, savedHistories, historyToLogMap);

                return savedLogs;
        }

        @Transactional
        public List<TransactionLog> syncTransactionsStub(Long clubId, Long stubId, LocalDate from, LocalDate to) {
                // 0. 날짜 범위 자동 설정
                LocalDate actualFrom = from;
                LocalDate actualTo = to;

                if (actualFrom == null || actualTo == null) {
                        // 마지막 거래 날짜 조회
                        var latestTransaction = transactionLogRepository.findLatestByClubId(clubId);

                        // 마지막 거래 다음날부터
                        // 첫 동기화인 경우 30일 전부터
                        actualFrom = latestTransaction
                                        .map(transactionLog -> transactionLog.getCreatedAt().toLocalDate().plusDays(1))
                                        .orElseGet(() -> LocalDate.now().minusDays(30));

                        // 오늘까지
                        actualTo = LocalDate.now();
                }

                // 1. clubId로 계좌 조회
                // [refactor] 예외처리 수정 필요
                BankAccounts account = bankAccountRepository.findByClubId(clubId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "해당 모임의 계좌를 찾을 수 없습니다. clubId: " + clubId));

                // 2. 계좌의 bankCode로 Provider 선택
                String bankCode = account.getBankCode();
                BankProvider provider = registry.get(bankCode);

                // 3. Provider를 통해 오픈뱅킹 API 호출 (거래내역 조회)
                List<BankTransaction> bankTransactions = provider.getTransactionsStub(account.getAccountNumber(),
                                stubId, actualFrom,
                                actualTo);

                List<TransactionLog> savedLogs = new ArrayList<>();
                List<BankTransactionHistory> savedHistories = new ArrayList<>();
                Map<Long, TransactionLog> historyToLogMap = new HashMap<>();

                // 4. 각 거래내역을 BankTransactionHistory와 TransactionLog에 저장
                for (BankTransaction tx : bankTransactions) {
                        // 중복 확인 (uniqueTxKey로)
                        if (transactionHistoryRepository.existsByUniqueTxKey(tx.txId())) {
                                continue; // 이미 저장된 거래내역은 스킵
                        }

                        // 4-1. BankTransactionHistory에 저장 (실제 은행 거래내역)
                        BankTransactionHistory history = new BankTransactionHistory(
                                        clubId,
                                        tx.occurredAt(),
                                        tx.printContent(), // senderName
                                        tx.amount().abs(), // 무조건 양수로 저장
                                        tx.txId(), // uniqueTxKey로 사용
                                        tx.type() // inoutType
                        );
                        transactionHistoryRepository.save(history);

                        // 4-2. TransactionLog에 저장 (회계 원장)
                        // accountId 가 필요, 지금은 모임장 id가 들어감
                        TransactionLog log = new TransactionLog(
                                        clubId,
                                        account.getAccountId(),
                                        tx.type(), // "DEPOSIT" or "WITHDRAW"
                                        tx.amount(),
                                        tx.balanceAfter(),
                                        tx.printContent(),
                                        null // editorId는 시스템 자동 동기화이므로 null
                        );
                        TransactionLog savedLog = transactionLogRepository.save(log);
                        savedLogs.add(savedLog);

                        // 새로 저장된 내역 수집 및 매핑
                        savedHistories.add(history);
                        historyToLogMap.put(history.getHistoryId(), savedLog);
                }

                // 5. 자동 매칭 수행 (새로 저장된 거래내역과 입금요청 매칭)
                transactionMatchingService.autoMatchTransactions(clubId, savedHistories, historyToLogMap);

                return savedLogs;
        }

        /**
         * 모임 정산 환급
         * - 모임장/총무가 남은 돈을 회원들에게 돌려주기
         * - 오픈뱅킹 API 출금/이체 요청
         * 
         * @param request 환급 요청
         * @return 환급 결과
         */
        @Transactional
        public RefundResponse refundToMember(RefundRequest request) {
                // 1. clubId로 계좌 조회
                BankAccounts account = bankAccountRepository.findByClubId(request.clubId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "해당 모임의 계좌를 찾을 수 없습니다. clubId: " + request.clubId()));

                // 2. 계좌의 bankCode로 Provider 선택
                String bankCode = account.getBankCode();
                BankProvider provider = registry.get(bankCode);

                // 3. Provider를 통해 오픈뱅킹 API 출금/이체 호출
                RefundResponse response = provider.refund(request);

                if (!response.success()) {
                        throw new RuntimeException("환급 실패: " + response.message());
                }

                // 4. TransactionLog에 저장 (출금 기록)
                // 4-1. 이전 잔액 조회
                var latestLog = transactionLogRepository.findLatestByClubId(request.clubId());
                BigDecimal previousBalance = latestLog.map(TransactionLog::getBalanceAfter).orElse(BigDecimal.ZERO);
                BigDecimal currentBalance = previousBalance.subtract(response.amount());

                TransactionLog log = new TransactionLog(
                                request.clubId(),
                                account.getAccountId(),
                                "WITHDRAW", // 출금
                                response.amount(),
                                currentBalance, // 계산된 잔액
                                "환급: " + response.recipientName() + " - " + request.memo(),
                                null // editorId (자동 처리)
                );
                transactionLogRepository.save(log);

                // 5. BankTransactionHistory에 저장 (은행 거래내역 미러링)
                // Stub 환경이나 실제 환경에서 sync 시 중복 방지 및 누락 방지
                BankTransactionHistory history = new BankTransactionHistory(
                                request.clubId(),
                                LocalDateTime.now(), // 발생 시간
                                response.recipientName(), // 받는 사람 이름
                                response.amount().abs(), // 무조건 양수로 저장
                                response.transferId(), // uniqueTxKey (bank_tran_id)
                                "WITHDRAW" // 출금
                );
                transactionHistoryRepository.save(history);

                return response;
        }
}