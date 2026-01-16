package back.repository;

import back.domain.ClubMembers;
import back.repository.clubs.MemberNameView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMembers, Long> {

    boolean existsByClubIdAndUserId(Long clubId, Long userId);

    Optional<ClubMembers> findByClubIdAndUserId(Long clubId, Long userId);
    Optional<ClubMembers> findByClubIdAndMemberId(Long clubId, Long memberId);

    @Query("""
        select u.realName as realName, m.clubNickname as clubNickname
        from ClubMembers m
        join Users u on u.userId = m.userId
        where m.clubId = :clubId and m.memberId = :memberId
    """)
    Optional<MemberNameView> findNameView(Long clubId, Long memberId);

    @Query("""
        select count(m)
        from ClubMembers m
        join Users u on u.userId = m.userId
        where m.clubId = :clubId and u.realName = :realName
    """)
    long countByClubIdAndRealName(Long clubId, String realName);

    long countByClubIdAndClubNickname(Long clubId, String clubNickname);
}
