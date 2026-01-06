package back.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_club_user", columnNames = {"club_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMembers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "club_nickname", nullable = false, length = 50)
    private String clubNickname;

    @Column(length = 20)
    private String role = "MEMBER";

    @Column(length = 20)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    // 생성자
    public ClubMembers(Long clubId, Long userId, String clubNickname) {
        this.clubId = clubId;
        this.userId = userId;
        this.clubNickname = clubNickname;
    }

    // 도메인 메서드
    public void changeNickname(String nickname) {
        this.clubNickname = nickname;
    }

    public void changeRole(String role) {
        this.role = role;
    }

    public void promoteToStaff() {
        this.role = "STAFF";
    }

    public void promoteToAccountant() {
        this.role = "ACCOUNTANT";
    }

    public void demoteToMember() {
        this.role = "MEMBER";
    }

    public void deactivate() {
        this.status = "INACTIVE";
    }

    public void activate() {
        this.status = "ACTIVE";
    }

    public void kick() {
        this.status = "KICKED";
    }
}

