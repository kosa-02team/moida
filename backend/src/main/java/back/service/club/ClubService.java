package back.service.club;

import back.bank.domain.BankAccounts;
import back.bank.dto.request.AccountCreateRequest;
import back.bank.service.BankService;
import back.domain.Users;
import back.domain.club.ClubMembers;
import back.domain.club.Clubs;
import back.dto.club.ClubRequest;
import back.dto.club.ClubResponse;
import back.exception.AuthException;
import back.exception.ClubException;
import back.exception.response.ErrorCode;
import back.repository.UserRepository;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import back.service.post.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final BankService bankService;
    private final ImageService imageService;

    @Transactional
    public ClubResponse createClub(ClubRequest request, Long ownerId) {
        if (clubRepository.existsByClubName(request.getClubName())) {
            throw new ClubException.AlreadyExists();
        }

        // 사용자 정보 조회 (닉네임 생성을 위해)
        Users owner = userRepository.findById(ownerId)
                .orElseThrow(AuthException.UserNotFound::new);

        Clubs club = new Clubs(
                request.getClubName(),
                ownerId,
                request.getTypeEnum(),
                request.getMaxMembers() != null ? request.getMaxMembers() : 100);
        club.setVisibility(request.getVisibilityEnum());
        club.setCategory(request.getCategoryEnum());

        // 이미지 저장 처리
        if (request.getCoverImageUrl() != null && !request.getCoverImageUrl().isEmpty()) {
            String imageUrl = imageService.saveBase64Image(request.getCoverImageUrl());
            club.setCoverImageUrl(imageUrl);
        }

        Clubs savedClub = clubRepository.save(club);
        
        // 모임 생성 시 자동으로 가상 계좌 생성 (실패해도 모임 생성은 계속 진행)
        try {
            BankAccounts bankAccount = bankService.createAccount(
                    savedClub.getClubId(),
                    new AccountCreateRequest(
                            ownerId,
                            "STUB", // 기본값으로 STUB 은행 사용 (개발 환경)
                            null, // accountNumber는 null로 하면 자동 생성
                            owner.getRealName() // 예금주명은 모임장 이름
                    ));

            // 생성된 계좌 ID를 모임의 main_account_id에 저장
            savedClub.changeMainAccount(bankAccount.getAccountId().toString());
            clubRepository.save(savedClub);
        } catch (Exception e) {
            // 계좌 생성 실패 시 로그만 남기고 모임 생성은 계속 진행
            // 나중에 관리 페이지에서 수동으로 계좌를 생성할 수 있음
            System.err.println("모임 생성 시 계좌 자동 생성 실패 (clubId: " + savedClub.getClubId() + "): " + e.getMessage());
            e.printStackTrace();
        }
        
        // 모임 생성자를 OWNER로 자동 가입 (사용자 이름을 닉네임으로 사용)
        ClubMembers ownerMember = ClubMembers.builder()
                .clubId(savedClub.getClubId())
                .userId(ownerId)
                .nickname(owner.getRealName()) // 사용자 실제 이름을 닉네임으로 사용
                .build();
        ownerMember.promoteToOwner(); // OWNER 권한 부여 (approve 전에 먼저 설정)
        ownerMember.approve(); // PENDING -> ACTIVE로 변경 (role은 이미 OWNER로 설정됨)
        clubMemberRepository.save(ownerMember);
        
        return ClubResponse.from(savedClub, 1); // 현재 멤버 수 1명 (owner)
    }

    public ClubResponse getClub(Long clubId, Long viewerId) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        boolean isPublic = club.getVisibility() == Clubs.Visibility.PUBLIC;
        boolean isMember = viewerId != null &&
                clubMemberRepository.existsByClubIdAndUserIdAndStatus(clubId, viewerId, ClubMembers.Status.ACTIVE);

        // 비공개 모임이고 멤버가 아닌 경우 접근 거부
        if (!isPublic && !isMember) {
            if (viewerId == null) {
                throw new ClubException.AuthLoginRequired();
            }
            throw new ClubException.AuthNotActive();
        }

        Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE);
        return ClubResponse.full(club, currentMembers);
    }

    public ClubResponse getClub(Long clubId) {
        return getClub(clubId, null);
    }

    @Transactional
    public ClubResponse updateClub(Long clubId, ClubRequest request, Long ownerId) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        if (!club.getClubName().equals(request.getClubName())
                && clubRepository.existsByClubName(request.getClubName())) {
            throw new ClubException.AlreadyExists();
        }

        club.updateName(request.getClubName());
        club.setVisibility(request.getVisibilityEnum());
        if (request.getType() != null) {
            club.setType(request.getTypeEnum());
        }
        if (request.getMaxMembers() != null) {
            club.setMaxMembers(request.getMaxMembers());
        }
        if (request.getCategory() != null) {
            club.setCategory(request.getCategoryEnum());
        }

        // 이미지 업데이트 처리
        if (request.getCoverImageUrl() != null) {
            if (request.getCoverImageUrl().isEmpty()) {
                // 빈 문자열이면 이미지 삭제
                club.setCoverImageUrl(null);
            } else {
                // 새 이미지가 있으면 저장
                String imageUrl = imageService.saveBase64Image(request.getCoverImageUrl());
                club.setCoverImageUrl(imageUrl);
            }
        }

        Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE);
        return ClubResponse.from(club, currentMembers);
    }

    @Transactional
    public void closeClub(Long clubId, Long ownerId) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        club.close();
    }

    @Transactional
    public void requestClubDeletion(Long clubId, Long ownerId) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        if (!club.getOwnerId().equals(ownerId)) {
            throw new ClubException(ErrorCode.CLUB_AUTH_NOT_OWNER);
        }

        if (club.getDeletionRequestStatus() != null
                && club.getDeletionRequestStatus() == Clubs.DeletionRequestStatus.PENDING) {
            throw new ClubException(ErrorCode.CLUB_DELETION_ALREADY_REQUESTED);
        }

        // 삭제 요청 시작
        club.requestDeletion();

        // 모든 운영진(OWNER, ACCOUNTANT, STAFF)의 삭제 동의 초기화
        List<ClubMembers> staffMembers = clubMemberRepository.findByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE)
                .stream()
                .filter(member -> member.getRole() == ClubMembers.Role.OWNER
                        || member.getRole() == ClubMembers.Role.ACCOUNTANT
                        || member.getRole() == ClubMembers.Role.STAFF)
                .toList();

        for (ClubMembers member : staffMembers) {
            member.resetDeletionApproval();
            // 모임장은 삭제 요청 시작 시 자동으로 동의한 것으로 간주
            if (member.getRole() == ClubMembers.Role.OWNER && member.getUserId().equals(ownerId)) {
                member.approveDeletion();
            }
        }

        // 모임장만 있는 경우 즉시 APPROVED로 변경
        if (staffMembers.size() == 1 && staffMembers.get(0).getRole() == ClubMembers.Role.OWNER) {
            club.approveDeletion();
        }
    }

    @Transactional
    public void approveClubDeletion(Long clubId, Long userId) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        if (club.getDeletionRequestStatus() != Clubs.DeletionRequestStatus.PENDING) {
            throw new ClubException(ErrorCode.CLUB_DELETION_NOT_REQUESTED);
        }

        ClubMembers member = clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(ClubException.MemberNotFound::new);

        if (member.getStatus() != ClubMembers.Status.ACTIVE) {
            throw new ClubException.MemberNotActive();
        }

        // 운영진(OWNER, ACCOUNTANT, STAFF)만 동의 가능
        if (member.getRole() != ClubMembers.Role.OWNER
                && member.getRole() != ClubMembers.Role.ACCOUNTANT
                && member.getRole() != ClubMembers.Role.STAFF) {
            throw new ClubException(ErrorCode.CLUB_AUTH_NOT_STAFF);
        }

        // 동의 처리
        member.approveDeletion();

        // 모든 운영진이 동의했는지 확인
        List<ClubMembers> staffMembers = clubMemberRepository.findByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE)
                .stream()
                .filter(m -> m.getRole() == ClubMembers.Role.OWNER
                        || m.getRole() == ClubMembers.Role.ACCOUNTANT
                        || m.getRole() == ClubMembers.Role.STAFF)
                .toList();

        boolean allApproved = staffMembers.stream()
                .allMatch(m -> Boolean.TRUE.equals(m.getDeletionApproval()));

        if (allApproved && !staffMembers.isEmpty()) {
            // 모든 운영진이 동의했으면 삭제 요청 상태를 APPROVED로 변경
            club.approveDeletion();
        }
    }

    @Transactional
    public void rejectClubDeletion(Long clubId, Long userId) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        if (club.getDeletionRequestStatus() == null
                || club.getDeletionRequestStatus() != Clubs.DeletionRequestStatus.PENDING) {
            throw new ClubException(ErrorCode.CLUB_DELETION_NOT_REQUESTED);
        }

        ClubMembers member = clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(ClubException.MemberNotFound::new);

        if (member.getStatus() != ClubMembers.Status.ACTIVE) {
            throw new ClubException.MemberNotActive();
        }

        // 운영진만 거부 가능
        if (member.getRole() != ClubMembers.Role.OWNER
                && member.getRole() != ClubMembers.Role.ACCOUNTANT
                && member.getRole() != ClubMembers.Role.STAFF) {
            throw new ClubException(ErrorCode.CLUB_AUTH_NOT_STAFF);
        }

        // 거부 처리 (거부 시 삭제 요청 취소)
        member.rejectDeletion();
        club.cancelDeletionRequest();

        // 모든 운영진의 동의 상태 초기화
        List<ClubMembers> staffMembers = clubMemberRepository.findByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE)
                .stream()
                .filter(m -> m.getRole() == ClubMembers.Role.OWNER
                        || m.getRole() == ClubMembers.Role.ACCOUNTANT
                        || m.getRole() == ClubMembers.Role.STAFF)
                .toList();

        for (ClubMembers m : staffMembers) {
            m.resetDeletionApproval();
        }
    }

    @Transactional
    public void cancelClubDeletionRequest(Long clubId, Long ownerId) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        if (!club.getOwnerId().equals(ownerId)) {
            throw new ClubException(ErrorCode.CLUB_AUTH_NOT_OWNER);
        }

        if (club.getDeletionRequestStatus() == null
                || club.getDeletionRequestStatus() != Clubs.DeletionRequestStatus.PENDING) {
            throw new ClubException(ErrorCode.CLUB_DELETION_NOT_REQUESTED);
        }

        club.cancelDeletionRequest();

        // 모든 운영진의 동의 상태 초기화
        List<ClubMembers> staffMembers = clubMemberRepository.findByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE)
                .stream()
                .filter(m -> m.getRole() == ClubMembers.Role.OWNER
                        || m.getRole() == ClubMembers.Role.ACCOUNTANT
                        || m.getRole() == ClubMembers.Role.STAFF)
                .toList();

        for (ClubMembers member : staffMembers) {
            member.resetDeletionApproval();
        }
    }

    @Transactional
    public void activateClub(Long clubId, Long ownerId) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        club.activate();
    }

    /**
     * 모임장 위임
     */
    @Transactional
    public void transferOwnership(Long clubId, Long currentOwnerId, Long newOwnerMemberId) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        // 현재 사용자가 모임장인지 확인
        if (!club.getOwnerId().equals(currentOwnerId)) {
            throw new ClubException.AuthNotOwner();
        }

        // 새 모임장 멤버 조회
        ClubMembers newOwnerMember = clubMemberRepository.findByClubIdAndMemberId(clubId, newOwnerMemberId)
                .orElseThrow(ClubException.MemberNotFound::new);

        if (newOwnerMember.getStatus() != ClubMembers.Status.ACTIVE) {
            throw new ClubException.MemberNotActive();
        }

        // 기존 모임장 멤버 조회
        ClubMembers currentOwnerMember = clubMemberRepository.findByClubIdAndUserId(clubId, currentOwnerId)
                .orElseThrow(ClubException.MemberNotFound::new);

        // 새 모임장의 기존 역할 저장 (권한 교환을 위해)
        ClubMembers.Role newOwnerPreviousRole = newOwnerMember.getRole();

        // 권한 교환: 새 모임장의 기존 역할을 기존 모임장에게 부여
        switch (newOwnerPreviousRole) {
            case ACCOUNTANT:
                currentOwnerMember.promoteToAccountant();
                break;
            case STAFF:
                currentOwnerMember.promoteToStaff();
                break;
            case MEMBER:
            default:
                currentOwnerMember.demoteToMember();
                break;
        }

        // 새 모임장으로 위임
        newOwnerMember.promoteToOwner();
        club.changeOwner(newOwnerMember.getUserId());
    }

    // 카테고리별 모임 조회 - ACTIVE 상태만 조회
    public Page<ClubResponse> getClubsByCategory(String category, Pageable pageable) {
        Clubs.Category categoryEnum;
        try {
            categoryEnum = Clubs.Category.valueOf(category.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ClubException(ErrorCode.CLUB_INVALID_CATEGORY);
        }

        return clubRepository.findByCategoryAndStatus(categoryEnum, Clubs.Status.ACTIVE, pageable)
                .map(club -> {
                    Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(
                            club.getClubId(), ClubMembers.Status.ACTIVE);
                    return ClubResponse.from(club, currentMembers);
                });
    }

    // 카테고리 + 상태별 모임 조회
    public Page<ClubResponse> getClubsByCategoryAndStatus(Clubs.Category category, Clubs.Status status,
            Pageable pageable) {
        return clubRepository.findByCategoryAndStatus(category, status, pageable)
    public Page<ClubResponse> getClubsByCategoryAndStatus(String category, String status, Pageable pageable) {
        Clubs.Category categoryEnum;
        try {
            categoryEnum = Clubs.Category.valueOf(category.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ClubException(ErrorCode.CLUB_INVALID_CATEGORY);
        }

        Clubs.Status statusEnum;
        try {
            statusEnum = Clubs.Status.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ClubException(ErrorCode.CLUB_INVALID_STATUS);
        }

        return clubRepository.findByCategoryAndStatus(categoryEnum, statusEnum, pageable)
                .map(club -> {
                    Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(
                            club.getClubId(), ClubMembers.Status.ACTIVE);
                    return ClubResponse.from(club, currentMembers);
                });
    }

    // 카테고리 + 이름 검색 - ACTIVE 상태만 조회
    public Page<ClubResponse> searchClubsByCategoryAndName(Clubs.Category category, String clubName,
            Pageable pageable) {
        return clubRepository
                .findByCategoryAndStatusAndClubNameContaining(category, Clubs.Status.ACTIVE, clubName, pageable)
    public Page<ClubResponse> searchClubsByCategoryAndName(String category, String clubName, Pageable pageable) {
        Clubs.Category categoryEnum;
        try {
            categoryEnum = Clubs.Category.valueOf(category.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ClubException(ErrorCode.CLUB_INVALID_CATEGORY);
        }

        return clubRepository.findByCategoryAndStatusAndClubNameContaining(categoryEnum, Clubs.Status.ACTIVE, clubName, pageable)
                .map(club -> {
                    Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(
                            club.getClubId(), ClubMembers.Status.ACTIVE);
                    return ClubResponse.from(club, currentMembers);
                });
    }

    // 이름 검색 - ACTIVE 상태만 조회
    public Page<ClubResponse> searchClubsByName(String clubName, Pageable pageable) {
        return clubRepository.findByStatusAndClubNameContaining(Clubs.Status.ACTIVE, clubName, pageable)
                .map(club -> {
                    Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(
                            club.getClubId(), ClubMembers.Status.ACTIVE);
                    return ClubResponse.from(club, currentMembers);
                });
    }

    // 모든 모임 조회 (페이징) - ACTIVE 상태만 조회
    public Page<ClubResponse> getAllClubs(Pageable pageable) {
        return clubRepository.findByStatus(Clubs.Status.ACTIVE, pageable)
                .map(club -> {
                    Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(
                            club.getClubId(), ClubMembers.Status.ACTIVE);
                    return ClubResponse.from(club, currentMembers);
                });
    }

    // 초대 코드로 모임 조회
    public ClubResponse getClubByInviteCode(String inviteCode, Long viewerId) {
        Clubs club = clubRepository.findByInviteCode(inviteCode)
                .orElseThrow(ClubException.NotFound::new);
        return getClub(club.getClubId(), viewerId);
    }

    public ClubResponse getClubByInviteCode(String inviteCode) {
        return getClubByInviteCode(inviteCode, null);
    }
}
