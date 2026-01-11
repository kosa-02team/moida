package back.service.posts;

import back.domain.posts.PostImages;
import back.domain.posts.PostMemberTags;
import back.domain.posts.Posts;
import back.dto.posts.request.StoryCreateRequest;
import back.dto.posts.request.StoryUpdateRequest;
import back.dto.posts.response.*;
import back.exception.PostException;
import back.repository.posts.PostImagesRepository;
import back.repository.posts.PostMemberTagsRepository;
import back.repository.posts.PostsRepository;
import back.repository.posts.projection.RecentAlbumRow;
import back.service.clubs.ClubsAuthorizationService;
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
public class PostsService {

    private final ClubsAuthorizationService clubAuthorizationService;
    private final PostsRepository postsRepository;
    private final PostImagesRepository postImagesRepository;
    private final PostMemberTagsRepository postMemberTagsRepository;

    @Transactional
    public PostIdResponse createStory(Long clubId, Long writerId, StoryCreateRequest request) {
        // TODO 권한 정책 확정 후 적용
        // clubAuthorizationService.assertActiveMember(clubId, writerId);

        Posts post = Posts.story(
                writerId,
                clubId,
                request.scheduleId(),
                request.content()
        );

        Posts saved = postsRepository.save(post);

        if (request.place() != null) {
            saved.updatePlace(request.place());
        }

        // create도 replace로 통일 가능 (delete 0건이라 부담 거의 없음)
        if (request.imagesUrl() != null) {
            replaceImages(saved, request.imagesUrl());
        }
        if (request.taggedMemberIds() != null) {
            replaceTaggedMembers(saved.getPostId(), request.taggedMemberIds());
        }

        return PostIdResponse.from(saved);
    }

    public PostDetailResponse getPost(Long clubId, Long postId, Long viewerId) {
        clubAuthorizationService.validateAndGetClubForReadPosts(clubId, viewerId);

        Posts post = postsRepository.findByPostIdAndClubId(postId, clubId)
                .orElseThrow(PostException.NotFound::new);

        if (post.getDeletedAt() != null) {
            throw new PostException.Deleted();
        }

        return PostDetailResponse.from(post);
    }

    @Transactional(readOnly = true)
    public List<AlbumCardResponse> getRecentAlbums(Long clubId, Long viewerId, int limit) {
        clubAuthorizationService.validateAndGetClubForReadPosts(clubId, viewerId);

        // 1) scheduleId 기준 최근 앨범 limit개
        List<RecentAlbumRow> rows = postsRepository.findRecentAlbumRows(
                clubId, PageRequest.of(0, limit)
        );

        if (rows.isEmpty()) return List.of();

        List<Long> scheduleIds = rows.stream()
                .map(RecentAlbumRow::getScheduleId)
                .toList();

        // 2) 해당 scheduleIds의 이미지들을 한 번에 조회 (최신순)
        List<PostImages> images = postImagesRepository.findImagesForSchedules(clubId, scheduleIds);

        // scheduleId -> 이미지 리스트
        Map<Long, List<PostImages>> imageMap = images.stream()
                .collect(Collectors.groupingBy(pi -> pi.getPost().getScheduleId()));

        // rows 순서(최신순) 유지해서 카드 만들기
        List<AlbumCardResponse> result = new ArrayList<>();

        for (RecentAlbumRow r : rows) {
            List<PostImages> list = imageMap.getOrDefault(r.getScheduleId(), List.of());
            if (list.isEmpty()) continue; // 이미지 없는 앨범이면 스킵(정책)

            PostImages cover = list.get(0); // findImagesForSchedules가 createdAt desc이므로 첫 장이 커버

            result.add(new AlbumCardResponse(
                    clubId,
                    cover.getPost().getPostId(),      // 커버가 속한 postId
                    r.getScheduleId(),
                    r.getScheduleName(),              // scheduleName 필요 없으면 null/제거
                    cover.getImageUrl(),              // 커버 이미지 1장
                    list.size(),                      // albumCount = 전체 이미지 수
                    r.getLastCreatedAt()              // 앨범 최신 시각(= max posts.createdAt)
            ));
        }

        return result;
    }

    public List<PostCardResponse> getRecentPosts(Long clubId, Long viewerId, Pageable pageable) {
        clubAuthorizationService.validateAndGetClubForReadPosts(clubId, viewerId);

        Page<PostCardBase> page = postsRepository.findPostCards(clubId, pageable);

        List<Long> postIds = page.getContent().stream()
                .map(PostCardBase::postId)
                .toList();

        Map<Long, List<String>> imageMap =
                postImagesRepository.findByPostIdIn(postIds).stream()
                        .collect(Collectors.groupingBy(
                                PostImages::getPostId,
                                Collectors.mapping(PostImages::getImageUrl, Collectors.toList())
                        ));

        return page.getContent().stream()
                .map(p -> PostCardResponse.of(p, imageMap.getOrDefault(p.postId(), List.of())))
                .toList();
    }

    @Transactional
    public PostIdResponse updatePost(Long clubId, Long postId, Long actorId, StoryUpdateRequest request) {

        Posts post = postsRepository.findByPostIdAndClubId(postId, clubId)
                .orElseThrow(PostException.NotFound::new);

        if (post.getDeletedAt() != null) {
            throw new PostException.Deleted();
        }

        // 권한 정책
        // 작성자면 OK, 작성자가 아니면 운영진 이상만 OK
        boolean isWriter = post.getWriterId().equals(actorId);
        if (!isWriter) {
            clubAuthorizationService.assertAtLeastManager(clubId, actorId);
        }

        if (request.content() != null) {
            post.updateStory(request.content());
        }
        if (request.place() != null) {
            post.updatePlace(request.place());
        }

        // null이면 변경 없음, 빈 리스트면 전체 삭제, 값 있으면 교체
        if (request.imagesUrl() != null) {
            replaceImages(post, request.imagesUrl());
        }
        if (request.taggedMemberIds() != null) {
            replaceTaggedMembers(postId, request.taggedMemberIds());
        }

        return PostIdResponse.from(post);
    }

    @Transactional
    public void blindPost(Long clubId, Long postId, Long actorId) {
        Posts post = postsRepository.findByPostIdAndClubId(postId, clubId)
                .orElseThrow(PostException.NotFound::new);

        if (post.getDeletedAt() != null) {
            return;
        }

        boolean isWriter = post.getWriterId().equals(actorId);
        if (!isWriter) {
            clubAuthorizationService.assertAtLeastManager(clubId, actorId);
        }

        post.blindPost(actorId);
    }

    @Transactional
    public void deletePost(Long clubId, Long postId, Long actorId) {
        Posts post = postsRepository.findByPostIdAndClubId(postId, clubId)
                .orElseThrow(PostException.NotFound::new);

        boolean isWriter = post.getWriterId().equals(actorId);
        if (!isWriter) {
            clubAuthorizationService.assertAtLeastManager(clubId, actorId);
        }

        post.delete();
    }

    private void replaceImages(Posts post, List<String> imagesUrl) {
        postImagesRepository.deleteByPost_PostId(post.getPostId());

        if (imagesUrl == null || imagesUrl.isEmpty()) return;

        List<PostImages> images = imagesUrl.stream()
                .map(url -> PostImages.of(post, url))
                .toList();

        postImagesRepository.saveAll(images);
    }

    private void replaceTaggedMembers(Long postId, List<Long> memberIds) {
        postMemberTagsRepository.deleteByPostId(postId);

        if (memberIds == null || memberIds.isEmpty()) return;

        List<PostMemberTags> tags = memberIds.stream()
                .distinct()
                .map(memberId -> PostMemberTags.of(postId, memberId))
                .toList();

        postMemberTagsRepository.saveAll(tags);
    }
}
