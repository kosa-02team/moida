package back.bank.service;

import back.bank.dto.request.TransferRequest;
import back.bank.dto.response.AccountOwnerResponse;
import back.bank.dto.response.TransferResponse;
import back.bank.provider.BankProviderRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class BankService {

    private final BankProviderRegistry registry;

    public BankService(BankProviderRegistry registry) {
        this.registry = registry;
    }

    public AccountOwnerResponse checkOwner(String bankCode, String accountNumber) {
        return registry.get(bankCode).inquireAccountOwner(accountNumber);
    }

    public boolean confirmDepositByAmount(
            String bankCode,
            String accountNumber,
            BigDecimal expectedAmount,
            LocalDate from,
            LocalDate to
    ) {
        // "입금 기능"이 아니라 "입금 확인"만: 거래내역에서 DEPOSIT + 금액 매칭
        return registry.get(bankCode).getTransactions(accountNumber, from, to).stream()
                .anyMatch(tx ->
                        "DEPOSIT".equalsIgnoreCase(tx.type())
                                && tx.amount().compareTo(expectedAmount) == 0
                );
    }

    public TransferResponse sendMoney(String fromBankCode, TransferRequest command) {
        return registry.get(fromBankCode).transfer(command);
    }
}