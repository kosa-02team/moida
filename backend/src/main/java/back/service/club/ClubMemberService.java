package back.service.club;

import back.domain.Users;
import back.domain.club.ClubMembers;
import back.domain.club.Clubs;
import back.dto.club.ClubMemberRequest;
import back.dto.club.ClubMemberResponse;
import back.exception.ClubException;
import back.exception.response.ErrorCode;
import back.repository.UserRepository;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMemberService {

    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public ClubMemberResponse joinClub(Long clubId, Long userId, ClubMemberRequest request) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(ClubException.NotFound::new);

        if (club.getStatus() != Clubs.Status.ACTIVE) {
            throw new ClubException.IsClosed();
        }

        long currentMembers = clubMemberRepository.countByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE);
        if (currentMembers >= club.getMaxMembers()) {
            throw new ClubException.ClubFull();
        }

        // 기존 멤버 조회 (재신청인지 확인)
        Optional<ClubMembers> existingMemberOpt = clubMemberRepository.findByClubIdAndUserId(clubId, userId);

        // ACTIVE 또는 PENDING 상태인 멤버의 닉네임만 중복 체크 (REJECTED, LEFT, KICKED 상태는 재사용 가능)
        // 단, 같은 사용자가 재신청하는 경우 자신의 기존 닉네임은 제외
        boolean isActiveNickname = clubMemberRepository.existsByClubIdAndNicknameAndStatus(clubId, request.getNickname(), ClubMembers.Status.ACTIVE);
        boolean isPendingNickname = clubMemberRepository.existsByClubIdAndNicknameAndStatus(clubId, request.getNickname(), ClubMembers.Status.PENDING);

        // 같은 사용자가 재신청하는 경우, 자신의 기존 닉네임과 같으면 중복 체크 통과
        if (existingMemberOpt.isPresent()) {
            ClubMembers existingMember = existingMemberOpt.get();
            if (existingMember.getNickname().equals(request.getNickname())) {
                // 같은 닉네임으로 재신청하는 경우 중복 체크 통과
                isActiveNickname = false;
                isPendingNickname = false;
            }
        }

        if (isActiveNickname || isPendingNickname) {
            throw new ClubException.MemberNicknameDuplicate();
        }

        ClubMembers member = existingMemberOpt
                .map(existingMember -> {
                    existingMember.reApply();
                    // 재신청 시 닉네임도 업데이트 (다른 닉네임으로 재신청하는 경우)
                    if (!existingMember.getNickname().equals(request.getNickname())) {
                        existingMember.changeNickname(request.getNickname());
                    }
                    return existingMember;
                })
                .orElseGet(() -> {
                    ClubMembers newMember = ClubMembers.builder()
                            .clubId(clubId)
                            .userId(userId)
                            .nickname(request.getNickname())
                            .build();
                    return clubMemberRepository.save(newMember);
                });

        // 가입 신청 알림 이벤트 발행
        eventPublisher.publishEvent(new back.event.ClubJoinRequestEvent(
                clubId,
                userId,
                member.getNickname(),
                club.getClubName()));

        return ClubMemberResponse.from(member);
    }

    @Transactional
    public ClubMemberResponse approveClubMember(Long clubId, Long memberId) {
        ClubMembers targetMember = clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)
                .orElseThrow(ClubException.MemberNotFound::new);

        if (targetMember.getStatus() != ClubMembers.Status.PENDING) {
            throw new ClubException.MemberNotPending();
        }

        targetMember.approve();

        // 클럽 정보 조회가 필요함 (이벤트를 위해)
        Clubs club = clubRepository.findById(clubId).orElseThrow(ClubException.NotFound::new);

        // 가입 환영 이벤트 발행
        eventPublisher.publishEvent(new back.event.ClubJoinEvent(
                clubId,
                targetMember.getMemberId(),
                targetMember.getUserId(),
                club.getClubName()));

        return ClubMemberResponse.from(targetMember);
    }

    @Transactional
    public void rejectClubMember(Long clubId, Long memberId) {
        ClubMembers member = clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)
                .orElseThrow(ClubException.MemberNotFound::new);

        member.reject();
    }

    @Transactional
    public void kickMember(Long clubId, Long memberId) {
        ClubMembers targetMember = clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)
                .orElseThrow(ClubException.MemberNotFound::new);

        targetMember.kick();
    }

    @Transactional
    public void leaveClub(Long clubId, Long userId) {
        ClubMembers member = clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(ClubException.MemberNotFound::new);

        if (member.getStatus() != ClubMembers.Status.ACTIVE) {
            throw new ClubException.MemberNotActive();
        }

        // 모임장은 탈퇴할 수 없음 (모임장 위임 후 탈퇴 가능)
        if (member.getRole() == ClubMembers.Role.OWNER) {
            throw new ClubException(ErrorCode.CLUB_AUTH_NOT_OWNER);
        }

        member.left();
    }

    /**
     * 모임 멤버 목록 조회 (상태별 필터링 가능)
     */
    public List<ClubMemberResponse> getMembers(Long clubId, ClubMembers.Status status) {
        List<ClubMembers> members = clubMemberRepository.findByClubIdAndStatus(clubId, status);

        return members.stream()
                .map(member -> {
                    Users user = userRepository.findById(member.getUserId()).orElse(null);
                    String realName = user != null ? user.getRealName() : null;
                    return ClubMemberResponse.from(member, realName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 멤버 역할 변경
     */
    @Transactional
    public ClubMemberResponse updateMemberRole(Long clubId, Long memberId, ClubMembers.Role newRole) {
        ClubMembers member = clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)
                .orElseThrow(ClubException.MemberNotFound::new);

        if (member.getStatus() != ClubMembers.Status.ACTIVE) {
            throw new ClubException.MemberNotActive();
        }

        // OWNER 역할은 변경 불가 (다른 방법으로 처리해야 함)
        if (member.getRole() == ClubMembers.Role.OWNER && newRole != ClubMembers.Role.OWNER) {
            throw new ClubException(ErrorCode.CLUB_AUTH_NOT_OWNER);
        }

        // OWNER로 변경 불가
        if (newRole == ClubMembers.Role.OWNER) {
            throw new ClubException(ErrorCode.CLUB_INVALID_ROLE);
        }

        switch (newRole) {
            case ACCOUNTANT:
                member.promoteToAccountant();
                break;
            case STAFF:
                member.promoteToStaff();
                break;
            case MEMBER:
                member.demoteToMember();
                break;
            case NONE:
                // NONE은 kick 처리 시에만 사용
                throw new ClubException(ErrorCode.CLUB_INVALID_ROLE);
            default:
                throw new ClubException(ErrorCode.CLUB_INVALID_ROLE);
        }

        Users user = userRepository.findById(member.getUserId()).orElse(null);
        String realName = user != null ? user.getRealName() : null;
        return ClubMemberResponse.from(member, realName);
    }
}
