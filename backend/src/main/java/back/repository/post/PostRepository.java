package back.repository.post;

import back.domain.post.PostCategory;
import back.domain.post.Posts;
import back.dto.post.post.response.PostCardBase;
import back.repository.post.projection.RecentAlbumRow;
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
      select new back.dto.post.post.response.PostCardBase(
          p.club.clubId,
          p.postId,
          p.writer.userId,
          p.writer.realName,
          p.title,
          p.content,
          coalesce(count(distinct l.likeId), 0L),
          coalesce(count(distinct c.commentId), 0L),
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

    /**
     * 특정 모임의 특정 카테고리 게시글 중 삭제되지 않은 게시글들을 조회합니다.
     */
    List<Posts> findByClub_ClubIdAndCategoryAndDeletedAtIsNull(Long clubId, PostCategory category);
}
