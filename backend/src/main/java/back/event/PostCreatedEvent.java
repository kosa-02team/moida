package back.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PostCreatedEvent {
    private final Long clubId;
    private final Long postId;
    private final String postTitle;
    private final Long authorId;
}
