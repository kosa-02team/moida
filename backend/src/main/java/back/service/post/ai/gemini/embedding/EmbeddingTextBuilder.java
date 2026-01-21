package back.service.post.ai.gemini.embedding;

import back.domain.post.Posts;

import java.util.List;

public final class EmbeddingTextBuilder {

    public static String build(Posts post, List<String> memberNames) {
        StringBuilder sb = new StringBuilder();

        // 핵심 콘텐츠 (필수)
        if (post.getContent() != null && !post.getContent().isBlank()) {
            sb.append(post.getContent().trim()).append("\n");
        }

        // 제목 (선택)
        if (post.getTitle() != null && !post.getTitle().isBlank()) {
            sb.append("제목: ").append(post.getTitle().trim()).append("\n");
        }

        // 장소 (선택)
        if (post.getPlace() != null && !post.getPlace().isBlank()) {
            sb.append("장소: ").append(post.getPlace().trim()).append("\n");
        }

        // 멤버 (선택)
        if (memberNames != null && !memberNames.isEmpty()) {
            sb.append("함께한 사람: ")
                    .append(String.join(", ", memberNames))
                    .append("\n");
        }

        return sb.toString().trim();
    }
}
