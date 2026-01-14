package back.repository.posts;

import back.domain.posts.PostImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImages, Long> {

    void deleteByPost_PostId(Long postId);

    @Query("select pi from PostImages pi where pi.post.postId in :postIds")
    List<PostImages> findByPostIdIn(@Param("postIds") List<Long> postIds);

    @Query("""
    select pi
    from PostImages pi
    join pi.post p
    where p.club.clubId = :clubId
      and p.schedule.scheduleId in :scheduleIds
      and p.deletedAt is null
    order by p.schedule.scheduleId asc, pi.createdAt asc
""")
    List<PostImages> findImagesForSchedules(@Param("clubId") Long clubId,
                                            @Param("scheduleIds") List<Long> scheduleIds);

}
