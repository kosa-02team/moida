package back.service.ledger;

import back.domain.ledger.PaymentRequest;
import back.dto.ledger.request.PaymentRequestCreateRequest;
import back.repository.ledger.PaymentRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 입금요청 서비스
 * - 입금요청 생성/조회/관리
 */
@Service
public class PaymentRequestService {

    private final PaymentRequestRepository paymentRequestRepository;

    public PaymentRequestService(PaymentRequestRepository paymentRequestRepository) {
        this.paymentRequestRepository = paymentRequestRepository;
    }

    /**
     * 입금요청 생성
     */
    @Transactional
    public List<PaymentRequest> createPaymentRequests(Long clubId, PaymentRequestCreateRequest request) {
        List<PaymentRequest> createdRequests = new ArrayList<>();

        for (var item : request.requests()) {
            // 만료 시간 계산
            LocalDateTime expiresAt = null;
            if (item.expiresInDays() != null && item.expiresInDays() > 0) {
                expiresAt = LocalDateTime.now().plusDays(item.expiresInDays());
            }

            // PaymentRequest 생성
            PaymentRequest paymentRequest = new PaymentRequest(
                    clubId,
                    item.memberId(),
                    item.memberName(),
                    PaymentRequest.RequestType.valueOf(item.requestType()),
                    item.expectedAmount(),
                    item.expectedDate(),
                    item.matchDaysRange(),
                    expiresAt);

            PaymentRequest saved = paymentRequestRepository.save(paymentRequest);
            createdRequests.add(saved);
        }

        return createdRequests;
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

    /**
     * 매칭 가능한 입금요청 조회
     */
    public List<PaymentRequest> getMatchableRequests(Long clubId) {
        return paymentRequestRepository.findMatchableRequests(clubId);
    }
}
