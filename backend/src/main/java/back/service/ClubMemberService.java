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

import java.util.List;
import java.util.NoSuchElementException;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMemberService {

    private final ClubMemberRepository clubMemberRepository;

    @Transactional
    public ClubMemberResponse joinClub(ClubMemberRequest request) {

        clubMemberRepository.findByClubIdAndUserId(request.getClubId(), request.getUserId())
                .ifPresent(member -> {
            if("PENDING".equals(member.getStatus()) || "ACTIVE".equals(member.getStatus())){
                throw new ClubMemberException(ErrorCode.CLUB_MEMBER_ALREADY_APPLIED_OR_ACTIVE);
            }
            if("KICKED".equals(member.getStatus())){
                throw new ClubMemberException(ErrorCode.CLUB_MEMBER_KICKED_OUT_USER);
            }
        });

        ClubMembers clubMember = new ClubMembers(
                request.getClubId(),
                request.getUserId(),
                request.getClubNickname()
        );

        return ClubMemberResponse.from(clubMemberRepository.save(clubMember));
    }

    @Transactional
    public ClubMemberResponse approveClubMember(Long memberId) {
        ClubMembers targetMember = clubMemberRepository.findById(memberId)
                .orElseThrow(() -> new ClubMemberException(ErrorCode.CLUB_MEMBER_NOT_FOUND));

        if(!"PENDING".equals(targetMember.getStatus())){
            throw new ClubMemberException(ErrorCode.CLUB_MEMBER_NOT_PENDING_STATUS);
        }

        targetMember.activate();
        return ClubMemberResponse.from(targetMember);
    }

    @Transactional
    public void rejectClubMember(Long memberId) {
        ClubMembers member = clubMemberRepository.findById(memberId)
                .orElseThrow(() -> new ClubMemberException(ErrorCode.CLUB_MEMBER_NOT_FOUND));

        member.reject();
    }

    @Transactional
    public void kickMember(Long memberId) {
        ClubMembers member = clubMemberRepository.findById(memberId)
                .orElseThrow(() -> new ClubMemberException(ErrorCode.CLUB_MEMBER_NOT_FOUND));

        member.kick();
    }
}
