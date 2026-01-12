package back.domain;

import back.domain.converter.RoleConverter;
import back.exception.ClubMemberException;
import back.exception.response.ErrorCode;
import jakarta.persistence.*;
import jdk.jfr.Timestamp;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

@Entity
@Table(name = "club_members", uniqueConstraints = {
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

    @Convert(converter = RoleConverter.class)
    @Column(name= "role", length = 100)
    private List<String> roles = new ArrayList<>(List.of("MEMBER"));

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status;

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING, ACTIVE, REJECTED, LEFT, KICKED
    }

    @Builder
    public ClubMembers(Long clubId, Long userId, String clubNickname) {
        this.clubId = clubId;
        this.userId = userId;
        this.clubNickname = clubNickname;

        this.roles = new ArrayList<>(List.of("MEMBER")) ;
        this.status = Status.PENDING;
    }

    // 도메인 메서드
    public void approve(){
        if (this.status != Status.PENDING) {
            throw new ClubMemberException(ErrorCode.CLUB_MEMBER_NOT_PENDING_STATUS);
        }

        this.status = Status.ACTIVE;
        this.roles = new ArrayList<>(List.of("MEMBER")) ;
        this.joinedAt = LocalDateTime.now();
    }

    public void left() {
        this.status = Status.LEFT;
    }

    public void kick() {
        if(this.status != Status.ACTIVE){
            throw new ClubMemberException(ErrorCode.CLUB_MEMBER_NOT_ACTIVE);
        };
        this.status = Status.KICKED;

        if (this.roles != null) {
            this.roles.clear();
        }
    }
    public void reject() {
        if (this.status != Status.PENDING) {
            throw new ClubMemberException(ErrorCode.CLUB_MEMBER_NOT_PENDING_STATUS);
        }
        this.status = Status.REJECTED;
    }
    public void reApply(){
        if(this.status == Status.KICKED){
            throw  new ClubMemberException(ErrorCode.CLUB_MEMBER_KICKED_OUT_USER);
        }
        if (this.status == Status.REJECTED || this.status == Status.LEFT) {
            this.status = Status.PENDING;
        }else {
            throw new ClubMemberException(ErrorCode.CLUB_MEMBER_ALREADY_APPLIED_OR_ACTIVE);
        }
    }

    public boolean hasRole(String targetRole){
        return this.roles.contains(targetRole);
    }
    public void addRole(String newRole){
        if(!hasRole(newRole)){
            this.roles.add(newRole);
        }
    }
    public void removeRole(String targetRole){
        if(this.roles.size() > 1) {
            this.roles.remove(targetRole);
        }
    }

    // promote
    public void promoteToStaff() {
        if(!hasRole("STAFF")){
            this.addRole("STAFF");
        }
    }
    public void promoteToAccountant() {
        this.addRole("ACCOUNTANT");
    }

    public void demoteToMember() {
        this.roles = new ArrayList<>(List.of("MEMBER"));
    }


    public void changeNickname(String nickname) {
        this.clubNickname = nickname;
    }
}
