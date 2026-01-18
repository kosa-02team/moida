package back.domain.club;

import back.domain.BaseEntity;
import back.exception.ClubException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_members", uniqueConstraints = {
    @UniqueConstraint(name = "uk_club_user", columnNames = {"club_id", "user_id"}),
    @UniqueConstraint(name = "uk_club_nickname", columnNames = {"club_id", "nickname"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMembers extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nickname", nullable = false, length = 10)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private Role role = Role.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status;

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    public enum Status {
        PENDING, ACTIVE, REJECTED, LEFT, KICKED
    }

    public enum Role {
        OWNER(0),
        ACCOUNTANT(1), 
        STAFF(2),      
        MEMBER(3),     
        NONE(99);

        private final int priority;

        Role(int priority) {
            this.priority = priority;
        }

        public boolean isAtLeast(Role targetRole) {
            return this.priority <= targetRole.priority;
        }
    }

    @Builder
    public ClubMembers(Long clubId, Long userId, String nickname) {
        this.clubId = clubId;
        this.userId = userId;
        this.nickname = nickname;
        this.role = Role.MEMBER;
        this.status = Status.PENDING;
    }

    public void approve(){
        if (this.status != Status.PENDING) {
            throw new ClubException.MemberNotPending();
        }

        this.status = Status.ACTIVE;
        this.role = Role.MEMBER;
        this.joinedAt = LocalDateTime.now();
    }

    public void left() {
        this.status = Status.LEFT;
    }

    public void kick() {
        if(this.status != Status.ACTIVE){
            throw new ClubException.MemberNotActive();
        };
        this.status = Status.KICKED;
        this.role = Role.NONE;
    }
    
    public void reject() {
        if (this.status != Status.PENDING) {
            throw new ClubException.MemberNotPending();
        }
        this.status = Status.REJECTED;
    }
    
    public void reApply(){
        if(this.status == Status.KICKED){
            throw new ClubException.MemberKickedOut();
        }
        if (this.status == Status.REJECTED || this.status == Status.LEFT) {
            this.status = Status.PENDING;
        }else {
            throw new ClubException.MemberAlreadyActive();
        }
    }

    public boolean isManagerLevel() {
        if (this.status != Status.ACTIVE) return false;
        return this.role.isAtLeast(Role.STAFF);
    }

    public boolean canManageFinance() {
        if (this.status != Status.ACTIVE) return false;
        return this.role.isAtLeast(Role.ACCOUNTANT);
    }

    public void promoteToOwner() {
        this.role = Role.OWNER;
    }

    public void promoteToAccountant() {
        this.role = Role.ACCOUNTANT;
    }

    public void promoteToStaff() {
        this.role = Role.STAFF;
    }

    public void demoteToMember() {
        this.role = Role.MEMBER;
    }


    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }
}