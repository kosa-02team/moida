package back.service.ledger;

import back.bank.domain.BankTransactionHistory;
import back.bank.repository.BankTransactionHistoryRepository;
import back.controller.ledger.ManualTransactionRequest;
import back.controller.ledger.TransactionLogResponse;
import back.controller.ledger.TransactionUpdateRequest;
import back.domain.ledger.PaymentRequest;
import back.domain.ledger.TransactionLog;
import back.repository.club.ClubMemberRepository;
import back.repository.ledger.PaymentRequestRepository;
import back.repository.ledger.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final TransactionLogRepository transactionLogRepository;
    private final BankTransactionHistoryRepository bankTransactionHistoryRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final ClubMemberRepository clubMemberRepository;

    // 조회
    @Transactional(readOnly = true)
    public List<TransactionLogResponse> getTransactions(Long clubId, LocalDate startDate, LocalDate endDate, Long scheduleId) {
        List<TransactionLog> logs;
        if (scheduleId != null) {
            logs = transactionLogRepository.findByClubIdAndScheduleId(clubId, scheduleId);
        } else {
            logs = transactionLogRepository.findByClubIdAndCreatedAtBetween(clubId, startDate.atStartOfDay(),
                    endDate.atTime(23, 59, 59));
        }
        
        // bankHistoryId가 있는 로그들의 매칭 정보 조회
        List<Long> bankHistoryIds = logs.stream()
                .map(TransactionLog::getBankHistoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        // 매칭된 PaymentRequest 조회
        Map<Long, String> matchedMemberNames = paymentRequestRepository
                .findByClubIdAndMatchedHistoryIdIn(clubId, bankHistoryIds)
                .stream()
                .collect(Collectors.toMap(
                        PaymentRequest::getMatchedHistoryId,
                        req -> getMemberName(req.getClubId(), req.getMemberId()),
                        (existing, replacement) -> existing // 중복 시 첫 번째 값 유지
                ));
        
        // DTO 변환
        return logs.stream()
                .map(log -> new TransactionLogResponse(
                        log.getTransactionId(),
                        log.getClubId(),
                        log.getScheduleId(),
                        log.getAccountId(),
                        log.getType(),
                        log.getAmount(),
                        log.getBalanceAfter(),
                        log.getDescription(),
                        log.getEditorId(),
                        log.getCreatedAt(),
                        log.getBankHistoryId(),
                        log.getBankHistoryId() != null ? matchedMemberNames.get(log.getBankHistoryId()) : null
                ))
                .collect(Collectors.toList());
    }
    
    private String getMemberName(Long clubId, Long memberId) {
        return clubMemberRepository.findNameView(clubId, memberId)
                .map(view -> view.getRealName())
                .orElse("알 수 없음");
    }

    // 수동 생성 (현금 지출 등)
    @Transactional
    public void createManualTransaction(Long clubId, ManualTransactionRequest req, Long editorId) {
        // 이전 잔액 조회
        var latestLog = transactionLogRepository.findLatestByClubId(clubId);
        java.math.BigDecimal previousBalance = latestLog.map(TransactionLog::getBalanceAfter)
                .orElse(java.math.BigDecimal.ZERO);
        java.math.BigDecimal currentBalance = previousBalance.add(req.amount());

        TransactionLog log = new TransactionLog(
                clubId,
                null, // scheduleId
                null, // accountId
                req.type(),
                req.amount(),
                currentBalance,
                req.content(),
                editorId);
        transactionLogRepository.save(log);
    }

    // 수정
    @Transactional
    public void updateTransaction(Long transactionId, TransactionUpdateRequest req) {
        TransactionLog log = transactionLogRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("내역이 없습니다."));

        // 메모나 일정 매핑 수정
        // 메모나 일정 매핑 수정
        if (req.memo() != null)
            log.updateDescription(req.memo());
        // if (req.scheduleId() != null) log.updateScheduleId(req.scheduleId()); //
        // TransactionUpdateRequest typically only has memo based on my creation?
        // Wait, I created TransactionUpdateRequest with only 'memo'.
        // So I should only update memo.
    }
}