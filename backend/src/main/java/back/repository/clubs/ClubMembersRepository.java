package back.repository.clubs;

import back.domain.ClubMembers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClubMembersRepository extends JpaRepository<ClubMembers, Long> {

    // 1) 정석: 엔티티를 가져온다
    Optional<ClubMembers> findByClubIdAndUserIdAndStatus(Long clubId, Long userId, ClubMembers.Status status);

    // 2) ACTIVE roles만 필요하면 편의 메서드로 제공
    default Optional<List<String>> findActiveRoles(Long clubId, Long userId) {
        return findByClubIdAndUserIdAndStatus(clubId, userId, ClubMembers.Status.ACTIVE)
                .map(ClubMembers::getRoles);
    }

    // 3) 존재 여부도 enum 타입으로
    boolean existsByClubIdAndUserIdAndStatus(Long clubId, Long userId, ClubMembers.Status status);
}