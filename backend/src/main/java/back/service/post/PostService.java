package back.service.post;

import back.domain.club.Clubs;
import back.domain.schedule.Schedules;
import back.domain.Users;
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
import back.repository.schedule.ScheduleRepository;
import back.repository.club.ClubRepository;
import back.repository.post.PostImageRepository;
import back.repository.post.PostMemberTagRepository;
import back.repository.post.PostRepository;
import back.repository.post.projection.RecentAlbumRow;
import back.repository.UserRepository;
import back.service.club.ClubAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final ClubAuthService clubAuthorizationService;

    private final ClubRepository clubsRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostMemberTagRepository postMemberTagRepository;

    // 알림 전송을 위해 추가
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public PostIdResponse createStory(Long clubId, Long writerId, StoryCreateRequest request) {

        clubAuthorizationService.assertActiveMember(clubId, writerId);

        Posts saved = postRepository.save(buildStoryPost(clubId, writerId, request));

        applyOptionalUpdatesOnCreate(saved, request);

        // 알림 이벤트 발행
        eventPublisher.publishEvent(new back.event.PostCreatedEvent(
                clubId,
                saved.getPostId(),
                saved.getContent(), /* 제목이 없으므로 내용 앞부분이나 전체 사용. 스토리형 게시글이라 제목 필드가 없는 모양 */
                writerId));

        return PostIdResponse.from(saved);
    }

    public PostDetailResponse getPost(Long clubId, Long postId, Long viewerId) {
        clubAuthorizationService.validateAndGetClubForReadPosts(clubId, viewerId);

        Posts post = getActivePostOrThrow(postId, clubId);

        return PostDetailResponse.from(post);
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

        return page.getContent().stream()
                .map(p -> PostCardResponse.of(p, imageMap.getOrDefault(p.postId(), List.of())))
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
        Users writerRef = userRepository.getReferenceById(writerId);
        Schedules scheduleRef = getScheduleRefOrNull(request.scheduleId());

        return Posts.story(clubRef, writerRef, scheduleRef, request.content());
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
