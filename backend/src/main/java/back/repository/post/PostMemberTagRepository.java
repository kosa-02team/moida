package back.repository.post;

import back.domain.post.PostMemberTags;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostMemberTagRepository extends JpaRepository<PostMemberTags, Long> {

    void deleteByPostId(Long postId);

    List<PostMemberTags> findByPostId(Long postId);

    //닉네임 → postId (필터용)
    @Query(value = """
        select distinct pmt.post_id
        from post_member_tags pmt
        join club_members cm on pmt.member_id = cm.member_id
        where cm.nickname in (:names)
    """, nativeQuery = true)
    List<Long> findPostIdsByMemberNames(@Param("names") List<String> names);

    //postId → 닉네임 (context 출력용)
    @Query(value = """
        select pmt.post_id, cm.nickname
        from post_member_tags pmt
        join club_members cm on pmt.member_id = cm.member_id
        where pmt.post_id in (:postIds)
    """, nativeQuery = true)
    List<Object[]> findMemberNamesGroupedByPostIds(@Param("postIds") List<Long> postIds);

    //전체 닉네임 목록 (질문 매칭용)
    @Query("""
        select distinct cm.nickname
        from ClubMembers cm
    """)
    List<String> findAllDistinctMemberNames();
}
