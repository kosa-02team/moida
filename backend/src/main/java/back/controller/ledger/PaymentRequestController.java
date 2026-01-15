package back.controller.ledger;

import back.domain.ledger.PaymentRequest;
import back.dto.ledger.request.PaymentRequestCreateRequest;
import back.service.ledger.PaymentRequestService;
import back.service.ledger.TransactionMatchingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 입금요청 관리 Controller
 */
@RestController
@RequestMapping("/clubs/{clubId}/payment-requests")
public class PaymentRequestController {

    private final PaymentRequestService paymentRequestService;
    private final TransactionMatchingService transactionMatchingService;

    public PaymentRequestController(PaymentRequestService paymentRequestService,
            TransactionMatchingService transactionMatchingService) {
        this.paymentRequestService = paymentRequestService;
        this.transactionMatchingService = transactionMatchingService;
    }

    /**
     * 입금요청 생성
     * POST /clubs/{clubId}/payment-requests
     */
    @PostMapping
    public ResponseEntity<List<PaymentRequest>> createPaymentRequests(
            @PathVariable Long clubId,
            @RequestBody PaymentRequestCreateRequest request) {
        List<PaymentRequest> created = paymentRequestService.createPaymentRequests(clubId, request);
        return ResponseEntity.ok(created);
    }

    /**
     * 입금요청 목록 조회
     * GET /clubs/{clubId}/payment-requests
     */
    @GetMapping
    public ResponseEntity<List<PaymentRequest>> getPaymentRequests(
            @PathVariable Long clubId) {
        List<PaymentRequest> requests = paymentRequestService.getPaymentRequests(clubId);
        return ResponseEntity.ok(requests);
    }

    /**
     * 특정 상태의 입금요청 조회
     * GET /clubs/{clubId}/payment-requests?status=PENDING
     */
    @GetMapping(params = "status")
    public ResponseEntity<List<PaymentRequest>> getPaymentRequestsByStatus(
            @PathVariable Long clubId,
            @RequestParam PaymentRequest.RequestStatus status) {
        List<PaymentRequest> requests = paymentRequestService.getPaymentRequestsByStatus(clubId, status);
        return ResponseEntity.ok(requests);
    }

    /**
     * 수동 매칭
     * POST /clubs/{clubId}/payment-requests/{requestId}/match
     */
    @PostMapping("/{requestId}/match")
    public ResponseEntity<Void> manualMatch(
            @PathVariable Long clubId,
            @PathVariable Long requestId,
            @RequestBody ManualMatchRequest request) {
        transactionMatchingService.manualMatch(requestId, request.historyId(), request.matchedBy());
        return ResponseEntity.ok().build();
    }

    /**
     * 수동 매칭 요청 DTO
     */
    public record ManualMatchRequest(
            Long historyId,
            Long matchedBy) {
    }
}
