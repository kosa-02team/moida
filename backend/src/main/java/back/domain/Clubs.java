package back.domain;

import back.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clubs extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_id")
    private Long clubId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "main_account_id", nullable = false)
    private Long mainAccountId;

    @Column(name = "invite_code", unique = true, length = 20)
    private String inviteCode;

    @Column(length = 20)
    private String status = "ACTIVE";

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // 생성자
    public Clubs(String name, Long ownerId, Long mainAccountId) {
        this.name = name;
        this.ownerId = ownerId;
        this.mainAccountId = mainAccountId;
    }

    // 도메인 메서드
    public void updateName(String name) {
        this.name = name;
    }

    public void changeOwner(Long newOwnerId) {
        this.ownerId = newOwnerId;
    }

    public void changeMainAccount(Long accountId) {
        this.mainAccountId = accountId;
    }

    public void generateInviteCode(String code) {
        this.inviteCode = code;
    }

    public void close() {
        this.status = "CLOSED";
        this.closedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = "ACTIVE";
        this.closedAt = null;
    }
}

