package back.bank.dto.response.openbanking;

/**
 * 오픈뱅킹 API 출금/이체 응답
 */
public record OpenBankingWithdrawResponse(
        String api_tran_id, // 거래고유번호(API)
        String api_tran_dtm, // 거래일시(밀리세컨드)
        String rsp_code, // 응답코드(API)
        String rsp_message, // 응답메시지(API)
        String dps_bank_code_std, // 입금기관.표준코드
        String dps_bank_code_sub, // 입금기관.점별코드
        String dps_bank_name, // 입금기관명
        String dps_account_num_masked, // 입금계좌번호(출력용)
        String dps_print_content, // 입금계좌인자내역
        String dps_account_holder_name, // 수취인성명
        String bank_tran_id, // 거래고유번호(참가은행)
        String bank_tran_date, // 거래일자(참가은행)
        String bank_code_tran, // 응답코드를 부여한 참가은행.표준코드
        String bank_rsp_code, // 응답코드(참가은행)
        String bank_rsp_message, // 응답메시지(참가은행)
        String fintech_use_num, // 출금계좌핀테크이용번호
        String account_alias, // 출금계좌별명(Alias)
        String bank_code_std, // 출금(개설)기관.표준코드
        String bank_code_sub, // 출금(개설)기관.점별코드
        String bank_name, // 출금(개설)기관명
        String savings_bank_name, // 개별저축은행명
        String account_num_masked, // 출금계좌번호(출력용)
        String print_content, // 출금계좌인자내역
        String account_holder_name, // 송금인성명
        String tran_amt, // 거래금액
        String wd_limit_remain_amt // 출금한도잔여금액
) {
    /**
     * 응답이 성공인지 확인
     */
    public boolean isSuccess() {
        return "A0000".equals(rsp_code) && "000".equals(bank_rsp_code);
    }
}
