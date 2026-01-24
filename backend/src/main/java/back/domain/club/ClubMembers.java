package back.domain.club;

import back.domain.BaseEntity;
import back.exception.ClubException;
import back.exception.response.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_club_user", columnNames = { "club_id", "user_id" }),
        @UniqueConstraint(name = "uk_club_nickname", columnNames = { "club_id", "nickname" })
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

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "deletion_approval", nullable = true)
    private Boolean deletionApproval;

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

    public void approve() {
        if (this.status != Status.PENDING) {
            throw new ClubException.MemberNotPending();
        }

        this.status = Status.ACTIVE;
        // role이 이미 설정되어 있으면 변경하지 않음 (OWNER 등)
        if (this.role == null || this.role == Role.MEMBER) {
            this.role = Role.MEMBER;
        }
        this.joinedAt = LocalDateTime.now();
    }

    public void left() {
        this.status = Status.LEFT;
        // 탈퇴 시 닉네임을 고유한 값으로 변경하여 제약조건 위반 방지
        // 재가입 시 같은 닉네임을 사용할 수 있도록 함
        // 닉네임 길이 제한(10자)을 고려하여 짧은 고유값 생성
        if (this.memberId != null) {
            // memberId를 사용 (닉네임 길이 제한 10자 고려)
            // 형식: "_" + memberId (memberId가 9자리 이하인 경우)
            String memberIdStr = String.valueOf(this.memberId);
            if (memberIdStr.length() <= 9) {
                this.nickname = "_" + memberIdStr;
            } else {
                // memberId가 너무 큰 경우 뒤 9자리만 사용
                this.nickname = "_" + memberIdStr.substring(memberIdStr.length() - 9);
            }
        } else if (this.userId != null) {
            // memberId가 null인 경우 userId 사용 (이론적으로 발생하지 않음)
            String userIdStr = String.valueOf(this.userId);
            if (userIdStr.length() <= 9) {
                this.nickname = "_" + userIdStr;
            } else {
                this.nickname = "_" + userIdStr.substring(userIdStr.length() - 9);
            }
        } else {
            // 최후의 수단: 타임스탬프의 마지막 9자리 사용
            String timestamp = String.valueOf(System.currentTimeMillis());
            this.nickname = "_" + timestamp.substring(Math.max(0, timestamp.length() - 9));
        }
    }

    public void kick() {
        if (this.status != Status.ACTIVE) {
            throw new ClubException.MemberNotActive();
        }
        this.status = Status.KICKED;
        this.role = Role.NONE;
        // 강퇴 시 닉네임을 고유한 값으로 변경하여 제약조건 위반 방지
        // 재가입 시 같은 닉네임을 사용할 수 있도록 함
        if (this.memberId != null) {
            this.nickname = "_" + this.memberId;
        }
    }

    public void reject() {
        if (this.status != Status.PENDING) {
            throw new ClubException.MemberNotPending();
        }
        this.status = Status.REJECTED;
        // 거절 시 닉네임을 고유한 값으로 변경하여 제약조건 위반 방지
        // 재가입 시 같은 닉네임을 사용할 수 있도록 함
        if (this.memberId != null) {
            this.nickname = "_" + this.memberId;
        }
    }

    public void reApply() {
        if (this.status == Status.KICKED) {
            throw new ClubException.MemberKickedOut();
        }
        if (this.status == Status.PENDING) {
            throw new ClubException.MemberAlreadyPending();
        }
        if (this.status == Status.ACTIVE) {
            throw new ClubException.MemberAlreadyActive();
        }
        if (this.status == Status.REJECTED || this.status == Status.LEFT) {
            this.status = Status.PENDING;
        }
    }

    public boolean isManagerLevel() {
        if (this.status != Status.ACTIVE)
            return false;
        return this.role.isAtLeast(Role.STAFF);
    }

    public boolean canManageFinance() {
        if (this.status != Status.ACTIVE)
            return false;
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

    public void approveDeletion() {
        if (this.role != Role.OWNER && this.role != Role.ACCOUNTANT && this.role != Role.STAFF) {
            throw new ClubException(ErrorCode.CLUB_AUTH_NOT_STAFF);
        }
        this.deletionApproval = true;
    }

    public void rejectDeletion() {
        if (this.role != Role.OWNER && this.role != Role.ACCOUNTANT && this.role != Role.STAFF) {
            throw new ClubException(ErrorCode.CLUB_AUTH_NOT_STAFF);
        }
        this.deletionApproval = false;
    }

    public void resetDeletionApproval() {
        this.deletionApproval = null;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }
}