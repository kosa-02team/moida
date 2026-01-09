package back.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "banks")
public class Banks extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "bank_code", nullable = false, unique = true, length = 10)
    private String bankCode;

    @Column(name = "bank_name", nullable = false, length = 50)
    private String bankName;

    @Column(name = "provider_class_name", length = 255)
    private String providerClassName;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // 생성자
    public Banks(String bankCode, String bankName, String providerClassName) {
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.providerClassName = providerClassName;
    }

    // 도메인 메서드
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
