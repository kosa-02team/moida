package back.service.post.ai.gemini.embedding;

import back.domain.post.Posts;

import java.util.List;

public final class EmbeddingTextBuilder {

    public static String build(Posts post, List<String> memberNames) {
        return """
        제목: %s
        내용: %s
        장소: %s
        작성자: %s
        날짜: %s
        함께 간 사람: %s
        """
                .formatted(
                        post.getTitle(),
                        post.getContent(),
                        post.getPlace(),
                        post.getWriter().getNickname(),
                        post.getCreatedAt().toLocalDate(),
                        String.join(", ", memberNames)
                );
    }
}
