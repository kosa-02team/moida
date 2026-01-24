package back.domain.club;

import back.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clubs", uniqueConstraints = {
    @UniqueConstraint(name = "uk_club_name", columnNames = {"club_name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clubs extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "club_name", nullable = false, length = 20, unique = true)
    private String clubName;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "main_account_id", nullable = false, length = 36)
    private String mainAccountId = UUID.randomUUID().toString();

    @Column(name = "invite_code", unique = true, length = 36)
    private String inviteCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Visibility visibility = Visibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "club_type", length = 20, nullable = false)
    private Type type = Type.OPERATION_FEE;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private Category category = Category.ETC;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "deletion_request_status", length = 20, nullable = true)
    private DeletionRequestStatus deletionRequestStatus;

    @Column(name = "deletion_requested_at", nullable = true)
    private LocalDateTime deletionRequestedAt;

    @Column(name = "cover_image_url", length = 255, nullable = true)
    private String coverImageUrl;

    public enum Status {
        ACTIVE, INACTIVE
    }

    public enum DeletionRequestStatus {
        NONE,        // 삭제 요청 없음
        PENDING,     // 삭제 요청 중 (운영진 동의 대기)
        APPROVED     // 모든 운영진 동의 완료 (삭제 가능)
    }

    public enum Visibility {
        PUBLIC, PRIVATE
    }

    public enum Type {
        OPERATION_FEE,
        FAIR_SETTLEMENT
    }

    public enum Category {
        STUDY,      // 스터디
        SPORTS,     // 운동
        SOCIAL,     // 친목
        HOBBY,      // 취미
        FINANCE,    // 재테크
        ETC         // 기타
    }

    public Clubs(String clubName, Long ownerId, Type type, Integer maxMembers) {
        this.clubName = clubName;
        this.ownerId = ownerId;
        this.inviteCode = generateUUID();
        this.status = Status.ACTIVE;
        this.visibility = Visibility.PUBLIC;
        this.type = type;
        this.maxMembers = maxMembers;
    }
    // 기본값 생성자 (테스트용)
    public Clubs(String clubName, Long ownerId) {
        this(clubName, ownerId, Type.OPERATION_FEE, 100);
    }

    public void updateName(String clubName) {
        this.clubName = clubName;
    }

    public void changeOwner(Long newOwnerId) {
        this.ownerId = newOwnerId;
    }

    public void changeMainAccount(String accountId) {
        this.mainAccountId = accountId;
    }

    public void regenerateInviteCode() {
        this.inviteCode = generateUUID();
    }

    public void close() {
        this.status = Status.INACTIVE;
        this.closedAt = LocalDateTime.now();
        this.deletionRequestStatus = DeletionRequestStatus.NONE;
        this.deletionRequestedAt = null;
    }

    public void requestDeletion() {
        if (this.deletionRequestStatus == DeletionRequestStatus.PENDING) {
            throw new IllegalStateException("이미 삭제 요청이 진행 중입니다.");
        }
        this.deletionRequestStatus = DeletionRequestStatus.PENDING;
        this.deletionRequestedAt = LocalDateTime.now();
    }

    public void cancelDeletionRequest() {
        if (this.deletionRequestStatus == null || this.deletionRequestStatus != DeletionRequestStatus.PENDING) {
            throw new IllegalStateException("진행 중인 삭제 요청이 없습니다.");
        }
        this.deletionRequestStatus = DeletionRequestStatus.NONE;
        this.deletionRequestedAt = null;
    }

    public void approveDeletion() {
        if (this.deletionRequestStatus == null || this.deletionRequestStatus != DeletionRequestStatus.PENDING) {
            throw new IllegalStateException("삭제 요청이 진행 중이 아닙니다.");
        }
        this.deletionRequestStatus = DeletionRequestStatus.APPROVED;
    }

    public void activate() {
        this.status = Status.ACTIVE;
        this.closedAt = null;
    }

    public void setVisibility(Visibility visibility) {
        if (visibility != null) {
            this.visibility = visibility;
        }
    }

    public void setType(Type type) {
        if (type != null) {
            this.type = type;
        }
    }

    public void setMaxMembers(Integer maxMembers) {
        if (maxMembers != null && maxMembers > 0) {
            this.maxMembers = maxMembers;
        }
    }

    public void setCategory(Category category) {
        if (category != null) {
            this.category = category;
        }
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    private static String generateUUID() {
        return UUID.randomUUID().toString();
    }
}