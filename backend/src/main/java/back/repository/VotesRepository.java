package back.repository;

import back.domain.Votes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VotesRepository extends JpaRepository<Votes, Long> {

    /**
     * 기한이 지났지만 아직 종료되지 않은 일반 투표들을 조회합니다.
     */
    @Query("SELECT v FROM Votes v WHERE v.voteType = 'GENERAL' " +
           "AND v.status = 'OPEN' " +
           "AND v.deadline IS NOT NULL " +
           "AND v.deadline <= :now")
    List<Votes> findExpiredGeneralVotes(@Param("now") LocalDateTime now);

    /**
     * 일정 시작 5분 전이 지났지만 아직 종료되지 않은 일정 투표들을 조회합니다.
     */
    @Query(value = "SELECT v.* FROM votes v " +
           "INNER JOIN schedules s ON v.schedule_id = s.schedule_id " +
           "WHERE v.vote_type = 'ATTENDANCE' " +
           "AND v.status = 'OPEN' " +
           "AND s.event_date IS NOT NULL " +
           "AND DATE_SUB(s.event_date, INTERVAL 5 MINUTE) <= :now", 
           nativeQuery = true)
    List<Votes> findExpiredAttendanceVotes(@Param("now") LocalDateTime now);
}
