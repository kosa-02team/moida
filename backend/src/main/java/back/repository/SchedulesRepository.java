package back.repository;

import back.domain.Schedules;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulesRepository extends JpaRepository<Schedules, Long> {
}

