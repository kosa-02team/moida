package back.repository.post.projection;

import java.time.LocalDateTime;

public interface RecentAlbumRow {
    Long getScheduleId();
    String getScheduleName();     // 스케줄명 필요 없으면 제거 가능
    LocalDateTime getLastCreatedAt();
}