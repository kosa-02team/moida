package back.bank.dto.response;

public record AccountCreateResponse(
        boolean success,
        String accountNumber,
        String message) {

}
