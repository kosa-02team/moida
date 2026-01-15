package back.bank.dto.request;

public record AccountCreateRequest(
                Long userId,
                String bankCode, // Provider 선택을 위한 bankCode 추가
                String accountNumber, // "등록할 계좌번호"로 쓰거나, 가상계좌 발급이면 null 가능
                String ownerName) {

}
