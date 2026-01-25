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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

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

        /**
         * TransactionLog의 실제 거래 날짜를 조회
         * bankHistoryId가 있으면 BankTransactionHistory의 bankTransactionAt을 사용,
         * 없으면 createdAt을 사용
         */
        private LocalDateTime getActualTransactionDate(TransactionLog log) {
                if (log.getBankHistoryId() != null) {
                        Optional<BankTransactionHistory> history = transactionHistoryRepository
                                        .findById(log.getBankHistoryId());
                        if (history.isPresent()) {
                                return history.get().getBankTransactionAt();
                        }
                }
                return log.getCreatedAt();
        }

        private BigDecimal calculateRunningBalance(Long clubId, LocalDateTime syncStartDate, Long firstTransactionId) {
                // 1. 은행 거래 중 syncStartDate/firstTransactionId 이전의 가장 최신 거래 조회
                // firstTransactionId가 없으면(새로 가져오는 경우) Long.MAX_VALUE로 설정하여 해당 시간의 모든 거래 포함
                Long cutoffId = (firstTransactionId != null) ? firstTransactionId : Long.MAX_VALUE;

                List<BankTransactionHistory> prevHistories = transactionHistoryRepository
                                .findPreviousHistory(clubId, syncStartDate, cutoffId,
                                                org.springframework.data.domain.PageRequest.of(0, 1));

                TransactionLog lastBankLog = null;
                LocalDateTime lastBankDate = LocalDateTime.MIN;

                if (!prevHistories.isEmpty()) {
                        BankTransactionHistory lastBankTxHist = prevHistories.get(0);
                        // 해당 히스토리의 TransactionLog 조회
                        Optional<TransactionLog> logOpt = transactionLogRepository
                                        .findByBankHistoryId(lastBankTxHist.getHistoryId());
                        if (logOpt.isPresent()) {
                                lastBankLog = logOpt.get();
                                lastBankDate = lastBankTxHist.getBankTransactionAt();
                        }
                }

                // 2. 수동 거래 중 syncStartDate 이전의 가장 최신 거래 조회
                // (수동 거래는 은행 거래와 ID 체계가 다르므로 시간으로만 단순 비교하되,
                // syncStartDate와 같으면 은행 거래가 우선이라고 가정하거나 포함)
                Optional<TransactionLog> lastManualLog = transactionLogRepository
                                .findFirstByClubIdAndBankHistoryIdIsNullAndCreatedAtBeforeOrderByCreatedAtDescTransactionIdDesc(
                                                clubId, syncStartDate);

                LocalDateTime lastManualDate = LocalDateTime.MIN;
                if (lastManualLog.isPresent()) {
                        lastManualDate = lastManualLog.get().getCreatedAt();
                }

                // 3. 둘 중 더 최신 거래의 잔액을 반환
                if (lastBankLog == null && lastManualLog.isEmpty()) {
                        System.out.println("💰 [잔액 계산] 이전 거래 없음. 0원으로 시작");
                        return BigDecimal.ZERO;
                }

                // 은행 거래가 수동 거래보다 뒤(최신)이거나 같으면 은행 거래 우선
                if (lastBankDate.isAfter(lastManualDate) || lastBankDate.isEqual(lastManualDate)) {
                        // lastBankLog가 null이 아닌 경우에만 여기로 옴 (위에서 체크함)
                        if (lastBankLog != null) {
                                System.out.println("💰 [잔액 계산] 은행 거래 기준 시작 잔액: " + lastBankLog.getBalanceAfter()
                                                + " (Date: " + lastBankDate + ")");
                                return lastBankLog.getBalanceAfter();
                        }
                }

                // 수동 거래가 더 최신인 경우
                if (lastManualLog.isPresent()) {
                        System.out.println("💰 [잔액 계산] 수동 거래 기준 시작 잔액: " + lastManualLog.get().getBalanceAfter()
                                        + " (Date: " + lastManualDate + ")");
                        return lastManualLog.get().getBalanceAfter();
                }

                return BigDecimal.ZERO;
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

                // 거래를 날짜/시간 순으로 정렬 (오래된 것부터) - 잔액 계산을 위해 중요!
                bankTransactions.sort((tx1, tx2) -> tx1.occurredAt().compareTo(tx2.occurredAt()));
                System.out.println("💰 [은행 동기화] 조회된 거래 수: " + bankTransactions.size() + "건 (날짜 순 정렬됨)");

                List<TransactionLog> savedLogs = new ArrayList<>();
                List<BankTransactionHistory> savedHistories = new ArrayList<>();
                Map<Long, TransactionLog> historyToLogMap = new HashMap<>();

                // 이전 잔액 조회 (잔액 계산을 위해)
                // 동기화 대상 거래들의 날짜 범위 확인하여 시작 잔액 계산
                // 이전 잔액 조회 (잔액 계산을 위해)
                // 동기화 대상 거래들의 날짜 범위 확인하여 시작 잔액 계산
                BigDecimal runningBalance;
                if (!bankTransactions.isEmpty()) {
                        LocalDateTime syncStartDate = bankTransactions.get(0).occurredAt();
                        // 첫 번째 거래가 이미 DB에 있는지 확인하여 ID를 가져옴 (Re-Sync 경우)
                        // 하지만 bankTransactions는 API에서 가져온 것이라 ID가 아직 없음(실제 DB ID는 저장 후에 생김).
                        // -> Re-Sync라면 이미 저장된 uniqueTxKey로 DB ID를 찾아야 함.

                        // API에서 가져온 첫 거래의 Unique Key
                        String firstTxKey = bankTransactions.get(0).txId();
                        Optional<BankTransactionHistory> existingFirst = transactionHistoryRepository
                                        .findByUniqueTxKey(firstTxKey);

                        Long firstTxId = existingFirst.map(BankTransactionHistory::getHistoryId).orElse(null);

                        runningBalance = calculateRunningBalance(clubId, syncStartDate, firstTxId);
                } else {
                        Optional<TransactionLog> latestLog = transactionLogRepository.findLatestByClubId(clubId);
                        runningBalance = latestLog.map(TransactionLog::getBalanceAfter).orElse(BigDecimal.ZERO);
                        System.out.println("💰 [은행 동기화] 시작 잔액: " + runningBalance);
                }

                // 4. 각 거래내역을 BankTransactionHistory와 TransactionLog에 저장 (날짜 순으로 처리)
                for (BankTransaction tx : bankTransactions) {
                        // 중복 확인 (uniqueTxKey로)
                        Optional<BankTransactionHistory> existingHistory = transactionHistoryRepository
                                        .findByUniqueTxKey(tx.txId());

                        BankTransactionHistory savedHistory;
                        TransactionLog savedLog;
                        if (existingHistory.isPresent()) {
                                // 이미 저장된 거래내역이 있는 경우
                                savedHistory = existingHistory.get();

                                // TransactionLog가 있는지 확인
                                Optional<TransactionLog> existingLog = transactionLogRepository
                                                .findByBankHistoryId(savedHistory.getHistoryId());

                                if (existingLog.isPresent()) {
                                        // TransactionLog도 이미 존재하면 잔액을 재계산하여 업데이트
                                        System.out.println("  🔄 거래내역과 TransactionLog 모두 이미 존재: " + tx.txId()
                                                        + " (잔액 재계산)");
                                        System.out.println(
                                                        "    → 이전 잔액: " + runningBalance + ", 거래 금액: " + tx.amount());

                                        // 잔액 재계산: 이전 잔액에 현재 거래 금액을 더함 (한 번만 계산)
                                        BigDecimal calculatedBalance = runningBalance.add(tx.amount());
                                        System.out.println("    → 계산된 잔액: " + calculatedBalance);

                                        // 잔액이 다르면 업데이트
                                        TransactionLog log = existingLog.get();
                                        if (log.getBalanceAfter().compareTo(calculatedBalance) != 0) {
                                                System.out.println("    → 잔액 수정: " + log.getBalanceAfter() + " → "
                                                                + calculatedBalance);
                                                // TransactionLog의 잔액을 업데이트
                                                log.updateBalanceAfter(calculatedBalance);
                                                transactionLogRepository.save(log);
                                        } else {
                                                System.out.println("    → 잔액 일치 (수정 불필요)");
                                        }
                                        // runningBalance는 항상 계산된 값으로 업데이트 (저장된 값 신뢰하지 않음)
                                        runningBalance = calculatedBalance;
                                        System.out.println("    → 현재 잔액: " + runningBalance);
                                        continue;
                                } else {
                                        // BankTransactionHistory는 있지만 TransactionLog가 없으면 생성
                                        System.out.println("  🔄 BankTransactionHistory는 존재하지만 TransactionLog가 없어서 생성: "
                                                        + tx.txId());
                                        System.out.println(
                                                        "    → 이전 잔액: " + runningBalance + ", 거래 금액: " + tx.amount());

                                        // 잔액 계산: 이전 잔액에 현재 거래 금액을 더함
                                        runningBalance = runningBalance.add(tx.amount());
                                        System.out.println("    → 계산된 잔액: " + runningBalance);

                                        savedLog = new TransactionLog(
                                                        clubId,
                                                        null, // scheduleId는 매칭 시점에 설정
                                                        account.getAccountId(),
                                                        tx.type(), // "DEPOSIT" or "WITHDRAW"
                                                        tx.amount(),
                                                        runningBalance, // 계산된 잔액 사용
                                                        tx.printContent(),
                                                        null, // editorId는 시스템 자동 동기화이므로 null
                                                        savedHistory.getHistoryId() // bankHistoryId 연결
                                        );
                                        savedLog = transactionLogRepository.save(savedLog);
                                        savedLogs.add(savedLog);
                                        historyToLogMap.put(savedHistory.getHistoryId(), savedLog);

                                        System.out.println("  ✓ 거래 저장: " + tx.printContent() + " (" + tx.amount()
                                                        + "원), 잔액: " + runningBalance);
                                }
                        } else {
                                // 새로운 거래내역인 경우
                                // 4-1. BankTransactionHistory에 저장 (실제 은행 거래내역)
                                savedHistory = new BankTransactionHistory(
                                                clubId,
                                                tx.occurredAt(),
                                                tx.printContent(), // senderName
                                                tx.amount().abs(), // 무조건 양수로 저장
                                                tx.txId(), // uniqueTxKey로 사용
                                                tx.type() // inoutType
                                );
                                savedHistory = transactionHistoryRepository.save(savedHistory);

                                // 4-2. TransactionLog에 저장 (회계 원장) - bankHistoryId 연결
                                System.out.println("  ➕ 새로운 거래 저장: " + tx.printContent());
                                System.out.println("    → 이전 잔액: " + runningBalance + ", 거래 금액: " + tx.amount());

                                // 잔액 계산: 이전 잔액에 현재 거래 금액을 더함
                                runningBalance = runningBalance.add(tx.amount());
                                System.out.println("    → 계산된 잔액: " + runningBalance);

                                savedLog = new TransactionLog(
                                                clubId,
                                                null, // scheduleId는 매칭 시점에 설정
                                                account.getAccountId(),
                                                tx.type(), // "DEPOSIT" or "WITHDRAW"
                                                tx.amount(),
                                                runningBalance, // 계산된 잔액 사용
                                                tx.printContent(),
                                                null, // editorId는 시스템 자동 동기화이므로 null
                                                savedHistory.getHistoryId() // bankHistoryId 연결
                                );
                                savedLog = transactionLogRepository.save(savedLog);
                                savedLogs.add(savedLog);

                                System.out.println("  ✓ 거래 저장: " + tx.printContent() + " (" + tx.amount() + "원), 잔액: "
                                                + runningBalance);

                                // 새로 저장된 내역 수집 및 매핑
                                savedHistories.add(savedHistory);
                                historyToLogMap.put(savedHistory.getHistoryId(), savedLog);
                        }
                }

                // 5. 자동 매칭 수행 (새로 저장된 거래내역과 입금요청 매칭) - 데드락 방지를 위해 트랜잭션 커밋 후 실행
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                                try {
                                        transactionMatchingService.autoMatchTransactions(clubId, savedHistories,
                                                        historyToLogMap);
                                } catch (Exception e) {
                                        System.err.println("자동 매칭 중 오류 발생: " + e.getMessage());
                                        e.printStackTrace();
                                }
                        }
                });

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

                // 거래를 날짜/시간 순으로 정렬 (오래된 것부터) - 잔액 계산을 위해 중요!
                bankTransactions.sort((tx1, tx2) -> tx1.occurredAt().compareTo(tx2.occurredAt()));
                System.out.println("💰 [은행 동기화] 조회된 거래 수: " + bankTransactions.size() + "건 (날짜 순 정렬됨)");

                List<TransactionLog> savedLogs = new ArrayList<>();
                List<BankTransactionHistory> savedHistories = new ArrayList<>();
                Map<Long, TransactionLog> historyToLogMap = new HashMap<>();

                // 이전 잔액 조회 (잔액 계산을 위해)
                // 동기화 대상 거래들의 날짜 범위 확인하여 시작 잔액 계산
                // 이전 잔액 조회 (잔액 계산을 위해)
                // 동기화 대상 거래들의 날짜 범위 확인하여 시작 잔액 계산
                BigDecimal runningBalance;
                if (!bankTransactions.isEmpty()) {
                        LocalDateTime syncStartDate = bankTransactions.get(0).occurredAt();
                        // 첫 번째 거래가 이미 DB에 있는지 확인하여 ID를 가져옴 (Re-Sync 경우)

                        // API에서 가져온 첫 거래의 Unique Key
                        String firstTxKey = bankTransactions.get(0).txId();
                        Optional<BankTransactionHistory> existingFirst = transactionHistoryRepository
                                        .findByUniqueTxKey(firstTxKey);

                        Long firstTxId = existingFirst.map(BankTransactionHistory::getHistoryId).orElse(null);

                        runningBalance = calculateRunningBalance(clubId, syncStartDate, firstTxId);
                } else {
                        Optional<TransactionLog> latestLog = transactionLogRepository.findLatestByClubId(clubId);
                        runningBalance = latestLog.map(TransactionLog::getBalanceAfter).orElse(BigDecimal.ZERO);
                        System.out.println("💰 [은행 동기화] 시작 잔액: " + runningBalance);
                }

                // 4. 각 거래내역을 BankTransactionHistory와 TransactionLog에 저장 (날짜 순으로 처리)
                for (BankTransaction tx : bankTransactions) {
                        // 중복 확인 (uniqueTxKey로)
                        Optional<BankTransactionHistory> existingHistory = transactionHistoryRepository
                                        .findByUniqueTxKey(tx.txId());

                        BankTransactionHistory savedHistory;
                        TransactionLog savedLog;

                        if (existingHistory.isPresent()) {
                                // 이미 저장된 거래내역이 있는 경우
                                savedHistory = existingHistory.get();

                                // TransactionLog가 있는지 확인
                                Optional<TransactionLog> existingLog = transactionLogRepository
                                                .findByBankHistoryId(savedHistory.getHistoryId());

                                if (existingLog.isPresent()) {
                                        // TransactionLog도 이미 존재하면 잔액을 재계산하여 업데이트
                                        System.out.println("  🔄 거래내역과 TransactionLog 모두 이미 존재: " + tx.txId()
                                                        + " (잔액 재계산)");
                                        System.out.println(
                                                        "    → 이전 잔액: " + runningBalance + ", 거래 금액: " + tx.amount());

                                        // 잔액 재계산: 이전 잔액에 변동 금액을 더함 (입/출금 구분)
                                        BigDecimal change = tx.amount();
                                        if ("WITHDRAW".equalsIgnoreCase(tx.type())) {
                                                change = change.abs().negate(); // 출금이면 음수로 변환
                                        } else {
                                                change = change.abs(); // 입금이면 양수
                                        }

                                        BigDecimal calculatedBalance = runningBalance.add(change);
                                        System.out.println("    → 계산된 잔액: " + calculatedBalance);

                                        // 잔액이 다르면 업데이트
                                        TransactionLog log = existingLog.get();
                                        if (log.getBalanceAfter().compareTo(calculatedBalance) != 0) {
                                                System.out.println("    → 잔액 수정: " + log.getBalanceAfter() + " → "
                                                                + calculatedBalance);
                                                // TransactionLog의 잔액을 업데이트
                                                log.updateBalanceAfter(calculatedBalance);
                                                transactionLogRepository.save(log);
                                        } else {
                                                System.out.println("    → 잔액 일치 (수정 불필요)");
                                        }
                                        // runningBalance는 항상 계산된 값으로 업데이트 (저장된 값 신뢰하지 않음)
                                        runningBalance = calculatedBalance;
                                        System.out.println("    → 현재 잔액: " + runningBalance);
                                        continue;
                                } else {
                                        // BankTransactionHistory는 있지만 TransactionLog가 없으면 생성
                                        System.out.println("  🔄 BankTransactionHistory는 존재하지만 TransactionLog가 없어서 생성: "
                                                        + tx.txId());
                                        System.out.println(
                                                        "    → 이전 잔액: " + runningBalance + ", 거래 금액: " + tx.amount());

                                        // 잔액 계산: 이전 잔액에 변동 금액을 더함 (입/출금 구분)
                                        BigDecimal change = tx.amount();
                                        if ("WITHDRAW".equalsIgnoreCase(tx.type())) {
                                                change = change.abs().negate(); // 출금이면 음수로 변환
                                        } else {
                                                change = change.abs(); // 입금이면 양수
                                        }

                                        runningBalance = runningBalance.add(change);
                                        System.out.println("    → 계산된 잔액: " + runningBalance);

                                        savedLog = new TransactionLog(
                                                        clubId,
                                                        null, // scheduleId는 매칭 시점에 설정
                                                        account.getAccountId(),
                                                        tx.type(), // "DEPOSIT" or "WITHDRAW"
                                                        tx.amount(), // 원본 금액 (절대값) 저장
                                                        runningBalance, // 계산된 잔액 사용
                                                        tx.printContent(),
                                                        null, // editorId는 시스템 자동 동기화이므로 null
                                                        savedHistory.getHistoryId() // bankHistoryId 연결
                                        );
                                        savedLog = transactionLogRepository.save(savedLog);
                                        savedLogs.add(savedLog);
                                        historyToLogMap.put(savedHistory.getHistoryId(), savedLog);

                                        System.out.println("  ✓ 거래 저장: " + tx.printContent() + " (" + tx.amount()
                                                        + "원), 잔액: " + runningBalance);
                                }
                        } else {
                                // 새로운 거래내역인 경우
                                // 4-1. BankTransactionHistory에 저장 (실제 은행 거래내역)
                                savedHistory = new BankTransactionHistory(
                                                clubId,
                                                tx.occurredAt(),
                                                tx.printContent(), // senderName
                                                tx.amount().abs(), // 무조건 양수로 저장
                                                tx.txId(), // uniqueTxKey로 사용
                                                tx.type() // inoutType
                                );
                                savedHistory = transactionHistoryRepository.save(savedHistory);

                                // 4-2. TransactionLog에 저장 (회계 원장) - bankHistoryId 연결
                                System.out.println("  ➕ 새로운 거래 저장: " + tx.printContent());
                                System.out.println("    → 이전 잔액: " + runningBalance + ", 거래 금액: " + tx.amount());

                                // 잔액 계산: 이전 잔액에 변동 금액을 더함 (입/출금 구분)
                                BigDecimal change = tx.amount();
                                if ("WITHDRAW".equalsIgnoreCase(tx.type())) {
                                        change = change.abs().negate(); // 출금이면 음수로 변환
                                } else {
                                        change = change.abs(); // 입금이면 양수
                                }

                                runningBalance = runningBalance.add(change);
                                System.out.println("    → 계산된 잔액: " + runningBalance);

                                savedLog = new TransactionLog(
                                                clubId,
                                                null, // scheduleId는 매칭 시점에 설정
                                                account.getAccountId(),
                                                tx.type(), // "DEPOSIT" or "WITHDRAW"
                                                tx.amount(),
                                                runningBalance, // 계산된 잔액 사용
                                                tx.printContent(),
                                                null, // editorId는 시스템 자동 동기화이므로 null
                                                savedHistory.getHistoryId() // bankHistoryId 연결
                                );
                                savedLog = transactionLogRepository.save(savedLog);
                                savedLogs.add(savedLog);

                                System.out.println("  ✓ 거래 저장: " + tx.printContent() + " (" + tx.amount() + "원), 잔액: "
                                                + runningBalance);

                                // 새로 저장된 내역 수집 및 매핑
                                savedHistories.add(savedHistory);
                                historyToLogMap.put(savedHistory.getHistoryId(), savedLog);
                        }
                }

                // 5. 자동 매칭 수행 (새로 저장된 거래내역과 입금요청 매칭) - 데드락 방지를 위해 트랜잭션 커밋 후 실행
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                                try {
                                        transactionMatchingService.autoMatchTransactions(clubId, savedHistories,
                                                        historyToLogMap);
                                } catch (Exception e) {
                                        System.err.println("자동 매칭(Stub) 중 오류 발생: " + e.getMessage());
                                        e.printStackTrace();
                                }
                        }
                });

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

                // 4. BankTransactionHistory에 저장 (은행 거래내역 미러링)
                BankTransactionHistory history = new BankTransactionHistory(
                                request.clubId(),
                                LocalDateTime.now(), // 발생 시간
                                response.recipientName(), // 받는 사람 이름
                                response.amount().abs(), // 무조건 양수로 저장
                                response.transferId(), // uniqueTxKey (bank_tran_id)
                                "WITHDRAW" // 출금
                );
                history = transactionHistoryRepository.save(history);

                // 5. TransactionLog에 저장 (출금 기록) - historyId 연결
                // 5-1. 이전 잔액 조회
                var latestLog = transactionLogRepository.findLatestByClubId(request.clubId());
                BigDecimal previousBalance = latestLog.map(TransactionLog::getBalanceAfter).orElse(BigDecimal.ZERO);
                BigDecimal currentBalance = previousBalance.subtract(response.amount());

                TransactionLog log = new TransactionLog(
                                request.clubId(),
                                null, // accountId (or get from account)
                                account.getAccountId(), // accountId 추가
                                "WITHDRAW", // 출금
                                response.amount(),
                                currentBalance, // 계산된 잔액
                                "환급: " + response.recipientName() + " - " + request.memo(),
                                null, // editorId (자동 처리)
                                history.getHistoryId() // bankHistoryId 연결
                );
                transactionLogRepository.save(log);

                return response;
        }
}