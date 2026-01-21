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
import back.repository.UserRepository;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        Clubs club = new Clubs(
                request.getClubName(),
                ownerId,
                request.getTypeEnum(),
                request.getMaxMembers() != null ? request.getMaxMembers() : 100
        );
        club.setVisibility(request.getVisibilityEnum());
        club.setCategory(request.getCategoryEnum());
        Clubs savedClub = clubRepository.save(club);
        
        // 모임 생성 시 자동으로 가상 계좌 생성
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
        
        // 모임 생성자를 OWNER로 자동 가입 (사용자 이름을 닉네임으로 사용)
        ClubMembers ownerMember = ClubMembers.builder()
                .clubId(savedClub.getClubId())
                .userId(ownerId)
                .nickname(owner.getRealName()) // 사용자 실제 이름을 닉네임으로 사용
                .build();
        ownerMember.approve(); // PENDING -> ACTIVE로 변경
        ownerMember.promoteToOwner(); // OWNER 권한 부여
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

    // 카테고리별 모임 조회
    public Page<ClubResponse> getClubsByCategory(Clubs.Category category, Pageable pageable) {
        return clubRepository.findByCategory(category, pageable)
                .map(club -> {
                    Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(
                            club.getClubId(), ClubMembers.Status.ACTIVE);
                    return ClubResponse.from(club, currentMembers);
                });
    }

    // 카테고리 + 상태별 모임 조회
    public Page<ClubResponse> getClubsByCategoryAndStatus(Clubs.Category category, Clubs.Status status, Pageable pageable) {
        return clubRepository.findByCategoryAndStatus(category, status, pageable)
                .map(club -> {
                    Integer currentMembers = (int) clubMemberRepository.countByClubIdAndStatus(
                            club.getClubId(), ClubMembers.Status.ACTIVE);
                    return ClubResponse.from(club, currentMembers);
                });
    }

    // 카테고리 + 이름 검색
    public Page<ClubResponse> searchClubsByCategoryAndName(Clubs.Category category, String clubName, Pageable pageable) {
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
}
