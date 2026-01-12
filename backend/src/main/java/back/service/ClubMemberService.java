package back.service;

import back.domain.ClubMembers;
import back.dto.ClubMemberRequest;
import back.dto.ClubMemberResponse;
import back.exception.ClubMemberException;
import back.exception.response.ErrorCode;
import back.repository.ClubMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.NoSuchElementException;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMemberService {

    private final ClubMemberRepository clubMemberRepository;

    @Transactional
    public ClubMemberResponse joinClub(Long clubId, ClubMemberRequest request) {

        return clubMemberRepository.findByClubIdAndUserId(clubId, request.getUserId())
                .map(existingMember -> {
                    existingMember.reApply();
                    return ClubMemberResponse.from(existingMember);
                })
                .orElseGet(() -> {
                    ClubMembers newMember = ClubMembers.builder()
                            .clubId(clubId)
                            .userId(request.getUserId())
                            .clubNickname(request.getClubNickname())
                            .build();
                    return ClubMemberResponse.from(clubMemberRepository.save(newMember));
                });
    }

    @Transactional
    public ClubMemberResponse approveClubMember(Long clubId, Long memberId) {
        ClubMembers targetMember = clubMemberRepository.findByClubIdAndUserId(clubId, memberId)
                .orElseThrow(() -> new ClubMemberException(ErrorCode.CLUB_MEMBER_NOT_FOUND));

        if (targetMember.getStatus() != ClubMembers.Status.PENDING) {
            throw new ClubMemberException(ErrorCode.CLUB_MEMBER_NOT_PENDING_STATUS);
        }

        targetMember.approve();
        return ClubMemberResponse.from(targetMember);
    }

    @Transactional
    public void rejectClubMember(Long clubId, Long memberId) {
        ClubMembers member = clubMemberRepository.findById(memberId)
                .orElseThrow(() -> new ClubMemberException(ErrorCode.CLUB_MEMBER_NOT_FOUND));

        if (!member.getClubId().equals(clubId)) {
            throw new ClubMemberException(ErrorCode.CLUB_MEMBER_NOT_FOUND);
        }

        member.reject();
    }

    @Transactional
    public void kickMember(Long clubId, Long memberId) {
        ClubMembers targetMember = clubMemberRepository.findByClubIdAndUserId(clubId, memberId)
                .orElseThrow(() -> new ClubMemberException(ErrorCode.CLUB_MEMBER_NOT_FOUND));

        targetMember.kick();
    }
}
