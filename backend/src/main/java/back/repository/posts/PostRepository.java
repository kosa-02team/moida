package back.repository.posts;

import back.domain.posts.Posts;
import back.dto.posts.posts.response.PostCardBase;
import back.repository.posts.projection.RecentAlbumRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Posts, Long> {
  Optional<Posts> findByPostIdAndClub_ClubId(Long postId, Long clubId);

  @Query("""
      select new back.dto.posts.posts.response.PostCardBase(
          p.club.clubId,
          p.postId,
          p.writer.userId,
          p.writer.realName,
          p.title,
          p.content,
          coalesce(count(distinct l.likeId), 0),
          coalesce(count(distinct c.commentId), 0),
          p.createdAt
      )
      from Posts p
      left join PostLikes l on l.postId = p.postId
      left join Comments c on c.post.postId = p.postId and c.deletedAt is null
      where p.club.clubId = :clubId
        and p.deletedAt is null
      group by p.club.clubId, p.postId, p.writer.userId, p.writer.realName, p.title, p.content, p.createdAt
      """)
  Page<PostCardBase> findPostCards(@Param("clubId") Long clubId, Pageable pageable);

    @Query("""
    select
        p.schedule.scheduleId as scheduleId,
        p.schedule.scheduleName as scheduleName,
        max(p.createdAt) as lastCreatedAt
    from Posts p
    where p.club.clubId = :clubId
      and p.deletedAt is null
      and p.schedule is not null
    group by p.schedule.scheduleId, p.schedule.scheduleName
    order by max(p.createdAt) desc
    """)
    List<RecentAlbumRow> findRecentAlbumRows(@Param("clubId") Long clubId, Pageable pageable);
}
