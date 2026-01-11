package back.repository.posts;

import back.domain.posts.Posts;
import back.dto.posts.response.PostCardBase;
import back.dto.posts.response.PostCardResponse;
import back.repository.posts.projection.RecentAlbumRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostsRepository extends JpaRepository<Posts, Long> {
    Optional<Posts> findByPostIdAndClubId(Long postId, Long clubId);
    Page<Posts> findAllByClubIdAndDeletedAtIsNull(Long clubId, Pageable pageable);

    @Query("""
    select new back.dto.posts.response.PostCardResponse(
        p.clubId,
        p.postId,
        p.writerId,
        p.title,
        p.content,
        cast(null as java.util.List),
        coalesce(count(distinct l.id), 0),
        coalesce(count(distinct c.id), 0),
        p.createdAt
    )
    from Posts p
    left join PostLike l on l.postId = p.postId
    left join Comment c on c.postId = p.postId and c.deletedAt is null
    where p.clubId = :clubId
      and p.deletedAt is null
    group by p.clubId, p.postId, p.writerId, p.title, p.content, p.createdAt
    """)
    Page<PostCardBase> findPostCards(@Param("clubId") Long clubId, Pageable pageable);

    @Query("""
    select 
        p.scheduleId as scheduleId,
        s.name as scheduleName,
        max(p.createdAt) as lastCreatedAt
    from Posts p
    join Schedules s on s.scheduleId = p.scheduleId
    where p.clubId = :clubId
      and p.deletedAt is null
      and p.scheduleId is not null
    group by p.scheduleId, s.name
    order by max(p.createdAt) desc
    """)
    List<RecentAlbumRow> findRecentAlbumRows(@Param("clubId") Long clubId, Pageable pageable);
}
