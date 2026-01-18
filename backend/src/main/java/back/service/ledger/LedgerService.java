package back.service.ledger;

import back.controller.ledger.ManualTransactionRequest;
import back.controller.ledger.TransactionUpdateRequest;
import back.domain.ledger.TransactionLog;
import back.repository.ledger.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final TransactionLogRepository transactionLogRepository;

    // 조회
    @Transactional(readOnly = true)
    public List<TransactionLog> getTransactions(Long clubId, LocalDate startDate, LocalDate endDate, Long scheduleId) {
        // Repository에 메서드 추가 필요: findByClubIdAndDateBetween...
        if (scheduleId != null) {
            return transactionLogRepository.findByClubIdAndScheduleId(clubId, scheduleId);
        }
        return transactionLogRepository.findByClubIdAndCreatedAtBetween(clubId, startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59));
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