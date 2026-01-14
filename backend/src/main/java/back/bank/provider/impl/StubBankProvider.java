package back.bank.provider.impl;

import back.bank.dto.request.AccountCreateRequest;
import back.bank.dto.request.TransferRequest;
import back.bank.dto.response.AccountCreateResponse;
import back.bank.dto.response.AccountOwnerResponse;
import back.bank.dto.response.BankTransaction;
import back.bank.dto.response.TransferResponse;
import back.bank.provider.BankProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class StubBankProvider implements BankProvider {

    @Override
    public String bankCode() {
        return "STUB";
    }

    @Override
    public AccountOwnerResponse inquireAccountOwner(String accountNumber) {
        // TODO: 추후 오픈뱅킹 실계좌조회 API로 교체
        if (accountNumber == null || accountNumber.isBlank()) {
            return new AccountOwnerResponse(false, null, "accountNumber is empty");
        }
        return new AccountOwnerResponse(true, "홍길동", "stub-owner");
    }

    @Override
    public AccountCreateResponse createAccount(AccountCreateRequest command) {
        // TODO: 추후 가상계좌 발급/계좌등록 API로 교체
        String acc = (command.accountNumber() == null || command.accountNumber().isBlank())
                ? "110-" + (int)(Math.random() * 9000 + 1000) + "-" + (int)(Math.random() * 900000 + 100000)
                : command.accountNumber();

        return new AccountCreateResponse(true, acc, "stub-created");
    }

    @Override
    public List<BankTransaction> getTransactions(String accountNumber, LocalDate from, LocalDate to) {
        // TODO: 추후 거래내역조회 API로 교체
        return List.of(
                new BankTransaction(
                        "TX-" + UUID.randomUUID(),
                        LocalDateTime.now().minusHours(3),
                        "DEPOSIT",
                        new BigDecimal("50000"),
                        new BigDecimal("150000"),
                        "입금 테스트"
                ),
                new BankTransaction(
                        "TX-" + UUID.randomUUID(),
                        LocalDateTime.now().minusHours(1),
                        "WITHDRAW",
                        new BigDecimal("12000"),
                        new BigDecimal("138000"),
                        "출금 테스트"
                )
        );
    }

    @Override
    public TransferResponse transfer(TransferRequest command) {
        // TODO: 추후 이체 API로 교체
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return new TransferResponse(false, null, "amount must be positive");
        }
        return new TransferResponse(true, "TR-" + UUID.randomUUID(), "stub-transfer-ok");
    }
}