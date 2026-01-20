package back.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CommentCreatedEvent {
    private final Long postId;
    private final String commentContent;
    private final Long commentAuthorId; // 댓글 작성자
    private final Long postAuthorId; // 게시글 작성자 (수신 대상)
}
