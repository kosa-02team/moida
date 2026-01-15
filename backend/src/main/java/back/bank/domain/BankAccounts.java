package back.bank.domain;

import back.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankAccounts extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", nullable = false)
    private Banks bank;

    @Column(name = "account_number", nullable = false, unique = true, length = 255)
    private String accountNumber;

    @Column(name = "depositor_name", nullable = false, length = 50)
    private String depositorName;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 생성자
    public BankAccounts(Long clubId, String bankCode, Long userId, Banks bank, String accountNumber,
            String depositorName) {
        this.clubId = clubId;
        this.bankCode = bankCode;
        this.userId = userId;
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.depositorName = depositorName;
    }

    // 도메인 메서드
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void updateAccountInfo(Banks bank, String accountNumber, String depositorName) {
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.depositorName = depositorName;
    }
}
