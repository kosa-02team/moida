package back.repository.ledger;

import back.domain.ledger.PaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {

        /**
         * 특정 모임의 특정 상태인 입금요청 조회
         */
        List<PaymentRequest> findByClubIdAndStatus(Long clubId, PaymentRequest.RequestStatus status);

        /**
         * 특정 모임의 모든 입금요청 조회
         */
        List<PaymentRequest> findByClubIdOrderByCreatedAtDesc(Long clubId);

        /**
         * 매칭 가능한 입금요청 조회 (PENDING 상태 + 만료되지 않음)
         */
        @Query("SELECT pr FROM PaymentRequest pr WHERE pr.clubId = :clubId " +
                        "AND pr.status = 'PENDING' " +
                        "AND (pr.expiresAt IS NULL OR pr.expiresAt > CURRENT_TIMESTAMP) " +
                        "ORDER BY pr.expectedDate ASC")
        List<PaymentRequest> findMatchableRequests(@Param("clubId") Long clubId);

        /**
         * 날짜 범위 내 매칭 가능한 입금요청 조회
         */
        @Query("SELECT pr FROM PaymentRequest pr WHERE pr.clubId = :clubId " +
                        "AND pr.status = 'PENDING' " +
                        "AND pr.expectedDate BETWEEN :fromDate AND :toDate " +
                        "AND (pr.expiresAt IS NULL OR pr.expiresAt > CURRENT_TIMESTAMP)")
        List<PaymentRequest> findMatchableRequestsByDateRange(
                        @Param("clubId") Long clubId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        List<PaymentRequest> findByScheduleIdAndStatus(Long scheduleId, PaymentRequest.RequestStatus status);
}
