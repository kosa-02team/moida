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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final BankService bankService;

    @Transactional
    public ClubResponse createClub(ClubRequest request, Long ownerId) {
        if (clubRepository.existsByClubName(request.getClubName())) {
            throw new ClubException.AlreadyExists();
        }

        // 사용자 정보 조회 (닉네임 생성을 위해)
        Users owner = userRepository.findById(ownerId)
                .orElseThrow(AuthException.UserNotFound::new);

        // String을 enum으로 변환
        Clubs.Type type = parseType(request.getType());
        Clubs.Visibility visibility = parseVisibility(request.getVisibility());
        Clubs.Category category = parseCategory(request.getCategory());

        Clubs club = new Clubs(
                request.getClubName(),
                ownerId,
                type,
                request.getMaxMembers() != null ? request.getMaxMembers() : 100
        );
        club.setVisibility(visibility);
        club.setCategory(category);
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
                    )
            );
            
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

        if (isPublic || isMember) {
            Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE);
            return ClubResponse.full(club, currentMembers);
        } else {
            return ClubResponse.limited(club);
        }
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
        
        // String을 enum으로 변환
        if (request.getVisibility() != null) {
            club.setVisibility(parseVisibility(request.getVisibility()));
        }
        if (request.getType() != null) {
            club.setType(parseType(request.getType()));
        }
        if (request.getMaxMembers() != null) {
            club.setMaxMembers(request.getMaxMembers());
        }
        if (request.getCategory() != null) {
            club.setCategory(parseCategory(request.getCategory()));
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

        // 기존 모임장을 운영진으로 변경
        ClubMembers currentOwnerMember = clubMemberRepository.findByClubIdAndUserId(clubId, currentOwnerId)
                .orElseThrow(ClubException.MemberNotFound::new);
        currentOwnerMember.promoteToStaff();

        // 새 모임장으로 위임
        newOwnerMember.promoteToOwner();
        club.changeOwner(newOwnerMember.getUserId());
    }

    // 카테고리별 모임 조회 (String 파라미터)
    public Page<ClubResponse> getClubsByCategory(String categoryStr, Pageable pageable) {
        Clubs.Category category = parseCategory(categoryStr);
        return getClubsByCategory(category, pageable);
    }

    // 카테고리별 모임 조회 (enum 파라미터 - 내부 사용)
    private Page<ClubResponse> getClubsByCategory(Clubs.Category category, Pageable pageable) {
        return clubRepository.findByCategory(category, pageable)
                .map(club -> {
                    Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(
                            club.getClubId(), ClubMembers.Status.ACTIVE);
                    return ClubResponse.from(club, currentMembers);
                });
    }

    // 카테고리 + 상태별 모임 조회 (String 파라미터)
    public Page<ClubResponse> getClubsByCategoryAndStatus(String categoryStr, String statusStr, Pageable pageable) {
        Clubs.Category category = parseCategory(categoryStr);
        Clubs.Status status = parseStatus(statusStr);
        return getClubsByCategoryAndStatus(category, status, pageable);
    }

    // 카테고리 + 상태별 모임 조회 (enum 파라미터 - 내부 사용)
    private Page<ClubResponse> getClubsByCategoryAndStatus(Clubs.Category category, Clubs.Status status, Pageable pageable) {
        return clubRepository.findByCategoryAndStatus(category, status, pageable)
                .map(club -> {
                    Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(
                            club.getClubId(), ClubMembers.Status.ACTIVE);
                    return ClubResponse.from(club, currentMembers);
                });
    }

    // 카테고리 + 이름 검색 (String 파라미터)
    public Page<ClubResponse> searchClubsByCategoryAndName(String categoryStr, String clubName, Pageable pageable) {
        Clubs.Category category = parseCategory(categoryStr);
        return searchClubsByCategoryAndName(category, clubName, pageable);
    }

    // 카테고리 + 이름 검색 (enum 파라미터 - 내부 사용)
    private Page<ClubResponse> searchClubsByCategoryAndName(Clubs.Category category, String clubName, Pageable pageable) {
        return clubRepository.findByCategoryAndClubNameContaining(category, clubName, pageable)
                .map(club -> {
                    Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(
                            club.getClubId(), ClubMembers.Status.ACTIVE);
                    return ClubResponse.from(club, currentMembers);
                });
    }

    // 이름 검색
    public Page<ClubResponse> searchClubsByName(String clubName, Pageable pageable) {
        return clubRepository.findByClubNameContaining(clubName, pageable)
                .map(club -> {
                    Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(
                            club.getClubId(), ClubMembers.Status.ACTIVE);
                    return ClubResponse.from(club, currentMembers);
                });
    }

    // 모든 모임 조회 (페이징)
    public Page<ClubResponse> getAllClubs(Pageable pageable) {
        return clubRepository.findAll(pageable)
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

    // String을 enum으로 변환하는 헬퍼 메서드들
    private Clubs.Category parseCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            return Clubs.Category.ETC;
        }
        try {
            return Clubs.Category.valueOf(categoryStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ClubException(ErrorCode.CLUB_INVALID_CATEGORY);
        }
    }

    private Clubs.Status parseStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            throw new ClubException(ErrorCode.CLUB_INVALID_STATUS);
        }
        try {
            return Clubs.Status.valueOf(statusStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ClubException(ErrorCode.CLUB_INVALID_STATUS);
        }
    }

    private Clubs.Visibility parseVisibility(String visibilityStr) {
        if (visibilityStr == null || visibilityStr.trim().isEmpty()) {
            return Clubs.Visibility.PUBLIC;
        }
        try {
            return Clubs.Visibility.valueOf(visibilityStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ClubException(ErrorCode.CLUB_INVALID_STATUS);
        }
    }

    private Clubs.Type parseType(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return Clubs.Type.OPERATION_FEE;
        }
        try {
            return Clubs.Type.valueOf(typeStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ClubException(ErrorCode.CLUB_INVALID_STATUS);
        }
    }
}
