package back.service.club;

import back.domain.club.ClubMembers;
import back.domain.club.Clubs;
import back.dto.club.ClubMemberRequest;
import back.dto.club.ClubMemberResponse;
import back.exception.ClubException;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMemberService {

    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;
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

        return clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .map(existingMember -> {
                    existingMember.reApply();
                    return ClubMemberResponse.from(existingMember);
                })
                .orElseGet(() -> {
                    ClubMembers newMember = ClubMembers.builder()
                            .clubId(clubId)
                            .userId(userId)
                            .nickname(request.getNickname())
                            .build();
                    return ClubMemberResponse.from(clubMemberRepository.save(newMember));
                });
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
}
