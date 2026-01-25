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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final TransactionLogRepository transactionLogRepository;
    private final BankTransactionHistoryRepository bankTransactionHistoryRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final ClubMemberRepository clubMemberRepository;

    // 조회 (최신순 정렬: createdAt DESC, transactionId DESC)
    @Transactional(readOnly = true)
    public List<TransactionLogResponse> getTransactions(Long clubId, LocalDate startDate, LocalDate endDate, Long scheduleId) {
        System.out.println("📊 [거래 내역 조회] clubId=" + clubId + ", startDate=" + startDate + ", endDate=" + endDate + ", scheduleId=" + scheduleId);
        
        List<TransactionLog> logs;
        if (scheduleId != null) {
            logs = transactionLogRepository.findByClubIdAndScheduleIdOrderByCreatedAtDescTransactionIdDesc(clubId, scheduleId);
            System.out.println("  → 일정별 조회: " + logs.size() + "건");
        } else {
            // 넓은 범위로 조회 (동기화 지연 고려)
            LocalDateTime queryStart = startDate.atStartOfDay().minusDays(7);
            LocalDateTime queryEnd = endDate.atTime(23, 59, 59).plusDays(7);
            
            logs = transactionLogRepository.findByClubIdAndCreatedAtBetweenOrderByCreatedAtDescTransactionIdDesc(clubId, queryStart, queryEnd);
            System.out.println("  → 날짜 범위 조회 (전체): " + logs.size() + "건 (조회 범위: " + queryStart + " ~ " + queryEnd + ")");
            
            // 실제 거래 날짜 기준으로 필터링
            List<TransactionLog> filteredLogs = new ArrayList<>();
            LocalDateTime filterStart = startDate.atStartOfDay();
            LocalDateTime filterEnd = endDate.atTime(23, 59, 59);
            
            for (TransactionLog log : logs) {
                LocalDateTime actualTransactionDate = getActualTransactionDate(log);
                
                // 실제 거래 날짜가 필터 기간 내에 있는지 확인
                if (!actualTransactionDate.isBefore(filterStart) && !actualTransactionDate.isAfter(filterEnd)) {
                    filteredLogs.add(log);
                }
            }
            
            logs = filteredLogs;
            System.out.println("  → 실제 거래 날짜 기준 필터링 후: " + logs.size() + "건 (필터 범위: " + filterStart + " ~ " + filterEnd + ")");
        }
        
        // 1. 모든 로그의 날짜 정보를 미리 조회 (Pre-fetch) to Avoid N+1 & ensure consistency
        Map<Long, LocalDateTime> logIdToDateMap = new java.util.HashMap<>();
        
        // bankHistoryId 수집
        List<Long> allHistoryIds = logs.stream()
                .map(TransactionLog::getBankHistoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        // 한 번에 조회
        Map<Long, LocalDateTime> historyDateMap = bankTransactionHistoryRepository.findAllById(allHistoryIds).stream()
                .collect(Collectors.toMap(
                        BankTransactionHistory::getHistoryId,
                        BankTransactionHistory::getBankTransactionAt
                ));
        
        // Map 구축 (TransactionId -> ActualDate)
        for (TransactionLog log : logs) {
            LocalDateTime date = log.getCreatedAt(); // 기본값
            if (log.getBankHistoryId() != null && historyDateMap.containsKey(log.getBankHistoryId())) {
                date = historyDateMap.get(log.getBankHistoryId());
            }
            logIdToDateMap.put(log.getTransactionId(), date);
        }

        // 실제 거래 날짜 기준 정렬 (최신순) - 모든 케이스에 적용
        Collections.sort(logs, (log1, log2) -> {
            LocalDateTime date1 = logIdToDateMap.get(log1.getTransactionId());
            LocalDateTime date2 = logIdToDateMap.get(log2.getTransactionId());
            
            // 내림차순: date2.compareTo(date1)
            int dateCompare = date2.compareTo(date1);
            if (dateCompare != 0) {
                return dateCompare;
            }
            
            // 날짜가 같으면 ID 내림차순 (최신순)
            return Long.compare(log2.getTransactionId(), log1.getTransactionId());
        });
        
        System.out.println("  → 실제 거래 날짜 기준 정렬 완료 (최신순: 날짜 DESC, ID DESC)");
        
        // bankHistoryId가 있는 로그들의 매칭 정보 조회
        // 매칭된 PaymentRequest 조회
        Map<Long, String> matchedMemberNames = paymentRequestRepository
                .findByClubIdAndMatchedHistoryIdIn(clubId, allHistoryIds)
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
                        (log.getDescription() != null ? log.getDescription() : "") + 
                        " [Act:" + logIdToDateMap.get(log.getTransactionId()).toString() + 
                        " / ID:" + log.getTransactionId() + "]", // 디버깅용: ActualTime, ID 표시
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
    
    /**
     * TransactionLog의 실제 거래 날짜를 조회
     * bankHistoryId가 있으면 BankTransactionHistory의 bankTransactionAt을 사용,
     * 없으면 createdAt을 사용
     */
    private LocalDateTime getActualTransactionDate(TransactionLog log) {
        if (log.getBankHistoryId() != null) {
            // bankHistoryId가 있으면 실제 거래 날짜 조회
            Optional<BankTransactionHistory> history = bankTransactionHistoryRepository.findById(log.getBankHistoryId());
            if (history.isPresent()) {
                return history.get().getBankTransactionAt();
            }
        }
        // BankTransactionHistory를 찾을 수 없거나 bankHistoryId가 없으면 createdAt 사용
        return log.getCreatedAt();
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
        if (req.memo() != null)
            log.updateDescription(req.memo());
    }
}