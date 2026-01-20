package back.service.ledger;

import back.domain.ledger.PaymentRequest;
import back.dto.ledger.request.PaymentRequestCreateRequest;
import back.repository.ledger.PaymentRequestRepository;
import back.exception.ResourceException;
import back.exception.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentRequestService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final TransactionMatchingService transactionMatchingService;

    /**
     * [기본] 입금요청 수동 생성
     * - 운영비 회비 요청 등을 수동으로 만들 때 사용
     */
    @Transactional
    public List<PaymentRequest> createPaymentRequests(Long clubId, PaymentRequestCreateRequest request) {
        List<PaymentRequest> createdRequests = new ArrayList<>();

        for (var item : request.requests()) {
            LocalDateTime expiresAt = null;
            if (item.expiresInDays() != null && item.expiresInDays() > 0) {
                expiresAt = LocalDateTime.now().plusDays(item.expiresInDays());
            }

            // Entity 생성자 변경 사항 반영 (scheduleId, billingPeriod)
            PaymentRequest paymentRequest = new PaymentRequest(
                    clubId,
                    item.memberId(),
                    item.memberName(),
                    PaymentRequest.RequestType.valueOf(item.requestType()),
                    item.expectedAmount(),
                    item.expectedDate(),
                    item.matchDaysRange(),
                    expiresAt,
                    item.scheduleId(), // 추가됨
                    item.billingPeriod() // 추가됨
            );

            createdRequests.add(paymentRequestRepository.save(paymentRequest));
        }

        return createdRequests;
    }

    /**
     * 입금 확인 처리 (수동)
     * - 관리자가 "입금 확인됨" 버튼을 눌렀을 때
     */
    @Transactional
    public void confirmPayment(Long requestId, Long adminId) {
        PaymentRequest request = paymentRequestRepository.findById(requestId)
                .orElseThrow(ResourceException.NotFound::new); // ErrorCode 확인 필요

        // 매칭 이력 ID 등은 null로 처리하거나 별도 생성 필요
        request.confirmMatch(null, adminId);
    }

    @Transactional
    public void confirmManualPayment(Long requestId, Long matchedBy) {
        transactionMatchingService.confirmPaymentWithoutHistory(requestId, matchedBy);
    }

    /**
     * 특정 모임의 모든 입금요청 조회
     */
    public List<PaymentRequest> getPaymentRequests(Long clubId) {
        return paymentRequestRepository.findByClubIdOrderByCreatedAtDesc(clubId);
    }

    /**
     * 특정 상태의 입금요청 조회
     */
    public List<PaymentRequest> getPaymentRequestsByStatus(Long clubId, PaymentRequest.RequestStatus status) {
        return paymentRequestRepository.findByClubIdAndStatus(clubId, status);
    }
}