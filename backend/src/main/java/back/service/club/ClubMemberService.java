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

        if (clubMemberRepository.existsByClubIdAndNickname(clubId, request.getNickname())) {
            throw new ClubException.MemberNicknameDuplicate();
        }

        ClubMembers member = clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .map(existingMember -> {
                    existingMember.reApply();
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

    /**
     * 모임 멤버 목록 조회 (상태별 필터링 가능) - String 파라미터
     */
    public List<ClubMemberResponse> getMembers(Long clubId, String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            statusStr = "ACTIVE"; // 기본값
        }
        ClubMembers.Status status = parseStatus(statusStr);
        return getMembers(clubId, status);
    }

    /**
     * 모임 멤버 목록 조회 (상태별 필터링 가능) - enum 파라미터 (내부 사용)
     */
    private List<ClubMemberResponse> getMembers(Long clubId, ClubMembers.Status status) {
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
     * 멤버 역할 변경 - String 파라미터
     */
    @Transactional
    public ClubMemberResponse updateMemberRole(Long clubId, Long memberId, String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) {
            throw new ClubException(ErrorCode.CLUB_INVALID_ROLE);
        }
        ClubMembers.Role newRole = parseRole(roleStr);
        return updateMemberRole(clubId, memberId, newRole);
    }

    /**
     * 멤버 역할 변경 - enum 파라미터 (내부 사용)
     */
    @Transactional
    private ClubMemberResponse updateMemberRole(Long clubId, Long memberId, ClubMembers.Role newRole) {
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

    // String을 enum으로 변환하는 헬퍼 메서드들
    private ClubMembers.Status parseStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return ClubMembers.Status.ACTIVE; // 기본값
        }
        try {
            return ClubMembers.Status.valueOf(statusStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ClubException(ErrorCode.CLUB_INVALID_STATUS);
        }
    }

    private ClubMembers.Role parseRole(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) {
            throw new ClubException(ErrorCode.CLUB_INVALID_ROLE);
        }
        try {
            return ClubMembers.Role.valueOf(roleStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ClubException(ErrorCode.CLUB_INVALID_ROLE);
        }
    }
}
