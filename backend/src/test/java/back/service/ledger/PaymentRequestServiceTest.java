package back.service.ledger;

import back.domain.ledger.PaymentRequest;
import back.dto.ledger.request.PaymentRequestCreateRequest;
import back.repository.ledger.PaymentRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PaymentRequestServiceTest {

    @Mock
    private PaymentRequestRepository paymentRequestRepository;

    @InjectMocks
    private PaymentRequestService paymentRequestService;

    @Test
    @DisplayName("입금 요청 생성 성공 - 스케줄 ID 포함")
    void create_requests_with_schedule_id() {
        // given
        Long clubId = 1L;
        Long scheduleId = 123L;

        PaymentRequestCreateRequest.RequestItem item = new PaymentRequestCreateRequest.RequestItem(
                10L, "홍길동", "DEPOSIT", BigDecimal.valueOf(10000), LocalDate.now(), 7, 7,
                scheduleId, // ✨ scheduleId
                null        // billingPeriod
        );
        PaymentRequestCreateRequest request = new PaymentRequestCreateRequest(List.of(item));

        // when
        paymentRequestService.createPaymentRequests(clubId, request);

        // then
        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        then(paymentRequestRepository).should(times(1)).save(captor.capture());

        PaymentRequest saved = captor.getValue();
        assertThat(saved.getScheduleId()).isEqualTo(scheduleId);
        assertThat(saved.getExpectedAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(saved.getMemberName()).isEqualTo("홍길동");
    }
}