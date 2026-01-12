package back.service.posts;

import back.domain.Users;
import back.domain.posts.Comments;
import back.domain.posts.Posts;
import back.dto.posts.comments.request.PostCommentRequest;
import back.dto.posts.comments.response.PostCommentsIdResponse;
import back.dto.posts.comments.response.PostCommentsResponse;
import back.exception.PostsException;
import back.repository.posts.PostCommentsRepository;
import back.repository.posts.PostsRepository;
import back.repository.users.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCommentsService {

        private final PostCommentsRepository postCommentsRepository;
        private final UsersRepository usersRepository;
        private final PostsRepository postsRepository;

        public PostCommentsIdResponse createComment(
                        Long writerId,
                        Long clubId,
                        Long postId,
                        PostCommentRequest request) {
                // TODO 권한 정책 확정 후 적용
                // clubAuthorizationService.assertActiveMember(clubId, writerID);
                Comments comments = postCommentsRepository.save(buildPostComment(writerId, postId, request));

                return PostCommentsIdResponse.from(comments);
        }

        @Transactional
        public PostCommentsIdResponse updateComment(Long writerId, Long clubId, Long postId, Long commentId,
                        PostCommentRequest request) {
                // TODO: clubAuthorizationService.validateAndGetClubForReadPosts(clubId,
                // writerId);
                // boolean isWriter = comment.getWriter().getUserId().equals(writerId); //
                // writer 타입에 맞게 수정
                // if (!isWriter) {
                // clubAuthorizationService.assertAtLeastManager(clubId, writerId);
                // }

                Comments comment = postCommentsRepository.findByCommentIdAndPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(commentId, postId, clubId)
                                .orElseThrow(PostsException.PostCommentNotFound::new); // 예외는 댓글용으로 교체 권장

                comment.updateContent(request.content()); // 엔티티 메서드

                return PostCommentsIdResponse.from(comment);
        }

        public PostCommentsResponse getPostComments(Long viewerId, Long clubId, Long postId, Pageable pageable) {
                // clubAuthorizationService.validateAndGetClubForReadPosts(clubId, viewerId);

                Page<Comments> page = postCommentsRepository
                                .findAllByPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(
                                                postId, clubId, pageable);

                List<PostCommentsResponse.Item> items = page.getContent().stream().map(c -> {
                        return PostCommentsResponse.Item.from(c);
                }).toList();

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
                // TODO: 읽기 권한 정책 필요하면 여기서 체크
                // clubAuthorizationService.validateAndGetClubForReadPosts(clubId, actorId);

                Comments comment = postCommentsRepository
                                .findByCommentIdAndPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(commentId, postId, clubId)
                                .orElseThrow(PostsException.PostCommentNotFound::new); // 프로젝트 예외로 교체

                // 멱등 삭제
                if (comment.getDeletedAt() != null) {
                        return PostCommentsIdResponse.from(comment);
                }

                // boolean isWriter = comment.getWriter().getUserId().equals(actorId);
                // if (!isWriter) {
                // clubAuthorizationService.assertAtLeastManager(clubId, actorId);
                // }

                comment.delete(); // 엔티티 메서드에서 deletedAt 세팅

                return PostCommentsIdResponse.from(comment);
        }

        private Comments buildPostComment(
                        Long writerId,
                        Long postId,
                        PostCommentRequest request) {
                Users writerRef = usersRepository.getReferenceById(writerId);
                Posts postsRef = postsRepository.getReferenceById(postId);

                String content = request.content();
                return new Comments(postsRef, writerRef, content);
        }
}
