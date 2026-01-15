package back.bank.dto.request.openbanking;

/**
 * 오픈뱅킹 API 출금/이체 요청
 * POST https://openapi.openbanking.or.kr/v2.0/transfer/withdraw/fin_num
 */
public record OpenBankingWithdrawRequest(
        String bank_tran_id, // 은행거래고유번호 (필수)
        String cntr_account_type, // 약정 계좌/계정 구분 N:계좌, C:계정 (필수)
        String cntr_account_num, // 약정 계좌/계정 번호 (필수)
        String dps_print_content, // 입금계좌인자내역 (필수)
        String fintech_use_num, // 출금계좌핀테크이용번호 (필수)
        String wd_print_content, // 출금계좌인자내역 (선택)
        String tran_amt, // 거래금액 (필수)
        String tran_dtime, // 요청일시 (필수) - yyyyMMddHHmmss
        String req_client_name, // 요청고객성명 (필수)
        String req_client_bank_code, // 요청고객계좌 개설기관.표준코드 (선택)
        String req_client_account_num, // 요청고객계좌번호 (선택)
        String req_client_fintech_use_num, // 요청고객핀테크이용번호 (선택)
        String req_client_num, // 요청고객회원번호 (필수)
        String transfer_purpose, // 이체용도 (필수) - TR
        String sub_frnc_name, // 하위가맹점명 (선택)
        String sub_frnc_num, // 하위가맹점번호 (선택)
        String sub_frnc_business_num, // 하위가맹점 사업자등록번호 (선택)
        String recv_client_name, // 최종수취고객성명 (선택)
        String recv_client_bank_code, // 최종수취고객계좌 개설기관.표준코드 (선택)
        String recv_client_account_num // 최종수취고객계좌번호 (선택)
) {
}
