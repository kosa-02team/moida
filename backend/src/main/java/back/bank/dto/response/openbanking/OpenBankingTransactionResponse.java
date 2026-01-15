package back.bank.dto.response.openbanking;

/**
 * 오픈뱅킹 API 거래내역 조회 응답
 */
public record OpenBankingTransactionResponse(
        String api_tran_id, // 거래고유번호(API)
        String api_tran_dtm, // 거래일시(밀리세컨드)
        String rsp_code, // 응답코드(API)
        String rsp_message, // 응답메시지(API)
        String bank_tran_id, // 거래고유번호(참가기관)
        String bank_tran_date, // 거래일자(참가기관)
        String bank_code_tran, // 응답코드를 부여한 참가기관 표준코드
        String bank_rsp_code, // 응답코드(참가기관)
        String bank_rsp_message, // 응답메시지(참가기관)
        String bank_name, // 개설기관명
        String savings_bank_name, // 개별저축은행명
        String fintech_use_num, // 핀테크이용번호
        String balance_amt, // 계좌잔액
        String page_record_cnt, // 현재페이지 레코드 건수
        String next_page_yn, // 다음 페이지 존재여부
        java.util.List<TransactionItem> res_list // 조회된 거래내역
) {
    /**
     * 거래내역 개별 항목
     */
    public record TransactionItem(
            String tran_date, // 거래일자 (YYYYMMDD)
            String tran_time, // 거래시간 (HHMMSS)
            String inout_type, // 입출금구분 ("입금", "출금")
            String tran_type, // 거래구분 ("현금", "이체" 등)
            String print_content, // 통장인자내용
            String tran_amt, // 거래금액
            String after_balance_amt // 거래 후 잔액
    ) {
    }
}
