package back.controller.ledger;

import back.config.security.UserPrincipal;
import back.domain.ledger.PaymentRequest;
import back.dto.ledger.request.PaymentRequestCreateRequest;
import back.service.ledger.EventFundService;
import back.service.ledger.PaymentRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 입금 및 정산 요청 관리 Controller
 */
@RestController
@RequestMapping("/api/clubs/{clubId}")
@RequiredArgsConstructor
public class PaymentRequestController {

    private final PaymentRequestService paymentRequestService;
    private final EventFundService eventFundService;

    // --- 1. 기본 입금 요청 관리 (PaymentRequestService) ---

    /**
     * 수동 입금 요청 생성 (운영비 등)
     */
    @PostMapping("/payment-requests")
    public ResponseEntity<List<PaymentRequest>> createManualRequests(
            @PathVariable Long clubId,
            @RequestBody PaymentRequestCreateRequest request) {
        return ResponseEntity.ok(paymentRequestService.createPaymentRequests(clubId, request));
    }

    /**
     * 입금 요청 목록 조회
     */
    @GetMapping("/payment-requests")
    public ResponseEntity<List<PaymentRequest>> getRequests(@PathVariable Long clubId) {
        return ResponseEntity.ok(paymentRequestService.getPaymentRequests(clubId));
    }

    /**
     * 특정 요청 입금 확인 (관리자 수동 처리)
     */
    @PatchMapping("/payment-requests/{requestId}/confirm")
    public ResponseEntity<Void> confirmRequest(
            @PathVariable Long clubId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal user) {
        Long userId = user.getUserId();
        paymentRequestService.confirmPayment(requestId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 입금 요청 수동 확인 (현금 수령 등)
     */
    @PatchMapping("/payment-requests/{requestId}/confirm-manual")
    public ResponseEntity<Void> confirmManualRequest(
            @PathVariable Long clubId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal user) {
        Long userId = user.getUserId();
        paymentRequestService.confirmManualPayment(requestId, userId);
        return ResponseEntity.ok().build();
    }

    // --- 2. 일정 자금 관리 (EventFundService) ---

    /**
     * [일정] 참가비 일괄 걷기 요청
     * POST /clubs/{id}/schedules/{scheduleId}/collect-fee
     */
    @PostMapping("/schedules/{scheduleId}/collect-fee")
    public ResponseEntity<Void> collectScheduleFee(
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        eventFundService.collectEntryFees(clubId, scheduleId);
        return ResponseEntity.ok().build();
    }

    /**
     * [일정] 정산 및 잔액 환급 실행
     * POST /clubs/{id}/schedules/{scheduleId}/settle
     */
    @PostMapping("/schedules/{scheduleId}/settle")
    public ResponseEntity<Void> settleSchedule(
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        eventFundService.settleAndRefund(clubId, scheduleId);
        return ResponseEntity.ok().build();
    }
}