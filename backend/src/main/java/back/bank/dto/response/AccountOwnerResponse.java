package back.bank.dto.response;

public record AccountOwnerResponse(
        boolean success,
        String ownerName,   // 소유주명
        String message
) {}
