package back.repository;

import back.domain.ClubMembers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubMembersRepository extends JpaRepository<ClubMembers, Long> {

    /**
     * 특정 모임에 속한 특정 사용자를 조회합니다.
     */
    Optional<ClubMembers> findByClubIdAndUserId(Long clubId, Long userId);

    /**
     * 특정 모임에 속한 특정 사용자가 활성 상태인지 확인합니다.
     */
    boolean existsByClubIdAndUserIdAndStatus(Long clubId, Long userId, String status);
}
