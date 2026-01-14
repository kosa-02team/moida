package back.repository.schedule;

import back.domain.schedule.Schedules;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedules, Long> {

    /**
     * 모임에 속한 일정 목록을 조회합니다.
     */
    List<Schedules> findByClubId(Long clubId);
}

