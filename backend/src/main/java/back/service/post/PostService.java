package back.service.post;

import back.domain.club.ClubMembers;
import back.domain.club.Clubs;
import back.domain.schedule.Schedules;
import back.domain.post.PostImages;
import back.domain.post.PostMemberTags;
import back.domain.post.Posts;
import back.dto.post.post.response.PostCardBase;
import back.dto.post.post.response.PostCardResponse;
import back.dto.post.post.response.PostIdResponse;
import back.dto.post.story.request.StoryCreateRequest;
import back.dto.post.story.request.StoryUpdateRequest;
import back.dto.post.story.response.*;
import back.exception.PostsException;
import back.repository.club.ClubMemberRepository;
import back.repository.schedule.ScheduleRepository;
import back.repository.club.ClubRepository;
import back.repository.post.PostImageRepository;
import back.repository.post.PostLikeRepository;
import back.repository.post.PostMemberTagRepository;
import back.repository.post.PostRepository;
import back.repository.post.projection.RecentAlbumRow;
import back.service.club.ClubAuthService;
import back.service.post.ai.PostVectorService;
import back.domain.vote.Votes;
import back.domain.vote.VoteOptions;
import back.dto.vote.VoteOptionCreateRequest;
import back.exception.VoteException;
import back.repository.vote.VoteRepository;
import back.repository.vote.VoteOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PostService {

    private final ClubAuthService clubAuthorizationService;

    private final ClubRepository clubsRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ScheduleRepository scheduleRepository;

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostMemberTagRepository postMemberTagRepository;
    private final PostLikeRepository postLikeRepository;
    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;

    private final Optional<PostVectorService> postVectorService;
    private final ImageService imageService;
    // 알림 전송을 위해 추가
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public PostIdResponse createStory(Long clubId, Long userId, StoryCreateRequest request) {
        try {
            clubAuthorizationService.assertActiveMember(clubId, userId);

            // userId를 memberId로 변환 (posts.writer_id는 club_members.member_id를 참조)
            ClubMembers writer = clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 모임의 멤버를 찾을 수 없습니다."));
            Long writerId = writer.getMemberId();

            // 투표 게시글인지 확인 (voteOptions가 있으면 투표 게시글)
            boolean isVotePost = request.voteOptions() != null && !request.voteOptions().isEmpty();

            // 투표 게시글일 때는 title 필수, 일반 게시글일 때는 content 필수
            if (isVotePost) {
                if (request.title() == null || request.title().trim().isEmpty()) {
                    throw new VoteException.OptionInvalid(); // title이 필수
                }
            } else {
                if (request.content() == null || request.content().trim().isEmpty()) {
                    throw new IllegalArgumentException("content는 필수입니다."); // content가 필수
                }
            }

            Posts saved;
            if (isVotePost) {
                // 투표 게시글 생성
                saved = postRepository.save(buildVotePost(clubId, writerId, request));

                // 투표 생성
                createVoteForPost(clubId, userId, saved, request);
            } else {
                // 일반 게시글 생성

                saved = postRepository.save(buildStoryPost(clubId, writerId, request));
                applyOptionalUpdatesOnCreate(saved, request);
            }

            // 벡터 서비스 호출 (실패해도 게시글 생성은 성공)
            postVectorService.ifPresent(service -> {
                try {
                    service.savePost(saved);
                } catch (Exception e) {
                    // 벡터 DB 연결 실패 등으로 인한 에러는 로그만 남기고 게시글 생성은 계속 진행
                    log.warn("벡터 서비스 저장 실패 (게시글 생성은 성공): {}", e.getMessage());
                }
            });

            // 알림 이벤트 발행
            eventPublisher.publishEvent(new back.event.PostCreatedEvent(
                    clubId,
                    saved.getPostId(),
                    saved.getContent() != null ? saved.getContent()
                            : (saved.getTitle() != null ? saved.getTitle() : ""),
                    userId));

            return PostIdResponse.from(saved);
        } catch (Exception e) {
            log.error("createStory 실패: clubId={}, userId={}, error={}", clubId, userId, e.getMessage(), e);
            throw e;
        }
    }

    public PostDetailResponse getPost(Long clubId, Long postId, Long viewerId) {
        clubAuthorizationService.validateAndGetClubForReadPosts(clubId, viewerId);

        Posts post = getActivePostOrThrow(postId, clubId);

        // 게시글 이미지 조회
        List<String> imagesUrl = postImageRepository.findByPostIdIn(List.of(postId)).stream()
                .map(PostImages::getImageUrl)
                .toList();

        // 좋아요 수 및 좋아요 여부 조회
        Long postLikes = postLikeRepository.countByPostId(postId);
        Boolean isLiked = postLikeRepository.existsByPostIdAndUserId(postId, viewerId);

        return PostDetailResponse.from(post, imagesUrl, postLikes, isLiked);
    }

    // 스토리 페이지에 게시글 박스
    public List<PostCardResponse> getRecentPosts(Long clubId, Long viewerId, Pageable pageable) {
        clubAuthorizationService.validateAndGetClubForReadPosts(clubId, viewerId);

        Page<PostCardBase> page = postRepository.findPostCards(clubId, pageable);

        List<Long> postIds = page.getContent().stream()
                .map(PostCardBase::postId)
                .toList();

        Map<Long, List<String>> imageMap = postIds.isEmpty()
                ? Map.of()
                : postImageRepository.findByPostIdIn(postIds).stream()
                        .collect(Collectors.groupingBy(
                                PostImages::getPostId,
                                Collectors.mapping(PostImages::getImageUrl, Collectors.toList())));

        // viewerId가 있으면 좋아요 여부 조회, 없으면 모두 false
        Map<Long, Boolean> likedMap = Map.of();
        if (viewerId != null && !postIds.isEmpty()) {
            likedMap = postIds.stream()
                    .collect(Collectors.toMap(
                            postId -> postId,
                            postId -> postLikeRepository.existsByPostIdAndUserId(postId, viewerId)));
        }

        final Map<Long, Boolean> finalLikedMap = likedMap;
        return page.getContent().stream()
                .map(p -> PostCardResponse.of(
                        p,
                        imageMap.getOrDefault(p.postId(), List.of()),
                        finalLikedMap.getOrDefault(p.postId(), false)))
                .toList();
    }

    // 스토리 페이지에 앨범 박스
    public List<AlbumCardResponse> getRecentAlbums(Long clubId, Long viewerId, int limit) {
        clubAuthorizationService.validateAndGetClubForReadPosts(clubId, viewerId);

        List<RecentAlbumRow> rows = postRepository.findRecentAlbumRows(
                clubId, PageRequest.of(0, limit));
        if (rows.isEmpty())
            return List.of();

        List<Long> scheduleIds = rows.stream()
                .map(RecentAlbumRow::getScheduleId)
                .toList();

        List<PostImages> images = postImageRepository.findImagesForSchedules(clubId, scheduleIds);

        Map<Long, List<PostImages>> imageMap = images.stream()
                .collect(Collectors.groupingBy(pi -> pi.getPost().getSchedule().getScheduleId()));

        List<AlbumCardResponse> result = new ArrayList<>();

        for (RecentAlbumRow r : rows) {
            List<PostImages> list = imageMap.getOrDefault(r.getScheduleId(), List.of());
            if (list.isEmpty())
                continue;

            PostImages cover = list.getFirst(); // createdAt desc 기준 1장

            result.add(new AlbumCardResponse(
                    clubId,
                    cover.getPost().getPostId(),
                    r.getScheduleId(),
                    r.getScheduleName(),
                    cover.getImageUrl(),
                    list.size(),
                    r.getLastCreatedAt()));
        }

        return result;
    }

    @Transactional
    public PostIdResponse updatePost(Long clubId, Long postId, Long actorId, StoryUpdateRequest request) {
        Posts post = getActivePostOrThrow(postId, clubId);

        // 작성자면 OK, 아니면 운영진 이상
        assertCanManagePost(clubId, post, actorId);

        applyStoryUpdates(post, request);
        applyMediaUpdatesOnUpdate(post, request);

        return PostIdResponse.from(post);
    }

    @Transactional
    public void blindPost(Long clubId, Long postId, Long actorId) {
        Posts post = getPostOrThrow(postId, clubId);

        if (post.getDeletedAt() != null) {
            return; // 기존 동작 유지(멱등)
        }

        assertCanManagePost(clubId, post, actorId);

        post.blindPost(actorId);
    }

    @Transactional
    public void deletePost(Long clubId, Long postId, Long actorId) {
        Posts post = getPostOrThrow(postId, clubId);

        if (post.getDeletedAt() != null) {
            return; // 멱등
        }

        assertCanManagePost(clubId, post, actorId);

        post.delete();
    }

    // ====== private helpers ======

    private Posts buildStoryPost(Long clubId, Long writerId, StoryCreateRequest request) {
        Clubs clubRef = clubsRepository.getReferenceById(clubId);
        ClubMembers writerRef = clubMemberRepository.getReferenceById(writerId);
        Schedules scheduleRef = getScheduleRefOrNull(request.scheduleId());

        return Posts.story(clubRef, writerRef, scheduleRef, request.content());
    }

    private Posts buildVotePost(Long clubId, Long writerId, StoryCreateRequest request) {
        Clubs clubRef = clubsRepository.getReferenceById(clubId);
        ClubMembers writerRef = clubMemberRepository.getReferenceById(writerId);
        // 일반 투표 게시글은 일정과 연결하지 않음
        Schedules scheduleRef = null;

        // 투표 게시글: title은 request.title(), description은 request.content()
        String voteTitle = request.title() != null && !request.title().trim().isEmpty()
                ? request.title().trim()
                : "";
        String voteDescription = request.content() != null ? request.content().trim() : null;

        return Posts.vote(clubRef, writerRef, scheduleRef, voteTitle, voteDescription);
    }

    private void createVoteForPost(Long clubId, Long userId, Posts post, StoryCreateRequest request) {
        // 투표 옵션 검증
        if (request.voteOptions() == null || request.voteOptions().size() < 2) {
            throw new VoteException.OptionRequired();
        }

        // 투표 엔티티 생성
        LocalDateTime deadline = request.voteDeadline();
        Boolean isAnonymous = request.isAnonymous() != null ? request.isAnonymous() : false;
        Boolean allowMultiple = request.allowMultiple() != null ? request.allowMultiple() : false;

        // title이 null이거나 빈 문자열이면 안 됨 (Votes 엔티티의 title은 nullable = false)
        String voteTitle = post.getTitle();
        if (voteTitle == null || voteTitle.trim().isEmpty()) {
            voteTitle = request.title() != null && !request.title().trim().isEmpty()
                    ? request.title().trim()
                    : "투표";
        }

        Votes vote = new Votes(
                post.getPostId(),
                "GENERAL",
                null, // scheduleId는 null (GENERAL 타입)
                userId, // creatorId는 user_id를 참조
                voteTitle,
                post.getContent(),
                isAnonymous,
                allowMultiple,
                deadline);
        Votes savedVote = voteRepository.save(vote);

        // 투표 옵션 생성
        for (VoteOptionCreateRequest optionRequest : request.voteOptions()) {
            if (optionRequest.optionText() == null || optionRequest.optionText().trim().isEmpty()) {
                continue; // 빈 옵션은 건너뛰기
            }
            VoteOptions option = new VoteOptions(
                    savedVote.getVoteId(),
                    optionRequest.optionText().trim(),
                    optionRequest.order(),
                    optionRequest.eventDate(),
                    optionRequest.location());
            voteOptionRepository.save(option);
        }
    }

    private Schedules getScheduleRefOrNull(Long scheduleId) {
        return (scheduleId == null) ? null : scheduleRepository.getReferenceById(scheduleId);
    }

    private Posts getPostOrThrow(Long postId, Long clubId) {
        return postRepository.findByPostIdAndClub_ClubId(postId, clubId)
                .orElseThrow(PostsException.PostNotFound::new);
    }

    private Posts getActivePostOrThrow(Long postId, Long clubId) {
        Posts post = getPostOrThrow(postId, clubId);
        if (post.getDeletedAt() != null) {
            throw new PostsException.Deleted();
        }
        return post;
    }

    private void assertCanManagePost(Long clubId, Posts post, Long actorId) {
        boolean isWriter = post.getWriter().getUserId().equals(actorId);
        if (!isWriter) {
            clubAuthorizationService.assertAtLeastManager(clubId, actorId);
        }
    }

    private void applyStoryUpdates(Posts post, StoryUpdateRequest request) {
        if (request.content() != null) {
            post.updateStory(request.content());
        }
        if (request.place() != null) {
            post.updatePlace(request.place());
        }
    }

    private void applyOptionalUpdatesOnCreate(Posts saved, StoryCreateRequest request) {
        if (request.place() != null) {
            saved.updatePlace(request.place());
        }
        // create는 빈 리스트면 굳이 delete 쿼리 날릴 필요 없음
        if (request.imagesUrl() != null && !request.imagesUrl().isEmpty()) {
            replaceImages(saved, request.imagesUrl());
        }
        if (request.taggedMemberIds() != null && !request.taggedMemberIds().isEmpty()) {
            replaceTaggedMembers(saved.getPostId(), request.taggedMemberIds());
        }
    }

    /**
     * null이면 변경 없음
     * 빈 리스트면 전체 삭제
     * 값 있으면 교체
     */
    private void applyMediaUpdatesOnUpdate(Posts post, StoryUpdateRequest request) {
        if (request.imagesUrl() != null) {
            replaceImages(post, request.imagesUrl());
        }
        if (request.taggedMemberIds() != null) {
            replaceTaggedMembers(post.getPostId(), request.taggedMemberIds());
        }
    }

    private void replaceImages(Posts post, List<String> imagesUrl) {
        postImageRepository.deleteByPost_PostId(post.getPostId());

        if (imagesUrl.isEmpty())
            return;

        List<PostImages> images = imagesUrl.stream()
                .map(imageService::saveBase64Image)
                .map(url -> PostImages.of(post, url))
                .toList();

        postImageRepository.saveAll(images);
    }

    private void replaceTaggedMembers(Long postId, List<Long> memberIds) {
        postMemberTagRepository.deleteByPostId(postId);

        if (memberIds.isEmpty())
            return;

        List<PostMemberTags> tags = memberIds.stream()
                .distinct()
                .map(memberId -> PostMemberTags.of(postId, memberId))
                .toList();

        postMemberTagRepository.saveAll(tags);
    }

}
