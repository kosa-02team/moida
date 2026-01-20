package back.bank.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankTransactionHistory {

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "bank_transaction_at", nullable = false)
    private LocalDateTime bankTransactionAt;

    @Column(name = "print_content", nullable = false, length = 50)
    private String printContent;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "is_matched")
    private Boolean isMatched = false;

    @Column(name = "unique_tx_key", unique = true, length = 255)
    private String uniqueTxKey;

    @Column(name = "inout_type", nullable = false, length = 20)
    private String inoutType; // "DEPOSIT" or "WITHDRAW"

    // 생성자
    public BankTransactionHistory(Long clubId, LocalDateTime bankTransactionAt,
            String printContent,
            BigDecimal amount, String uniqueTxKey, String inoutType) {
        this.clubId = clubId;
        this.bankTransactionAt = bankTransactionAt;
        this.printContent = printContent;
        this.amount = amount;
        this.uniqueTxKey = uniqueTxKey;
        this.inoutType = inoutType;
    }

    // 도메인 메서드
    public void markAsMatched() {
        this.isMatched = true;
    }

    public void unmarkAsMatched() {
        this.isMatched = false;
    }
}
