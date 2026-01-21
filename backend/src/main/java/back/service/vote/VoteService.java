package back.service.vote;

import back.domain.*;
import back.domain.club.Clubs;
import back.domain.club.ClubMembers;
import back.domain.post.PostCategory;
import back.domain.post.Posts;
import back.domain.schedule.Schedules;
import back.domain.vote.VoteOptions;
import back.domain.vote.VoteRecords;
import back.domain.vote.Votes;
import back.dto.vote.*;
import back.exception.ResourceException;
import back.exception.VoteException;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import back.repository.post.PostRepository;
import back.repository.schedule.ScheduleRepository;
import back.repository.vote.VoteOptionRepository;
import back.repository.vote.VoteRecordRepository;
import back.repository.vote.VoteRepository;
import back.repository.UserRepository;
import back.service.club.ClubAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final PostRepository postRepository;
    private final ScheduleRepository scheduleRepository;
    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteRecordRepository voteRecordRepository;
    private final ClubMemberRepository clubMembersRepository;
    private final ClubRepository clubsRepository;
    private final ClubAuthService clubsAuthorizationService;

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
        // 1. voteType 검증
        if (request.voteType() == null || 
            (!"GENERAL".equals(request.voteType()) && !"ATTENDANCE".equals(request.voteType()))) {
            throw new VoteException.OptionInvalid(); // voteType이 유효하지 않음
        }

        // 2. title 검증 (null, 빈 문자열, 길이 체크)
        if (request.title() == null || request.title().trim().isEmpty()) {
            throw new VoteException.OptionInvalid(); // title이 유효하지 않음
        }
        if (request.title().length() > 200) {
            throw new VoteException.OptionInvalid(); // title이 200자 초과
        }

        // 3. 권한 체크: ATTENDANCE 타입은 모임장/운영진만, GENERAL 타입은 ACTIVE 멤버만 생성 가능
        if ("ATTENDANCE".equals(request.voteType())) {
            clubsAuthorizationService.assertAtLeastManager(clubId, userId);
        } else {
            clubsAuthorizationService.assertActiveMember(clubId, userId);
        }

        // 4. ATTENDANCE 타입일 때 scheduleId 필수 검증 및 options 불가 검증
        if ("ATTENDANCE".equals(request.voteType())) {
            if (request.scheduleId() == null) {
                throw new VoteException.ScheduleIdRequired();
            }
            // ATTENDANCE 타입은 options를 전달하면 안됨
            if (request.options() != null && !request.options().isEmpty()) {
                throw new VoteException.OptionInvalid(); // ATTENDANCE 타입은 options 사용 불가
            }
        }

        // 5. GENERAL 타입일 때 options 필수 검증 (최소 2개 이상) 및 scheduleId 불가 검증
        if ("GENERAL".equals(request.voteType())) {
            // GENERAL 타입은 scheduleId를 전달하면 안됨
            if (request.scheduleId() != null) {
                throw new VoteException.OptionInvalid(); // GENERAL 타입은 scheduleId 사용 불가
            }
            
            if (request.options() == null || request.options().size() < 2) {
                throw new VoteException.OptionRequired();
            }
            
            // options 리스트에 null이 포함되어 있는지 체크
            if (request.options().stream().anyMatch(option -> option == null)) {
                throw new VoteException.OptionInvalid(); // options에 null 포함
            }
            
            // 각 옵션 검증
            for (VoteOptionCreateRequest option : request.options()) {
                // optionText null 및 빈 문자열 체크 (방어적 코딩)
                if (option.optionText() == null || option.optionText().trim().isEmpty()) {
                    throw new VoteException.OptionInvalid(); // optionText가 null이거나 빈 문자열
                }
                
                // optionText 길이 체크 (200자)
                if (option.optionText().length() > 200) {
                    throw new VoteException.OptionInvalid(); // optionText가 200자 초과
                }
                
                // location 길이 체크 (255자)
                if (option.location() != null && option.location().length() > 255) {
                    throw new VoteException.OptionInvalid(); // location이 255자 초과
                }
                
                // order가 null이거나 음수인지 체크
                if (option.order() == null || option.order() < 0) {
                    throw new VoteException.OptionInvalid(); // order가 null이거나 음수
                }
            }
            
            // deadline이 과거 날짜인지 검증
            if (request.deadline() != null && request.deadline().isBefore(LocalDateTime.now())) {
                throw new VoteException.DeadlinePassed();
            }
            
            // options의 order 중복 체크
            List<Integer> orders = request.options().stream()
                    .map(VoteOptionCreateRequest::order)
                    .filter(order -> order != null)
                    .collect(Collectors.toList());
            long uniqueOrderCount = orders.stream().distinct().count();
            if (orders.size() != uniqueOrderCount) {
                throw new VoteException.OptionDuplicate();
            }
        }

        // ATTENDANCE 타입이고 scheduleId가 있으면 일정 존재 여부 및 모임 소속 확인
        Schedules schedule = null;
        if ("ATTENDANCE".equals(request.voteType()) && request.scheduleId() != null) {
            schedule = scheduleRepository.findById(request.scheduleId())
                    .orElseThrow(ResourceException.NotFound::new);

            // 일정이 해당 모임에 속하는지 확인
            if (!schedule.getClubId().equals(clubId)) {
                throw new VoteException.ClubMismatch();
            }
        }

        Clubs clubRef = clubsRepository.getReferenceById(clubId);
        ClubMembers writerRef = clubMembersRepository.getReferenceById(userId);
        // 이미 조회한 schedule 객체를 재사용 (중복 조회 방지)
        Schedules scheduleRef = schedule;

        // 1. Posts 엔티티 생성 (GENERAL 타입일 때만)
        // ATTENDANCE 타입은 게시글과 무관하므로 postId는 null
        Posts post = null;
        if ("GENERAL".equals(request.voteType())) {
            post = Posts.vote(
                    clubRef,
                    writerRef,
                    null, // GENERAL 타입은 schedule과 무관
                    request.title(),
                    request.description()
            );
            post = postRepository.save(post);
        }

        // 2. Votes 엔티티 생성
        // GENERAL 타입일 때만 deadline 사용, ATTENDANCE 타입은 null
        LocalDateTime deadline = "GENERAL".equals(request.voteType()) ? request.deadline() : null;
        
        Votes vote = new Votes(
                post != null ? post.getPostId() : null, // GENERAL 타입일 때만 postId 설정
                request.voteType(),
                request.scheduleId(),
                userId,
                request.title(),
                request.description(),
                request.isAnonymous(),
                request.allowMultiple(),
                deadline
        );
        vote = voteRepository.save(vote);

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
            voteOptionRepository.save(attendOption);

            // "불참" 옵션 생성
            VoteOptions absentOption = new VoteOptions(
                    vote.getVoteId(),
                    "불참",
                    2,
                    null,
                    null
            );
            voteOptionRepository.save(absentOption);
        }
        
        // 4. GENERAL 타입이면 사용자가 입력한 옵션들 생성
        if ("GENERAL".equals(request.voteType()) && request.options() != null && !request.options().isEmpty()) {
            for (VoteOptionCreateRequest optionRequest : request.options()) {
                VoteOptions option = new VoteOptions(
                        vote.getVoteId(),
                        optionRequest.optionText(),
                        optionRequest.order(),
                        optionRequest.eventDate(),
                        optionRequest.location()
                );
                voteOptionRepository.save(option);
            }
        }

        // 5. VoteResponse로 변환해서 리턴
        return new VoteResponse(
                vote.getVoteId(),
                post != null ? post.getPostId() : null, // GENERAL 타입일 때만 postId 설정
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
        Votes vote = voteRepository.findById(voteId)
                .orElseThrow(VoteException.NotFound::new);

        // clubId 검증: 투표가 해당 모임에 속하는지 확인
        Long voteClubId = null;
        if ("ATTENDANCE".equals(vote.getVoteType()) && vote.getScheduleId() != null) {
            Schedules schedule = scheduleRepository.findById(vote.getScheduleId())
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

            // 2. 운영진 확인 (ClubMembers.role이 STAFF 이상)
            ClubMembers.Role role = clubMembersRepository.findActiveRole(clubId, userId)
                    .orElseThrow(() -> new VoteException.MemberOnly());
            boolean isStaff = role.isAtLeast(ClubMembers.Role.STAFF);

            // 3. 모임장 또는 운영진만 허용
            if (!isOwner && !isStaff) {
                throw new VoteException.StaffOnly();
            }
        }

        // 투표 종료
        vote.close();
        voteRepository.save(vote);
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
        Votes vote = voteRepository.findById(voteId)
                .orElseThrow(VoteException.NotFound::new);

        // clubId 검증: 투표가 해당 모임에 속하는지 확인
        Long voteClubId = null;
        if ("ATTENDANCE".equals(vote.getVoteType()) && vote.getScheduleId() != null) {
            Schedules schedule = scheduleRepository.findById(vote.getScheduleId())
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
        List<VoteOptions> validOptions = voteOptionRepository.findAllById(optionIds);
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
        List<VoteRecords> existingRecords = voteRecordRepository.findByVoteIdAndUserId(voteId, userId);

        // ATTENDANCE 타입은 기존 기록이 있으면 업데이트 (참석 → 불참 변경 가능)
        if ("ATTENDANCE".equals(vote.getVoteType())) {
            if (!existingRecords.isEmpty()) {
                // 기존 기록 삭제 (참석 → 불참 변경)
                voteRecordRepository.deleteAll(existingRecords);
            }
        } else {
            // GENERAL 타입
            if (!vote.getAllowMultiple()) {
                // allowMultiple이 false면 기존 기록이 있으면 삭제 (투표 변경 허용)
                if (!existingRecords.isEmpty()) {
                    voteRecordRepository.deleteAll(existingRecords);
                }
            } else {
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
        }

        // 7. 투표 기록 저장
        List<VoteRecords> newRecords = optionIds.stream()
                .map(optionId -> new VoteRecords(voteId, optionId, userId))
                .collect(Collectors.toList());

        voteRecordRepository.saveAll(newRecords);
    }

    /**
     * 투표 상세 정보를 조회합니다.
     *
     * @param clubId 모임 ID
     * @param voteId 투표 ID
     * @param userId 현재 로그인한 사용자 ID
     * @return 투표 상세 정보
     */
    @Transactional(readOnly = true)
    public VoteDetailResponse getVoteById(Long clubId, Long voteId, Long userId) {
        // 권한 체크: ACTIVE 멤버만 조회 가능
        clubsAuthorizationService.assertActiveMember(clubId, userId);

        Votes vote = voteRepository.findById(voteId)
                .orElseThrow(VoteException.NotFound::new);

        // 투표가 해당 모임에 속하는지 확인
        Long voteClubId = getVoteClubId(vote);
        if (voteClubId == null || !voteClubId.equals(clubId)) {
            throw new VoteException.ClubMismatch();
        }

        // 투표 옵션 조회
        List<VoteOptions> options = voteOptionRepository.findByVoteIdOrderByOptionOrderAsc(voteId);

        // 각 옵션별 투표 수 조회
        List<VoteOptionResponse> optionResponses = options.stream()
                .map(option -> new VoteOptionResponse(
                        option.getOptionId(),
                        option.getOptionText(),
                        option.getOptionOrder(),
                        option.getEventDate(),
                        option.getLocation(),
                        voteRecordRepository.countByOptionId(option.getOptionId())
                ))
                .collect(Collectors.toList());

        // 현재 사용자가 선택한 옵션 조회
        List<Long> mySelectedOptionIds = voteRecordRepository.findByVoteIdAndUserId(voteId, userId).stream()
                .map(VoteRecords::getOptionId)
                .collect(Collectors.toList());

        return new VoteDetailResponse(
                vote.getVoteId(),
                vote.getPostId(),
                vote.getVoteType(),
                vote.getScheduleId(),
                vote.getCreatorId(),
                vote.getTitle(),
                vote.getDescription(),
                vote.getIsAnonymous(),
                vote.getAllowMultiple(),
                vote.getStatus(),
                vote.getDeadline(),
                vote.getClosedAt(),
                vote.getCreatedAt(),
                vote.getUpdatedAt(),
                optionResponses,
                mySelectedOptionIds
        );
    }

    /**
     * 모임에 속한 전체 투표 목록을 조회합니다.
     *
     * @param clubId 모임 ID
     * @param userId 현재 로그인한 사용자 ID
     * @return 투표 목록
     */
    @Transactional(readOnly = true)
    public List<VoteListResponse> getVotesByClubId(Long clubId, Long userId) {
        // 권한 체크: ACTIVE 멤버만 조회 가능
        clubsAuthorizationService.assertActiveMember(clubId, userId);

        // 방법 1: 해당 모임의 Posts에서 VOTE 카테고리 게시글 조회 후 Votes 조회
        List<Posts> votePosts = postRepository.findByClub_ClubIdAndCategoryAndDeletedAtIsNull(clubId, PostCategory.VOTE);
        List<Long> postIds = votePosts.stream()
                .map(Posts::getPostId)
                .collect(Collectors.toList());

        // 방법 2: 해당 모임의 Schedules에서 ATTENDANCE 투표 조회
        List<Schedules> schedules = scheduleRepository.findByClubId(clubId);
        List<Long> scheduleIds = schedules.stream()
                .map(Schedules::getScheduleId)
                .collect(Collectors.toList());

        // GENERAL 타입 투표 (postId 기반)
        List<Votes> generalVotes = postIds.isEmpty()
                ? new ArrayList<>()
                : voteRepository.findByPostIdIn(postIds);

        // ATTENDANCE 타입 투표 (scheduleId 기반)
        List<Votes> attendanceVotes = scheduleIds.isEmpty()
                ? new ArrayList<>()
                : voteRepository.findByScheduleIdIn(scheduleIds);

        // 모든 투표 합치기 (중복 제거)
        List<Votes> allVotes = new ArrayList<>();
        allVotes.addAll(generalVotes);
        for (Votes attendanceVote : attendanceVotes) {
            boolean alreadyExists = allVotes.stream()
                    .anyMatch(v -> v.getVoteId().equals(attendanceVote.getVoteId()));
            if (!alreadyExists) {
                allVotes.add(attendanceVote);
            }
        }

        // VoteListResponse로 변환
        return allVotes.stream()
                .map(vote -> new VoteListResponse(
                        vote.getVoteId(),
                        vote.getPostId(),
                        vote.getVoteType(),
                        vote.getScheduleId(),
                        vote.getTitle(),
                        vote.getStatus(),
                        vote.getDeadline(),
                        vote.getClosedAt(),
                        vote.getCreatedAt(),
                        voteRecordRepository.countDistinctClubMembersByVoteId(vote.getVoteId())
                ))
                .collect(Collectors.toList());
    }

    /**
     * 투표가 속한 모임 ID를 반환합니다.
     */
    private Long getVoteClubId(Votes vote) {
        if ("ATTENDANCE".equals(vote.getVoteType()) && vote.getScheduleId() != null) {
            return scheduleRepository.findById(vote.getScheduleId())
                    .map(Schedules::getClubId)
                    .orElse(null);
        } else if ("GENERAL".equals(vote.getVoteType()) && vote.getPostId() != null) {
            return postRepository.findById(vote.getPostId())
                    .map(post -> post.getClub().getClubId())
                    .orElse(null);
        }
        return null;
    }
}
