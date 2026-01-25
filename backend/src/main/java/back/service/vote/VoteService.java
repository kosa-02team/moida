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
import back.repository.schedule.ScheduleParticipantRepository;
import back.repository.vote.VoteOptionRepository;
import back.repository.vote.VoteRecordRepository;
import back.repository.vote.VoteRepository;
import back.repository.UserRepository;
import back.domain.schedule.ScheduleParticipants;
import back.service.club.ClubAuthService;
import back.service.ledger.EventFundService;
import back.service.ledger.TransactionMatchingService;
import back.repository.ledger.PaymentRequestRepository;
import back.domain.ledger.PaymentRequest;
import back.domain.Users;
import back.bank.service.BankService;
import back.bank.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final PostRepository postRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteRecordRepository voteRecordRepository;
    private final ClubMemberRepository clubMembersRepository;
    private final ClubRepository clubsRepository;
    private final ClubAuthService clubsAuthorizationService;
    private final EventFundService eventFundService;
    private final PaymentRequestRepository paymentRequestRepository;
    private final UserRepository userRepository;
    private final TransactionMatchingService transactionMatchingService;
    private final BankService bankService;
    private final BankAccountRepository bankAccountRepository;

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
        // 참고: PostService.createStory()에서 투표 게시글을 생성할 때는 게시글이 이미 생성되어 있으므로
        // VoteService.createVote()를 직접 호출하지 않고 PostService 내부에서 투표를 생성함
        Posts post = null;
        if ("GENERAL".equals(request.voteType())) {
            post = Posts.vote(
                    clubRef,
                    writerRef,
                    null, // GENERAL 타입은 schedule과 무관
                    request.title(),
                    request.description());
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
                deadline);
        vote = voteRepository.save(vote);

        // 3. ATTENDANCE 타입이면 VoteOptions 자동 생성 (참석/불참)
        if ("ATTENDANCE".equals(request.voteType()) && schedule != null) {
            // "참석" 옵션 생성
            VoteOptions attendOption = new VoteOptions(
                    vote.getVoteId(),
                    "참석",
                    1,
                    schedule.getEventDate(),
                    schedule.getLocation());
            voteOptionRepository.save(attendOption);

            // "불참" 옵션 생성
            VoteOptions absentOption = new VoteOptions(
                    vote.getVoteId(),
                    "불참",
                    2,
                    null,
                    null);
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
                        optionRequest.location());
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
                vote.getScheduleId());
    }

    /**
     * 투표를 종료합니다. (ATTENDANCE, GENERAL 모두 지원)
     *
     * @param clubId 모임 ID
     * @param voteId 투표 ID
     * @param userId 현재 로그인한 사용자 ID (권한 체크용)
     */
    @Transactional
    public void closeVote(Long clubId, Long voteId, Long userId) {
        Votes vote = voteRepository.findById(voteId)
                .orElseThrow(VoteException.NotFound::new);

        // clubId 검증: 투표가 해당 모임에 속하는지 확인
        Long voteClubId = null;
        if ("ATTENDANCE".equals(vote.getVoteType()) && vote.getScheduleId() != null) {
            // ATTENDANCE 투표 마감 시 일정은 마감하지 않음
            // 일정 마감은 "일정 마무리" 기능에서만 수행
            Schedules schedule = scheduleRepository.findById(vote.getScheduleId())
                    .orElseThrow(ResourceException.NotFound::new);
            voteClubId = schedule.getClubId();

            // 일정이 이미 마감되어 있지 않은지 확인 (방어적 프로그래밍)
            if ("CLOSED".equals(schedule.getStatus())) {
                org.slf4j.LoggerFactory.getLogger(VoteService.class)
                        .warn("ATTENDANCE 투표 마감 시 일정이 이미 CLOSED 상태입니다. scheduleId={}", vote.getScheduleId());
            }
            // 일정은 OPEN 상태로 유지 (투표만 마감)
        } else if ("GENERAL".equals(vote.getVoteType()) && vote.getPostId() != null) {
            Posts post = postRepository.findByIdWithClub(vote.getPostId())
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

        // ATTENDANCE 투표 마감 시 투표 결과를 기반으로 참석자 상태 업데이트 및 참가비 요청 생성
        if ("ATTENDANCE".equals(vote.getVoteType()) && vote.getScheduleId() != null) {
            System.out.println(
                    "🗳️ [투표 종료] ATTENDANCE 투표 마감 시작: voteId=" + voteId + ", scheduleId=" + vote.getScheduleId());

            Schedules schedule = scheduleRepository.findById(vote.getScheduleId())
                    .orElse(null);

            if (schedule != null) {
                BigDecimal entryFee = schedule.getEntryFee();
                System.out.println("  → 일정 조회 성공: entryFee=" + entryFee);

                // 투표 결과를 기반으로 참석자 상태 업데이트
                updateParticipantsFromVoteResults(vote.getVoteId(), vote.getScheduleId());

                // 참가비가 있고, 0보다 큰 경우에만 참가비 요청 생성
                if (entryFee != null && entryFee.compareTo(BigDecimal.ZERO) > 0) {
                    System.out.println("  → 참가비 요청 생성 시도: clubId=" + clubId + ", scheduleId=" + vote.getScheduleId()
                            + ", userId=" + userId);
                    try {
                        // 투표 마감 시에는 권한 체크를 우회하고 직접 참가비 요청 생성
                        createPaymentRequestsFromVoteResults(clubId, vote.getVoteId(), vote.getScheduleId(), entryFee);
                        System.out.println("  ✓ 참가비 요청 생성 완료");
                    } catch (Exception e) {
                        // 참가비 요청 생성 실패 시 로깅만 하고 계속 진행
                        System.err.println("  ❌ 참가비 요청 생성 실패: clubId=" + clubId + ", scheduleId=" + vote.getScheduleId()
                                + ", error=" + e.getMessage());
                        e.printStackTrace();
                        org.slf4j.LoggerFactory.getLogger(VoteService.class)
                                .warn("ATTENDANCE 투표 마감 시 참가비 요청 생성 실패: clubId={}, scheduleId={}, error={}",
                                        clubId, vote.getScheduleId(), e.getMessage(), e);
                    }
                } else {
                    System.out.println("  → 참가비가 없거나 0원이므로 요청 생성 안 함: entryFee=" + entryFee);
                }
            } else {
                System.out.println("  ❌ 일정을 찾을 수 없음: scheduleId=" + vote.getScheduleId());
            }
        }
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
            Posts post = postRepository.findByIdWithClub(vote.getPostId())
                    .orElseThrow(ResourceException.NotFound::new);
            voteClubId = post.getClub().getClubId();
        }

        if (voteClubId == null || !voteClubId.equals(clubId)) {
            throw new VoteException.ClubMismatch();
        }

        // 2. 투표 종료 여부 확인
        // 투표가 마감되었지만 운영진 이상 권한이 있으면 투표 가능 (뒤늦게 참석하는 사람 추가)
        if ("CLOSED".equals(vote.getStatus())) {
            boolean hasManagerPermission = false;
            try {
                clubsAuthorizationService.assertAtLeastManager(clubId, userId);
                hasManagerPermission = true;
            } catch (back.exception.ClubException e) {
                // 운영진 권한이 없음 - ClubException의 모든 하위 예외 처리
                hasManagerPermission = false;
            }

            if (!hasManagerPermission) {
                throw new VoteException.AlreadyClosed();
            }
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
        if (optionIds == null) {
            throw new VoteException.OptionRequired();
        }

        // 빈 배열인 경우 투표 취소 처리
        if (optionIds.isEmpty()) {
            // 기존 투표 기록 삭제 (투표 취소)
            List<VoteRecords> existingRecords = voteRecordRepository.findByVoteIdAndUserId(voteId, userId);
            if (!existingRecords.isEmpty()) {
                voteRecordRepository.deleteAll(existingRecords);
            }

            // ATTENDANCE 타입인 경우 ScheduleParticipants도 삭제
            if ("ATTENDANCE".equals(vote.getVoteType()) && vote.getScheduleId() != null) {
                Optional<ScheduleParticipants> participant = scheduleParticipantRepository
                        .findByScheduleIdAndUserId(vote.getScheduleId(), userId);
                participant.ifPresent(scheduleParticipantRepository::delete);
            }

            return; // 투표 취소 완료
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

        // 기존 기록이 있으면 삭제 (투표 변경 허용)
        // ATTENDANCE, GENERAL 타입 모두 투표 변경 시 기존 기록 삭제 후 새로 저장
        if (!existingRecords.isEmpty()) {
            voteRecordRepository.deleteAll(existingRecords);
            // flush()를 호출하여 DELETE가 INSERT보다 먼저 실행되도록 보장
            // 이렇게 하지 않으면 JPA가 작업 순서를 재정렬하여 UniqueConstraint 위반 발생 가능
            voteRecordRepository.flush();
        }

        // 7. 투표 기록 저장
        List<VoteRecords> newRecords = optionIds.stream()
                .map(optionId -> new VoteRecords(voteId, optionId, userId))
                .collect(Collectors.toList());

        voteRecordRepository.saveAll(newRecords);

        // 8. ATTENDANCE 타입 투표인 경우 ScheduleParticipants 자동 생성/업데이트
        if ("ATTENDANCE".equals(vote.getVoteType()) && vote.getScheduleId() != null) {
            Long scheduleId = vote.getScheduleId();

            // 선택된 옵션 확인 (참석 또는 불참)
            VoteOptions selectedOption = validOptions.get(0); // ATTENDANCE는 항상 1개만 선택
            String optionText = selectedOption.getOptionText();

            // ScheduleParticipants 조회 또는 생성
            ScheduleParticipants participant = scheduleParticipantRepository
                    .findByScheduleIdAndUserId(scheduleId, userId)
                    .orElseGet(() -> {
                        ScheduleParticipants newParticipant = new ScheduleParticipants(scheduleId, userId);
                        scheduleParticipantRepository.save(newParticipant);
                        return newParticipant;
                    });

            // 참석 상태 업데이트
            if ("참석".equals(optionText) || optionText.contains("참석")) {
                participant.attend();
            } else if ("불참".equals(optionText) || optionText.contains("불참")) {
                participant.notAttend();
            } else {
                participant.undecided();
            }

            scheduleParticipantRepository.save(participant);
        }
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
                        voteRecordRepository.countByOptionId(option.getOptionId())))
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
                mySelectedOptionIds);
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
        List<Posts> votePosts = postRepository.findByClub_ClubIdAndCategoryAndDeletedAtIsNull(clubId,
                PostCategory.VOTE);
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
                        voteRecordRepository.countDistinctClubMembersByVoteId(vote.getVoteId())))
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
            return postRepository.findByIdWithClub(vote.getPostId())
                    .map(post -> post.getClub().getClubId())
                    .orElse(null);
        }
        return null;
    }

    /**
     * 투표 결과를 기반으로 참석자 상태를 업데이트합니다.
     * 투표 마감 시 "참석" 옵션을 선택한 사용자들을 ATTENDING 상태로 설정합니다.
     */
    @Transactional
    private void updateParticipantsFromVoteResults(Long voteId, Long scheduleId) {
        System.out.println("🔄 [투표 결과 기반 참석자 업데이트] voteId=" + voteId + ", scheduleId=" + scheduleId);

        // 투표의 모든 옵션 조회
        List<VoteOptions> options = voteOptionRepository.findByVoteIdOrderByOptionOrderAsc(voteId);

        // 1. "참석" 옵션 처리
        VoteOptions attendOption = options.stream()
                .filter(opt -> "참석".equals(opt.getOptionText()) || opt.getOptionText().contains("참석"))
                .findFirst()
                .orElse(null);

        if (attendOption != null) {
            System.out.println("  → '참석' 옵션 찾음: optionId=" + attendOption.getOptionId());
            List<VoteRecords> attendRecords = voteRecordRepository.findByOptionId(attendOption.getOptionId());

            for (VoteRecords record : attendRecords) {
                updateParticipantStatus(scheduleId, record.getUserId(), "ATTENDING");
            }
        }

        // 2. "불참" 옵션 처리
        VoteOptions absentOption = options.stream()
                .filter(opt -> "불참".equals(opt.getOptionText()) || opt.getOptionText().contains("불참"))
                .findFirst()
                .orElse(null);

        if (absentOption != null) {
            System.out.println("  → '불참' 옵션 찾음: optionId=" + absentOption.getOptionId());
            List<VoteRecords> absentRecords = voteRecordRepository.findByOptionId(absentOption.getOptionId());

            for (VoteRecords record : absentRecords) {
                updateParticipantStatus(scheduleId, record.getUserId(), "NOT_ATTENDING");
            }
        }

        System.out.println("  ✓ 참석자/불참자 상태 업데이트 완료");
    }

    private void updateParticipantStatus(Long scheduleId, Long userId, String status) {
        ScheduleParticipants participant = scheduleParticipantRepository
                .findByScheduleIdAndUserId(scheduleId, userId)
                .orElseGet(() -> {
                    ScheduleParticipants newParticipant = new ScheduleParticipants(scheduleId, userId);
                    return scheduleParticipantRepository.save(newParticipant);
                });

        if ("ATTENDING".equals(status)) {
            participant.attend();
        } else if ("NOT_ATTENDING".equals(status)) {
            participant.notAttend();
        }
        scheduleParticipantRepository.save(participant);
    }

    /**
     * 투표 결과를 기반으로 참가비 요청을 생성합니다.
     * 투표 마감 시 "참석" 옵션을 선택한 사용자들에 대해 PaymentRequest를 생성합니다.
     */
    @Transactional
    private void createPaymentRequestsFromVoteResults(Long clubId, Long voteId, Long scheduleId, BigDecimal entryFee) {
        System.out.println(
                "💰 [투표 결과 기반 참가비 요청 생성] clubId=" + clubId + ", voteId=" + voteId + ", scheduleId=" + scheduleId);

        // 투표의 모든 옵션 조회
        List<VoteOptions> options = voteOptionRepository.findByVoteIdOrderByOptionOrderAsc(voteId);

        // "참석" 옵션 찾기
        VoteOptions attendOption = options.stream()
                .filter(opt -> "참석".equals(opt.getOptionText()) || opt.getOptionText().contains("참석"))
                .findFirst()
                .orElse(null);

        if (attendOption == null) {
            System.out.println("  ⚠️ '참석' 옵션을 찾을 수 없음");
            return;
        }

        System.out.println("  → '참석' 옵션 찾음: optionId=" + attendOption.getOptionId());

        // "참석" 옵션을 선택한 사용자 목록 조회
        List<VoteRecords> attendRecords = voteRecordRepository.findByOptionId(attendOption.getOptionId());
        System.out.println("  → '참석' 옵션을 선택한 사용자 수: " + attendRecords.size() + "명");

        if (attendRecords.isEmpty()) {
            System.out.println("  ⚠️ 참석자가 없어서 요청 생성 안 함");
            return;
        }

        // 일정 정보 조회
        Schedules schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ResourceException.NotFound::new);

        // 사용자 정보 조회
        List<Long> userIds = attendRecords.stream()
                .map(VoteRecords::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Users> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(Users::getUserId, user -> user));

        LocalDate expectedDate = schedule.getEventDate().toLocalDate();
        int createdCount = 0;

        // 각 참석자에 대해 PaymentRequest 생성
        for (VoteRecords record : attendRecords) {
            Long userId = record.getUserId();

            // userId를 club_members.member_id로 변환
            Long memberId = clubMembersRepository.findByClubIdAndUserIdAndStatus(
                    clubId, userId, back.domain.club.ClubMembers.Status.ACTIVE)
                    .map(back.domain.club.ClubMembers::getMemberId)
                    .orElse(null);

            if (memberId == null) {
                System.out.println("  ⚠️ userId=" + userId + "는 활성 멤버가 아니므로 스킵");
                continue;
            }

            // 이미 요청이 생성되었는지 확인
            boolean alreadyRequested = paymentRequestRepository.existsByScheduleIdAndMemberId(
                    scheduleId, memberId);
            if (alreadyRequested) {
                System.out.println("  ⚠️ memberId=" + memberId + "는 이미 요청이 생성되어 있음");
                continue;
            }

            Users user = userMap.get(userId);
            String realName = (user != null) ? user.getRealName() : "알수없음";

            PaymentRequest req = new PaymentRequest(
                    clubId,
                    memberId,
                    realName,
                    PaymentRequest.RequestType.DEPOSIT,
                    entryFee,
                    expectedDate, // 일정 날짜로 설정
                    10, // ±10일 범위
                    schedule.getEventDate().plusDays(14),
                    scheduleId,
                    null);

            PaymentRequest savedReq = paymentRequestRepository.save(req);
            createdCount++;

            System.out.println("  ✓ 참가비 요청 생성: requestId=" + savedReq.getRequestId() +
                    ", memberName=" + realName + ", amount=" + entryFee +
                    ", expectedDate=" + expectedDate);
        }

        System.out.println("  ✓ 참가비 요청 생성 완료: 총 " + createdCount + "건");

        // 생성된 요청들을 기존 미매칭 거래내역과 매칭 시도
        if (createdCount > 0) {
            try {
                // 은행 거래내역 동기화 (입금 내역 조회)
                // 은행 계좌가 있는 경우에만 동기화 시도
                try {
                    if (bankAccountRepository.findByClubId(clubId).isPresent()) {
                        bankService.syncTransactionsStub(clubId, 1L, null, null);
                    }
                } catch (Exception e) {
                    System.err.println("  ⚠️ 은행 동기화 실패: " + e.getMessage());
                }

                // 새로 생성된 요청들을 기존 미매칭 거래내역과 매칭 시도
                List<PaymentRequest> newRequests = paymentRequestRepository.findByScheduleId(scheduleId)
                        .stream()
                        .filter(r -> r.getStatus() == PaymentRequest.RequestStatus.PENDING)
                        .collect(Collectors.toList());

                if (!newRequests.isEmpty()) {
                    transactionMatchingService.matchRequestsWithExistingTransactions(clubId, newRequests);
                    System.out.println("  ✓ 기존 거래내역과 매칭 시도 완료");
                }
            } catch (Exception e) {
                System.err.println("  ⚠️ 매칭 시도 실패: " + e.getMessage());
                // 매칭 실패해도 요청은 생성되었으므로 계속 진행
            }
        }
    }
}
