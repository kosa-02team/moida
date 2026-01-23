package back.bank.dto.response;

import back.bank.domain.BankAccounts;

public record BankAccountResponseDTO(
        Long accountId,
        Long clubId,
        String bankCode,
        Long userId,
        String accountNumber,
        String depositorName,
        BankResponseDTO bank) {
    public static BankAccountResponseDTO from(BankAccounts account) {
        if (account == null)
            return null;
        return new BankAccountResponseDTO(
                account.getAccountId(),
                account.getClubId(),
                account.getBankCode(),
                account.getUserId(),
                account.getAccountNumber(),
                account.getDepositorName(),
                BankResponseDTO.from(account.getBank()));
    }
}
