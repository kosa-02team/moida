package back.service.post.ai.gemini.embedding;

import back.domain.post.Posts;
import back.domain.schedule.Schedules;

import java.time.format.DateTimeFormatter;
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

    public static String build(Schedules schedule) {
        StringBuilder sb = new StringBuilder();

        // 일정 이름 (필수)
        if (schedule.getScheduleName() != null && !schedule.getScheduleName().isBlank()) {
            sb.append(schedule.getScheduleName().trim()).append("\n");
        }

        // 설명 (선택)
        if (schedule.getDescription() != null && !schedule.getDescription().isBlank()) {
            sb.append(schedule.getDescription().trim()).append("\n");
        }

        // 일정 날짜
        if (schedule.getEventDate() != null) {
            sb.append("일정 날짜: ")
                    .append(schedule.getEventDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .append("\n");
        }

        // 장소 (선택)
        if (schedule.getLocation() != null && !schedule.getLocation().isBlank()) {
            sb.append("장소: ").append(schedule.getLocation().trim()).append("\n");
        }

        // 참가비 (선택)
        if (schedule.getEntryFee() != null && schedule.getEntryFee().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("참가비: ").append(schedule.getEntryFee()).append("원\n");
        }

        return sb.toString().trim();
    }
}
