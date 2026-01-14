package back.service;

import back.domain.*;
import back.domain.posts.Posts;
import back.dto.VoteAnswerRequest;
import back.dto.VoteCreateRequest;
import back.dto.VoteResponse;
import back.exception.ResourceException;
import back.exception.VoteException;
import back.repository.clubs.ClubMembersRepository;
import back.repository.clubs.ClubsRepository;
import back.repository.posts.PostRepository;
import back.repository.SchedulesRepository;
import back.repository.VoteOptionsRepository;
import back.repository.VoteRecordsRepository;
import back.repository.VotesRepository;
import back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final PostRepository postRepository;
    private final SchedulesRepository schedulesRepository;
    private final VotesRepository votesRepository;
    private final VoteOptionsRepository voteOptionsRepository;
    private final VoteRecordsRepository voteRecordsRepository;
    private final ClubMembersRepository clubMembersRepository;
    private final ClubsRepository clubsRepository;
    private final UserRepository userRepository;

    /**
     * 모임에 속한 일정/참석 투표를 생성합니다.
     *
     * @param clubId  투표가 속한 모임 ID
     * @param userId  투표 생성자(현재 로그인 유저) ID
     * @param request 투표 생성 요청 정보
     * @return 생성된 투표 정보
     */
    @Transactional
    public VoteResponse createVote(Long clubId, Long userId, VoteCreateRequest request) {
        // ATTENDANCE 타입일 때 scheduleId 필수 검증
        if ("ATTENDANCE".equals(request.voteType()) && request.scheduleId() == null) {
            throw new VoteException.ScheduleIdRequired();
        }

        // ATTENDANCE 타입이고 scheduleId가 있으면 일정 존재 여부 및 모임 소속 확인
        Schedules schedule = null;
        if ("ATTENDANCE".equals(request.voteType()) && request.scheduleId() != null) {
            schedule = schedulesRepository.findById(request.scheduleId())
                    .orElseThrow(ResourceException.NotFound::new);

            // 일정이 해당 모임에 속하는지 확인
            if (!schedule.getClubId().equals(clubId)) {
                throw new VoteException.ClubMismatch();
            }
        }

        Clubs clubRef = clubsRepository.getReferenceById(clubId);
        Users writerRef = userRepository.getReferenceById(userId);
        Schedules scheduleRef = (request.scheduleId() == null)
                ? null
                : schedulesRepository.getReferenceById(request.scheduleId());

        // 1. Posts 엔티티 생성 (투표 게시글)
        Posts post = Posts.vote(
                clubRef,
                writerRef,
                scheduleRef,
                request.title(),
                request.description()
        );
        post = postRepository.save(post);

        // 2. Votes 엔티티 생성
        Votes vote = new Votes(
                post.getPostId(),
                request.voteType(),
                request.scheduleId(),
                userId,
                request.title(),
                request.description(),
                request.isAnonymous(),
                request.allowMultiple(),
                null  // deadline은 일반 투표에서만 사용
        );
        vote = votesRepository.save(vote);

        // 3. ATTENDANCE 타입이면 VoteOptions 자동 생성 (참석/불참)
        if ("ATTENDANCE".equals(request.voteType()) && schedule != null) {
            // "참석" 옵션 생성
            VoteOptions attendOption = new VoteOptions(
                    vote.getVoteId(),
                    "참석",
                    1,
                    schedule.getEventDate(),
                    schedule.getLocation()
            );
            voteOptionsRepository.save(attendOption);

            // "불참" 옵션 생성
            VoteOptions absentOption = new VoteOptions(
                    vote.getVoteId(),
                    "불참",
                    2,
                    null,
                    null
            );
            voteOptionsRepository.save(absentOption);
        }

        // 4. VoteResponse로 변환해서 리턴
        return new VoteResponse(
                vote.getVoteId(),
                post.getPostId(),
                vote.getVoteType(),
                vote.getTitle(),
                vote.getDescription(),
                vote.getStatus(),
                vote.getScheduleId()
        );
    }

    /**
     * 투표를 종료합니다. (ATTENDANCE, GENERAL 모두 지원)
     *
     * @param clubId  모임 ID
     * @param voteId  투표 ID
     * @param userId  현재 로그인한 사용자 ID (권한 체크용)
     */
    @Transactional
    public void closeVote(Long clubId, Long voteId, Long userId) {
        Votes vote = votesRepository.findById(voteId)
                .orElseThrow(VoteException.NotFound::new);

        // clubId 검증: 투표가 해당 모임에 속하는지 확인
        Long voteClubId = null;
        if ("ATTENDANCE".equals(vote.getVoteType()) && vote.getScheduleId() != null) {
            Schedules schedule = schedulesRepository.findById(vote.getScheduleId())
                    .orElseThrow(ResourceException.NotFound::new);
            voteClubId = schedule.getClubId();
        } else if ("GENERAL".equals(vote.getVoteType()) && vote.getPostId() != null) {
            Posts post = postRepository.findById(vote.getPostId())
                    .orElseThrow(ResourceException.NotFound::new);
            voteClubId = post.getClub().getClubId();
        }

        if (voteClubId == null || !voteClubId.equals(clubId)) {
            throw new VoteException.ClubMismatch();
        }

        // 이미 종료된 투표인지 확인
        if ("CLOSED".equals(vote.getStatus())) {
            throw new VoteException.AlreadyClosed();
        }

        // 권한 체크
        if ("GENERAL".equals(vote.getVoteType())) {
            // 일반 투표: 만든 사람만 종료 가능
            if (!vote.getCreatorId().equals(userId)) {
                throw new VoteException.CreatorOnly();
            }
        } else if ("ATTENDANCE".equals(vote.getVoteType())) {
            // ATTENDANCE 투표: 모임장 또는 운영진만 종료 가능
            
            // 1. 모임장 확인 (Clubs.ownerId)
            Clubs club = clubsRepository.findById(clubId)
                    .orElseThrow(ResourceException.NotFound::new);
            boolean isOwner = club.getOwnerId().equals(userId);
            
            // 2. 운영진 확인 (ClubMembers.role = "STAFF")
            List<String> roles = clubMembersRepository.findActiveRoles(clubId, userId)
                    .orElseThrow(() -> new VoteException.MemberOnly());
            boolean isStaff = roles.contains("STAFF");
            
            // 3. 모임장 또는 운영진만 허용
            if (!isOwner && !isStaff) {
                throw new VoteException.StaffOnly();
            }
        }

        // 투표 종료
        vote.close();
        votesRepository.save(vote);
    }

    /**
     * 투표에 참여합니다. (ATTENDANCE, GENERAL 모두 지원)
     *
     * @param clubId  모임 ID
     * @param voteId  투표 ID
     * @param userId  현재 로그인한 사용자 ID
     * @param request 선택한 옵션 ID 리스트
     */
    @Transactional
    public void answerVote(Long clubId, Long voteId, Long userId, VoteAnswerRequest request) {
        // 0. 권한 체크: userId가 해당 clubId의 활성 멤버인지 확인
        boolean isActiveMember = clubMembersRepository.existsByClubIdAndUserIdAndStatus(
                clubId, userId, ClubMembers.Status.ACTIVE);
        if (!isActiveMember) {
            throw new VoteException.MemberOnly();
        }

        // 1. 투표 존재 확인
        Votes vote = votesRepository.findById(voteId)
                .orElseThrow(VoteException.NotFound::new);

        // clubId 검증: 투표가 해당 모임에 속하는지 확인
        Long voteClubId = null;
        if ("ATTENDANCE".equals(vote.getVoteType()) && vote.getScheduleId() != null) {
            Schedules schedule = schedulesRepository.findById(vote.getScheduleId())
                    .orElseThrow(ResourceException.NotFound::new);
            voteClubId = schedule.getClubId();
        } else if ("GENERAL".equals(vote.getVoteType()) && vote.getPostId() != null) {
            Posts post = postRepository.findById(vote.getPostId())
                    .orElseThrow(ResourceException.NotFound::new);
            voteClubId = post.getClub().getClubId();
        }

        if (voteClubId == null || !voteClubId.equals(clubId)) {
            throw new VoteException.ClubMismatch();
        }

        // 2. 투표 종료 여부 확인
        if ("CLOSED".equals(vote.getStatus())) {
            throw new VoteException.AlreadyClosed();
        }

        // ATTENDANCE 타입인데 scheduleId가 NULL인 경우 체크
        if ("ATTENDANCE".equals(vote.getVoteType()) && vote.getScheduleId() == null) {
            throw new VoteException.ScheduleIdMissing();
        }

        // 3. 기한 체크 (GENERAL 타입이고 deadline이 설정된 경우)
        if ("GENERAL".equals(vote.getVoteType()) && vote.getDeadline() != null) {
            if (java.time.LocalDateTime.now().isAfter(vote.getDeadline())) {
                throw new VoteException.DeadlinePassed();
            }
        }

        // 4. 옵션 ID 유효성 검증
        List<Long> optionIds = request.optionIds();
        if (optionIds == null || optionIds.isEmpty()) {
            throw new VoteException.OptionRequired();
        }

        // ATTENDANCE 타입은 반드시 1개만 선택 가능
        if ("ATTENDANCE".equals(vote.getVoteType()) && optionIds.size() > 1) {
            throw new VoteException.AttendanceSingleOption();
        }

        // 옵션 ID 중복 제거
        List<Long> uniqueOptionIds = optionIds.stream()
                .distinct()
                .collect(Collectors.toList());

        if (uniqueOptionIds.size() != optionIds.size()) {
            throw new VoteException.OptionDuplicate();
        }

        optionIds = uniqueOptionIds;

        // 옵션이 해당 투표에 속하는지 확인
        List<VoteOptions> validOptions = voteOptionsRepository.findAllById(optionIds);
        boolean allOptionsBelongToVote = validOptions.stream()
                .allMatch(option -> option.getVoteId().equals(voteId));

        if (!allOptionsBelongToVote || validOptions.size() != optionIds.size()) {
            throw new VoteException.OptionInvalid();
        }

        // 5. 복수 선택 허용 여부 체크 (GENERAL 타입만)
        if ("GENERAL".equals(vote.getVoteType()) && !vote.getAllowMultiple() && optionIds.size() > 1) {
            throw new VoteException.MultipleNotAllowed();
        }

        // 6. 기존 투표 기록 확인 (중복 투표 체크)
        List<VoteRecords> existingRecords = voteRecordsRepository.findByVoteIdAndUserId(voteId, userId);

        // ATTENDANCE 타입은 기존 기록이 있으면 업데이트 (참석 → 불참 변경 가능)
        if ("ATTENDANCE".equals(vote.getVoteType())) {
            if (!existingRecords.isEmpty()) {
                // 기존 기록 삭제 (참석 → 불참 변경)
                voteRecordsRepository.deleteAll(existingRecords);
            }
        } else {
            // GENERAL 타입은 allowMultiple이 false면 기존 기록이 있으면 에러
            if (!vote.getAllowMultiple() && !existingRecords.isEmpty()) {
                throw new VoteException.AlreadyParticipated();
            }

            // allowMultiple이 true면 같은 옵션 중복 선택 방지
            List<Long> existingOptionIds = existingRecords.stream()
                    .map(VoteRecords::getOptionId)
                    .collect(Collectors.toList());

            for (Long optionId : optionIds) {
                if (existingOptionIds.contains(optionId)) {
                    throw new VoteException.OptionAlreadySelected();
                }
            }
        }

        // 7. 투표 기록 저장
        List<VoteRecords> newRecords = optionIds.stream()
                .map(optionId -> new VoteRecords(voteId, optionId, userId))
                .collect(Collectors.toList());

        voteRecordsRepository.saveAll(newRecords);
    }
}
