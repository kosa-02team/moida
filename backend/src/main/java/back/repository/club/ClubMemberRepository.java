package back.repository.club;

import back.domain.club.ClubMembers;
import back.repository.club.projection.NameView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMembers, Long> {

    boolean existsByClubIdAndUserId(Long clubId, Long userId);
    boolean existsByClubIdAndUserIdAndStatus(Long clubId, Long userId, ClubMembers.Status status);
    boolean existsByClubIdAndNickname(Long clubId, String nickname);

    Optional<ClubMembers> findByClubIdAndUserId(Long clubId, Long userId);
    Optional<ClubMembers> findByClubIdAndMemberId(Long clubId, Long memberId);
    Optional<ClubMembers> findByClubIdAndUserIdAndStatus(Long clubId, Long userId, ClubMembers.Status status);

    default Optional<ClubMembers.Role> findActiveRole(Long clubId, Long userId) {
        return findByClubIdAndUserIdAndStatus(clubId, userId, ClubMembers.Status.ACTIVE)
                .map(ClubMembers::getRole);
    }

    long countByClubIdAndStatus(Long clubId, ClubMembers.Status status);

    List<ClubMembers> findByClubIdAndStatus(Long clubId, ClubMembers.Status status);

    @Query("""
    select u.realName as realName,
           cm.nickname as clubNickname
    from ClubMembers cm
    join Users u on u.id = cm.userId
    where cm.clubId = :clubId
      and cm.memberId = :memberId
""")
    Optional<NameView> findNameView(
            @Param("clubId") Long clubId,
            @Param("memberId") Long memberId
    );


    @Query("""
    select count(cm)
    from ClubMembers cm
    join Users u on u.id = cm.userId
    where cm.clubId = :clubId
      and u.realName = :realName
""")
    Long countByClubIdAndRealName(
            @Param("clubId") Long clubId,
            @Param("realName") String realName
    );
    @Query("""
    select count(cm)
    from ClubMembers cm
    where cm.clubId = :clubId
      and cm.nickname = :nickname
""")
    Long countByClubIdAndClubNickname(
            @Param("clubId") Long clubId,
            @Param("nickname") String nickname
    );

}