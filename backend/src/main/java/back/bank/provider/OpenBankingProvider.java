package back.bank.provider;

import back.bank.dto.request.AccountCreateRequest;
import back.dto.ledger.request.RefundRequest;
import back.bank.dto.request.TransferRequest;
import back.bank.dto.response.AccountCreateResponse;
import back.bank.dto.response.AccountOwnerResponse;
import back.bank.dto.response.BankTransaction;
import back.dto.ledger.response.RefundResponse;
import back.bank.dto.response.TransferResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Profile("prod")
@Component
public class OpenBankingProvider implements BankProvider {

    @Override
    public String bankCode() {
        // 실제로는 은행별 provider를 만들거나(예: KBProvider, NHProvider),
        // OpenBankingProvider 하나로 bankCode를 받아 처리해도 됩니다.
        return "OPEN_BANKING";
    }

    @Override
    public AccountOwnerResponse inquireAccountOwner(String accountNumber) {
        // TODO: RestClient/WebClient로 오픈뱅킹 API 호출
        throw new UnsupportedOperationException("OpenBanking 연동 전입니다.");
    }

    @Override
    public AccountCreateResponse createAccount(AccountCreateRequest command) {
        // TODO: 가상계좌 발급/계좌등록 API 호출
        throw new UnsupportedOperationException("OpenBanking 연동 전입니다.");
    }

    @Override
    public List<BankTransaction> getTransactions(String accountNumber, LocalDate from, LocalDate to) {
        // TODO: 거래내역조회 API 호출
        throw new UnsupportedOperationException("OpenBanking 연동 전입니다.");
    }

    @Override
    public TransferResponse transfer(TransferRequest command) {
        // TODO: 이체 API 호출
        throw new UnsupportedOperationException("OpenBanking 연동 전입니다.");
    }

    @Override
    public RefundResponse refund(RefundRequest command) {
        // TODO: 오픈뱅킹 API 출금/이체 호출
        // POST https://openapi.openbanking.or.kr/v2.0/transfer/withdraw/fin_num
        throw new UnsupportedOperationException("OpenBanking 연동 전입니다.");
    }
}
