package back.bank.dto.response;

public record TransferResponse(
        boolean success,
        String transferId,
        String message
) {
}
