package back.bank.dto.response;

import back.bank.domain.Banks;

public record BankResponseDTO(
        Long bankId,
        String bankCode,
        String bankName) {
    public static BankResponseDTO from(Banks bank) {
        if (bank == null)
            return null;
        return new BankResponseDTO(
                bank.getBankId(),
                bank.getBankCode(),
                bank.getBankName());
    }
}
