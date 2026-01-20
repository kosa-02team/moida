package back.repository.notifications;

import back.domain.Notifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationsRepository extends JpaRepository<Notifications, Long> {
    
    /**
     * 사용자별 알림 목록 조회 (페이징)
     */
    Page<Notifications> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 사용자별 읽음/안읽음 알림 목록 조회 (페이징)
     */
    Page<Notifications> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, Boolean isRead, Pageable pageable);
    
    /**
     * 사용자별 알림 조회
     */
    Optional<Notifications> findByNotiIdAndUserId(Long notiId, Long userId);
    
    /**
     * 사용자별 읽지 않은 알림 개수
     */
    long countByUserIdAndIsReadFalse(Long userId);
}
