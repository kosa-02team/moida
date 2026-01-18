package back.bank.provider.stub;

import back.bank.dto.request.AccountCreateRequest;
import back.dto.ledger.request.RefundRequest;
import back.bank.dto.request.TransferRequest;
import back.bank.dto.request.openbanking.OpenBankingWithdrawRequest;
import back.bank.dto.response.AccountCreateResponse;
import back.bank.dto.response.AccountOwnerResponse;
import back.bank.dto.response.BankTransaction;
import back.dto.ledger.response.RefundResponse;
import back.bank.dto.response.TransferResponse;
import back.bank.dto.response.openbanking.OpenBankingTransactionResponse;
import back.bank.dto.response.openbanking.OpenBankingWithdrawResponse;
import back.bank.provider.BankProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Profile("dev")
@Component
public class StubBankProvider implements BankProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String bankCode() {
        return "STUB";
    }

    @Override
    public AccountOwnerResponse inquireAccountOwner(String accountNumber) {
        // TODO: 추후 오픈뱅킹 실계좌조회 API로 교체
        if (accountNumber == null || accountNumber.isBlank()) {
            return new AccountOwnerResponse(false, null, "accountNumber is empty");
        }
        return new AccountOwnerResponse(true, "홍길동", "stub-owner");
    }

    @Override
    public AccountCreateResponse createAccount(AccountCreateRequest command) {
        // TODO: 추후 가상계좌 발급/계좌등록 API로 교체
        String acc = (command.accountNumber() == null || command.accountNumber().isBlank())
                ? "110-" + (int) (Math.random() * 9000 + 1000) + "-" + (int) (Math.random() * 900000 + 100000)
                : command.accountNumber();

        return new AccountCreateResponse(true, acc, "stub-created");
    }

    @Override
    public List<BankTransaction> getTransactions(String accountNumber, LocalDate from, LocalDate to) {
        // resources/bank/stub/transactions_page1.json 파일에서 Mock 데이터 읽기
        try {
            ClassPathResource resource = new ClassPathResource("bank/stub/transactions_page1.json");
            OpenBankingTransactionResponse response = objectMapper.readValue(
                    resource.getInputStream(),
                    OpenBankingTransactionResponse.class);

            // 오픈뱅킹 API 응답을 BankTransaction으로 변환
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

            return response.res_list().stream()
                    .map(item -> {
                        // 날짜/시간 파싱
                        LocalDate tranDate = LocalDate.parse(item.tran_date(), dateFormatter);
                        LocalDateTime tranDateTime = LocalDateTime.of(
                                tranDate,
                                java.time.LocalTime.parse(item.tran_time(), timeFormatter));

                        // 거래 타입 변환 ("입금" -> "DEPOSIT", "출금" -> "WITHDRAW")
                        String type = item.inout_type().equals("입금") ? "DEPOSIT" : "WITHDRAW";

                        // Deterministic ID 생성
                        String uniqueKeySource = item.tran_date() + item.tran_time() + item.inout_type()
                                + item.tran_amt() + item.print_content();
                        String deterministicId = UUID.nameUUIDFromBytes(uniqueKeySource.getBytes()).toString();

                        return new BankTransaction(
                                deterministicId, // txId - 고유 ID 생성 (Deterministic)
                                tranDateTime,
                                type,
                                new BigDecimal(item.tran_amt()),
                                new BigDecimal(item.after_balance_amt()),
                                item.print_content());
                    })
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("Failed to read stub transaction data", e);
        }
    }

    @Override
    public List<BankTransaction> getTransactionsStub(String accountNumber, Long stubId, LocalDate from, LocalDate to) {
        // stubId를 이용해 파일명 동적 생성 (예: stubId가 1이면 transactions_page1.json)
        // stubId가 null일 경우 기본값(예: 1)을 설정하는 안전장치를 추가하는 것도 좋습니다.
        long pageId = (stubId != null) ? stubId : 1L;
        String filePath = String.format("bank/stub/transactions_page%d.json", pageId);

        try {
            ClassPathResource resource = new ClassPathResource(filePath);

            // 파일이 존재하는지 확인 (선택 사항)
            if (!resource.exists()) {
                throw new RuntimeException("Stub file not found: " + filePath);
            }

            OpenBankingTransactionResponse response = objectMapper.readValue(
                    resource.getInputStream(),
                    OpenBankingTransactionResponse.class);

            // 오픈뱅킹 API 응답을 BankTransaction으로 변환
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

            return response.res_list().stream()
                    .map(item -> {
                        // 날짜/시간 파싱
                        LocalDate tranDate = LocalDate.parse(item.tran_date(), dateFormatter);
                        LocalDateTime tranDateTime = LocalDateTime.of(
                                tranDate,
                                java.time.LocalTime.parse(item.tran_time(), timeFormatter));

                        // 거래 타입 변환 ("입금" -> "DEPOSIT", "출금" -> "WITHDRAW")
                        String type = "입금".equals(item.inout_type()) ? "DEPOSIT" : "WITHDRAW";

                        // Deterministic ID 생성
                        String uniqueKeySource = item.tran_date() + item.tran_time() + item.inout_type()
                                + item.tran_amt() + item.print_content();
                        String deterministicId = UUID.nameUUIDFromBytes(uniqueKeySource.getBytes()).toString();

                        return new BankTransaction(
                                deterministicId, // txId - 고유 ID 생성 (Deterministic)
                                tranDateTime,
                                type,
                                new BigDecimal(item.tran_amt()),
                                new BigDecimal(item.after_balance_amt()),
                                item.print_content());
                    })
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("Failed to read stub transaction data from " + filePath, e);
        }
    }

    @Override
    public TransferResponse transfer(TransferRequest command) {
        // TODO: 추후 이체 API로 교체
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return new TransferResponse(false, null, "amount must be positive");
        }
        return new TransferResponse(true, "TR-" + UUID.randomUUID(), "stub-transfer-ok");
    }

    @Override
    public RefundResponse refund(RefundRequest command) {
        // Mock 오픈뱅킹 API 출금/이체 처리

        // 1. RefundRequest → OpenBankingWithdrawRequest 변환
        OpenBankingWithdrawRequest openBankingRequest = convertToOpenBankingRequest(command);

        // 2. Mock 오픈뱅킹 API 응답 생성
        OpenBankingWithdrawResponse openBankingResponse = mockOpenBankingWithdraw(openBankingRequest);

        // 3. OpenBankingWithdrawResponse → RefundResponse 변환
        return convertToRefundResponse(openBankingResponse, command);
    }

    /**
     * RefundRequest를 오픈뱅킹 API 형식으로 변환
     */
    private OpenBankingWithdrawRequest convertToOpenBankingRequest(RefundRequest command) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String tranDtime = LocalDateTime.now().format(formatter);

        return new OpenBankingWithdrawRequest(
                "F" + UUID.randomUUID().toString().replace("-", "").substring(0, 19), // bank_tran_id
                "N", // cntr_account_type (N: 계좌)
                command.recipientAccountNum(), // cntr_account_num (받는 사람 계좌)
                command.memo(), // dps_print_content (입금계좌인자내역)
                "123456789012345678901234", // fintech_use_num (출금계좌 핀테크이용번호 - 모임 계좌)
                "모임정산환급", // wd_print_content (출금계좌인자내역)
                command.amount().toString(), // tran_amt
                tranDtime, // tran_dtime
                "모임장", // req_client_name (요청고객성명)
                null, // req_client_bank_code
                null, // req_client_account_num
                null, // req_client_fintech_use_num
                "CLUB" + command.clubId(), // req_client_num (요청고객회원번호)
                "TR", // transfer_purpose (이체용도)
                null, // sub_frnc_name
                null, // sub_frnc_num
                null, // sub_frnc_business_num
                command.recipientName(), // recv_client_name
                command.recipientBankCode(), // recv_client_bank_code
                command.recipientAccountNum() // recv_client_account_num
        );
    }

    /**
     * Mock 오픈뱅킹 API 출금/이체 응답 생성
     */
    private OpenBankingWithdrawResponse mockOpenBankingWithdraw(OpenBankingWithdrawRequest request) {
        return new OpenBankingWithdrawResponse(
                UUID.randomUUID().toString(), // api_tran_id
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")), // api_tran_dtm
                "A0000", // rsp_code (성공)
                "정상처리 되었습니다", // rsp_message
                "097", // dps_bank_code_std
                "1230001", // dps_bank_code_sub
                "오픈은행", // dps_bank_name
                maskAccountNumber(request.cntr_account_num()), // dps_account_num_masked
                request.dps_print_content(), // dps_print_content
                "수취인", // dps_account_holder_name
                request.bank_tran_id(), // bank_tran_id
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), // bank_tran_date
                "097", // bank_code_tran
                "000", // bank_rsp_code (성공)
                "", // bank_rsp_message
                request.fintech_use_num(), // fintech_use_num
                "모임계좌", // account_alias
                "097", // bank_code_std
                "1230001", // bank_code_sub
                "오픈은행", // bank_name
                "", // savings_bank_name
                "110-1234-***", // account_num_masked
                request.wd_print_content(), // print_content
                "모임장", // account_holder_name
                request.tran_amt(), // tran_amt
                "9990000" // wd_limit_remain_amt
        );
    }

    /**
     * OpenBankingWithdrawResponse를 RefundResponse로 변환
     */
    private RefundResponse convertToRefundResponse(OpenBankingWithdrawResponse openBankingResponse,
            RefundRequest command) {
        if (!openBankingResponse.isSuccess()) {
            return new RefundResponse(
                    false,
                    null,
                    "환급 실패: " + openBankingResponse.rsp_message(),
                    command.amount(),
                    command.recipientName(),
                    null);
        }

        return new RefundResponse(
                true,
                openBankingResponse.bank_tran_id(),
                "환급 완료",
                new BigDecimal(openBankingResponse.tran_amt()),
                command.recipientName(),
                openBankingResponse.dps_account_num_masked());
    }

    /**
     * 계좌번호 마스킹 (예: 110-1234-567890 → 110-1234-***)
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "***";
        }
        String[] parts = accountNumber.split("-");
        if (parts.length >= 3) {
            return parts[0] + "-" + parts[1] + "-***";
        }
        return accountNumber.substring(0, accountNumber.length() - 3) + "***";
    }
}