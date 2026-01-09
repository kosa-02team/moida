package back.service.posts;

import back.repository.posts.*;
import back.service.clubs.ClubsAuthorizationService;
import back.domain.posts.PostImages;
import back.domain.posts.PostMemberTags;
import back.domain.posts.Posts;
import back.dto.posts.request.PostUpdateRequest;
import back.dto.posts.request.StoryCreateRequest;
import back.dto.posts.response.PostResponse;
import back.exception.PostException;
import back.repository.posts.PostMemberTagsRepository;
import back.repository.posts.PostsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public Long createStory(Long clubId, Long writerId, StoryCreateRequest request) {
        //todo : 권한 확인, ClubAuthorizationService 호출해서 해야할듯?
        Posts post = Posts.story(
                writerId,
                clubId,
                request.scheduleId(),
                request.content()
        );

        Posts savedPost = postsRepository.save(post);
        post.updatePlace(request.place());

        saveImages(savedPost, request.imagesUrl());
        saveTaggedMembers(savedPost.getPostId(), request.taggedMemberIds());

        return savedPost.getPostId();
    }

    public PostResponse getPost(Long clubId, Long postId) {
        //todo : 모임 공개 방식 확인 필요

        Posts post = postsRepository.findById(postId)
                .orElseThrow(PostException.NotFound::new);

        return PostResponse.from(post);
    }

    public List<PostResponse> getAllPosts(Long clubId) {
        //todo : 모임 공개 방식 확인 필요
        return postsRepository.findAll().stream()
                .filter(post -> post.getDeletedAt() == null) // Filter soft-deleted posts
                .map(PostResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePost(Long clubId, Long postId, PostUpdateRequest request) {
        //todo : 1. 수정 권한 확인 v
        // 2. 작성자가 같은지 확인 v
        // 3. 시스템 관리자인지도 추가해야함 (MVP 아니라 일단 보류)
        Long actorId=1L; // todo : 요청한 사람 누구인지 확인 필요, 현재는 상수로 저장

        clubAuthorizationService.assertAtLeastManager(clubId, actorId);

        Posts post = postsRepository.findById(postId)
                .orElseThrow(PostException.NotFound::new);

        //이미 삭제되거나 블라인드처리된 게시글은 수정 불가
        if (post.getDeletedAt() != null ) {
            throw new PostException.Deleted();
        }

        //작성자가 아니면 수정 불가
        if (!post.getWriterId().equals(actorId)) {
            throw new PostException.Forbidden();
        }

        post.updatePost(request.title(), request.content());

    }

    @Transactional
    public void blindPost(Long postId) {
        //todo : 1. 수정 권한 확인
        Long adminId  = 1L; //todo : 요청한 사람 필요
        Posts post = postsRepository.findById(postId)
                .orElseThrow(PostException.NotFound::new);

        // 이미 블라인드면 그냥 종료(권장: idempotent)
        if (post.getDeletedAt() != null) {
            return;
        }

        post.blindPost(adminId);

    }

    @Transactional
    public void deletePost(Long postId) {
        Long actorId=1L; // todo : 요청한 사람 누구인지 확인 필요, 현재는 상수로 저장
        //작성자가 아니면 삭제 불가
        Posts post = postsRepository.findById(postId)
                .orElseThrow(PostException.NotFound::new);
        post.delete();
    }

    @Transactional
    public void replaceImages(Long postId, List<String> urls) {
        Posts post = postsRepository.findById(postId)
                .orElseThrow(PostException.NotFound::new);

        postImagesRepository.deleteByPost_PostId(postId);

        if (urls != null && !urls.isEmpty()) {
            postImagesRepository.saveAll(
                    urls.stream().map(url -> PostImages.of(post, url)).toList()
            );
        }
    }

    private void saveImages(Posts posts, List<String> imagesUrl){
        if (imagesUrl != null && !imagesUrl.isEmpty()) {
            List<PostImages> images = imagesUrl.stream()
                    .map(url -> PostImages.of(posts, url))
                    .toList();

            postImagesRepository.saveAll(images);
        }
    }

    private void saveTaggedMembers(Long postId, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return;

        List<PostMemberTags> tags = memberIds.stream()
                .map(memberId -> PostMemberTags.of(postId, memberId))
                .toList();
        postMemberTagsRepository.saveAll(tags);
    }
}
