package back.repository.notifications;

import back.domain.Notifications;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationsRepository extends JpaRepository<Notifications,Long> {
}
