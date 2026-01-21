package back.service.post;

import back.domain.club.ClubMembers;
import back.domain.post.Comments;
import back.domain.post.Posts;
import back.dto.post.comment.request.PostCommentRequest;
import back.dto.post.comment.response.PostCommentsIdResponse;
import back.dto.post.comment.response.PostCommentsResponse;
import back.exception.PostsException;
import back.repository.post.PostCommentRepository;
import back.repository.post.PostRepository;
import back.repository.club.ClubMemberRepository;
import back.service.club.ClubAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCommentService {

        private final PostCommentRepository postCommentRepository;
        private final ClubMemberRepository clubMembersRepository;
        private final PostRepository postRepository;
        private final ClubAuthService clubsAuthorizationService;
        private final org.springframework.context.ApplicationEventPublisher eventPublisher;

        @Transactional
        public PostCommentsIdResponse createComment(
                        Long writerId,
                        Long clubId,
                        Long postId,
                        PostCommentRequest request) {
                clubsAuthorizationService.assertActiveMember(clubId, writerId);
                Comments comments = postCommentRepository.save(buildPostComment(writerId, postId, request));

                // 알림 이벤트 발행
                eventPublisher.publishEvent(new back.event.CommentCreatedEvent(
                                postId,
                                comments.getContent(),
                                writerId,
                                comments.getPost().getWriter().getUserId()));

                return PostCommentsIdResponse.from(comments);
        }

        @Transactional
        public PostCommentsIdResponse updateComment(Long writerId, Long clubId, Long postId, Long commentId,
                        PostCommentRequest request) {
                clubsAuthorizationService.validateAndGetClubForUpdatePosts(clubId, writerId);

                Comments comment = postCommentRepository
                                .findByCommentIdAndPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(commentId, postId,
                                                clubId)
                                .orElseThrow(PostsException.PostCommentNotFound::new); // 예외는 댓글용으로 교체 권장

                boolean isWriter = comment.getWriter().getUserId().equals(writerId); //

                if (!isWriter) {
                        clubsAuthorizationService.assertAtLeastManager(clubId, writerId);
                }

                comment.updateContent(request.content()); // 엔티티 메서드

                return PostCommentsIdResponse.from(comment);
        }

        public PostCommentsResponse getPostComments(Long viewerId, Long clubId, Long postId, Pageable pageable) {
                clubsAuthorizationService.validateAndGetClubForReadPosts(clubId, viewerId);

                Page<Comments> page = postCommentRepository
                                .findAllByPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(
                                                postId, clubId, pageable);

                List<PostCommentsResponse.Item> items = page.getContent().stream().map(PostCommentsResponse.Item::from)
                                .toList();

                return new PostCommentsResponse(
                                items,
                                page.getNumber(),
                                page.getSize(),
                                page.getTotalElements(),
                                page.getTotalPages(),
                                page.hasNext());
        }

        @Transactional
        public PostCommentsIdResponse deleteComment(Long actorId, Long clubId, Long postId, Long commentId) {

                Comments comment = postCommentRepository
                                .findByCommentIdAndPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(commentId, postId,
                                                clubId)
                                .orElseThrow(PostsException.PostCommentNotFound::new); // 프로젝트 예외로 교체

                // 멱등 삭제
                if (comment.getDeletedAt() != null) {
                        return PostCommentsIdResponse.from(comment);
                }
                clubsAuthorizationService.validateAndGetClubForUpdatePosts(clubId, actorId);

                boolean isWriter = comment.getWriter().getUserId().equals(actorId);
                if (!isWriter) {
                        clubsAuthorizationService.assertAtLeastManager(clubId, actorId);
                }

                comment.delete(); // 엔티티 메서드에서 deletedAt 세팅

                return PostCommentsIdResponse.from(comment);
        }

        private Comments buildPostComment(
                        Long writerId,
                        Long postId,
                        PostCommentRequest request) {
                ClubMembers writerRef = clubMembersRepository.getReferenceById(writerId);
                Posts postsRef = postRepository.getReferenceById(postId);

                String content = request.content();
                return new Comments(postsRef, writerRef, content);
        }
}
