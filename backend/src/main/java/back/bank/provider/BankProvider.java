package back.bank.provider;

import back.bank.dto.request.AccountCreateRequest;
import back.bank.dto.request.TransferRequest;
import back.bank.dto.response.AccountCreateResponse;
import back.bank.dto.response.AccountOwnerResponse;
import back.bank.dto.response.BankTransaction;
import back.bank.dto.response.TransferResponse;

import java.time.LocalDate;
import java.util.List;

public interface BankProvider {

    /**
     * 어떤 은행을 담당하는 Provider인지 식별 (예: "KB", "NH", "SHINHAN")
     */
    String bankCode();

    /**
     * 계좌 조회 - 실명/소유주 조회(실계좌 검증)
     * - 오픈뱅킹 연동 전: Stub로 처리
     * - 연동 후: 실제 API 호출
     */
    AccountOwnerResponse inquireAccountOwner(String accountNumber);

    /**
     * 계좌 생성
     * - 지금 당장은 실제 은행 계좌 생성이 어려울 확률이 높아서
     *   "가상계좌 발급" or "계좌 등록(링킹)" 개념으로 남겨두는 용도
     * - 연동 시 은행/PG 정책에 따라 실제 구현
     */
    AccountCreateResponse createAccount(AccountCreateRequest command);

    /**
     * 입/출금 중 '입금' 기능은 필요없음
     * - 서비스 내에서 입금 확인은 "계좌 거래내역 조회"로 처리
     */
    List<BankTransaction> getTransactions(String accountNumber, LocalDate from, LocalDate to);

    /**
     * 송금(계좌이체) 기능
     * - 회원들에게 나눠줘야 하므로 Transfer 필요
     */
    TransferResponse transfer(TransferRequest command);
}