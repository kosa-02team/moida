package back.repository.schedule;

import back.domain.schedule.ScheduleParticipants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipants, Long> {

    /**
     * 특정 일정의 모든 참여자를 조회합니다.
     */
    List<ScheduleParticipants> findByScheduleId(Long scheduleId);

    /**
     * 특정 일정에서 특정 사용자의 참여 정보를 조회합니다.
     */
    Optional<ScheduleParticipants> findByScheduleIdAndUserId(Long scheduleId, Long userId);

    /**
     * 특정 일정에서 특정 참석 상태의 참여자 수를 조회합니다.
     */
    long countByScheduleIdAndAttendanceStatus(Long scheduleId, String attendanceStatus);

    /**
     * 특정 일정에서 특정 참석 상태의 참여자 목록을 조회합니다.
     */
    List<ScheduleParticipants> findByScheduleIdAndAttendanceStatus(Long scheduleId, String attendanceStatus);

    /**
     * 특정 일정의 참여자 존재 여부를 확인합니다.
     */
    boolean existsByScheduleIdAndUserId(Long scheduleId, Long userId);

    /**
     * 특정 일정의 참석 확정 인원 수를 조회합니다.
     */
    @Query("SELECT COUNT(sp) FROM ScheduleParticipants sp WHERE sp.scheduleId = :scheduleId AND sp.attendanceStatus = 'ATTENDING'")
    long countAttendingByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * 특정 일정의 모든 참여 기록을 삭제합니다.
     */
    void deleteByScheduleId(Long scheduleId);
}
