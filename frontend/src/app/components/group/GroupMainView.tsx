import { useState, useEffect } from 'react';
import { ChevronRight, Users, Calendar, MapPin } from 'lucide-react';
import { Card, CardContent } from '../ui/card';
import { Badge } from '../ui/badge';
import { Progress } from '../ui/progress';
import { Avatar, AvatarFallback } from '../ui/avatar';
import { Link, useOutletContext } from 'react-router-dom';
import { ClubDetailResponse } from '@/api/club-full';
import { getSchedules, type ScheduleResponse } from '@/api/schedule';
import { getRecentPosts, type PostCardResponse } from '@/api/post';

interface GroupContextType {
  club: ClubDetailResponse | null;
  loading: boolean;
}

const CATEGORY_LABELS: Record<string, string> = {
  STUDY: '스터디',
  SPORTS: '운동',
  SOCIAL: '친목',
  HOBBY: '취미',
  FINANCE: '재테크',
  ETC: '기타',
};

export function GroupMainView() {
  const { club, loading } = useOutletContext<GroupContextType>();
  const [nextSchedule, setNextSchedule] = useState<ScheduleResponse | null>(null);
  const [recentPosts, setRecentPosts] = useState<PostCardResponse[]>([]);
  const [schedulesLoading, setSchedulesLoading] = useState(false);
  const [postsLoading, setPostsLoading] = useState(false);

  useEffect(() => {
    async function fetchData() {
      if (!club?.clubId) return;
      
      // 다음 일정 가져오기
      try {
        setSchedulesLoading(true);
        const schedules = await getSchedules(club.clubId);
        const now = new Date();
        // 미래 일정 중 가장 가까운 일정 찾기
        const upcomingSchedules = schedules
          .filter(s => new Date(s.eventDate) > now && s.status !== 'CLOSED' && s.status !== 'CANCELLED')
          .sort((a, b) => new Date(a.eventDate).getTime() - new Date(b.eventDate).getTime());
        setNextSchedule(upcomingSchedules[0] || null);
      } catch (error) {
        console.error('일정 불러오기 실패:', error);
      } finally {
        setSchedulesLoading(false);
      }

      // 최근 게시글 가져오기
      try {
        setPostsLoading(true);
        const posts = await getRecentPosts(club.clubId, 0, 3);
        setRecentPosts(posts);
      } catch (error) {
        console.error('게시글 불러오기 실패:', error);
      } finally {
        setPostsLoading(false);
      }
    }

    if (club?.clubId) {
      fetchData();
    }
  }, [club?.clubId]);

  const formatScheduleDate = (dateString: string): string => {
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = date.getTime() - now.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) return `오늘 ${date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}`;
    if (diffDays === 1) return `내일 ${date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}`;
    
    return date.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' }) + 
           ' ' + date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  };

  if (loading) {
    return (
      <div className="space-y-6 pb-20">
        <div className="bg-white rounded-2xl p-5 shadow-sm border border-stone-100 animate-pulse">
          <div className="h-20 bg-stone-200 rounded-2xl"></div>
        </div>
      </div>
    );
  }

  if (!club) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-stone-500">모임 정보를 불러올 수 없습니다.</p>
      </div>
    );
  }

  const categoryLabel = club.category ? CATEGORY_LABELS[club.category] || '기타' : '기타';

  return (
    <div className="space-y-6 pb-20" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* Group Profile */}
      <section className="bg-white rounded-2xl p-5 shadow-sm border border-stone-100">
        <div className="flex items-start gap-4">
          <Avatar className="w-20 h-20 rounded-2xl border-2 border-white shadow-md" draggable={false}>
            {(() => {
              const clubImage = localStorage.getItem(`club_image_${club.clubId}`);
              return clubImage ? (
                <img src={clubImage} alt={club.clubName} className="w-full h-full object-cover rounded-2xl" />
              ) : (
                <AvatarFallback className="text-2xl bg-orange-100 text-orange-600">
                  {club.clubName[0]}
                </AvatarFallback>
              );
            })()}
          </Avatar>
          <div className="flex-1 space-y-1">
            <h2 className="text-xl font-bold text-stone-900">{club.clubName}</h2>
            <p className="text-sm text-stone-500">
              {club.visibility === 'PUBLIC' ? '공개 모임' : '비공개 모임'}
            </p>
            <div className="flex flex-wrap gap-1 pt-1">
              <Badge variant="secondary" className="bg-stone-100 text-stone-600 font-normal">
                #{categoryLabel}
              </Badge>
              {club.type === 'FAIR_SETTLEMENT' && (
                <Badge variant="secondary" className="bg-green-100 text-green-700 font-normal">
                  공정정산형
                </Badge>
              )}
            </div>
          </div>
        </div>

        <div className="mt-5 space-y-2">
          <div className="flex justify-between text-sm">
            <span className="flex items-center text-stone-600">
              <Users className="w-4 h-4 mr-1" />
              멤버 {club.currentMembers || 1}명
            </span>
            <span className="text-stone-400">최대 {club.maxMembers || 100}명</span>
          </div>
          <Progress
            value={((club.currentMembers || 1) / (club.maxMembers || 100)) * 100}
            className="h-2 bg-stone-100"
            indicatorClassName="bg-orange-500"
          />
        </div>
      </section>

      {/* Next Event */}
      <section>
        <div className="flex items-center justify-between mb-2 px-1">
          <h3 className="font-bold text-lg text-stone-800">다음 일정</h3>
          <Link to="schedule" className="text-xs text-stone-500 hover:text-orange-500 flex items-center">
            전체보기 <ChevronRight className="w-3 h-3 ml-0.5" />
          </Link>
        </div>
        {schedulesLoading ? (
          <Card className="border-stone-200 shadow-sm">
            <CardContent className="p-4 text-center text-stone-500">
              <p className="text-sm">로딩 중...</p>
            </CardContent>
          </Card>
        ) : nextSchedule ? (
          <Link to={`schedule/${nextSchedule.scheduleId}`}>
            <Card className="border-stone-200 shadow-sm hover:border-orange-300 transition-colors cursor-pointer">
              <CardContent className="p-4">
                <h4 className="font-bold text-stone-900 mb-2">{nextSchedule.scheduleName}</h4>
                <div className="space-y-1.5 text-sm text-stone-600">
                  <div className="flex items-center gap-2">
                    <Calendar className="w-4 h-4 text-stone-400" />
                    <span>{formatScheduleDate(nextSchedule.eventDate)}</span>
                  </div>
                  {nextSchedule.location && (
                    <div className="flex items-center gap-2">
                      <MapPin className="w-4 h-4 text-stone-400" />
                      <span>{nextSchedule.location}</span>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </Link>
        ) : (
          <Card className="border-stone-200 shadow-sm">
            <CardContent className="p-4 text-center text-stone-500">
              <Calendar className="w-12 h-12 mx-auto mb-2 text-stone-300" />
              <p className="text-sm">예정된 일정이 없습니다</p>
              <Link to="schedule" className="text-xs text-orange-500 hover:underline mt-2 inline-block">
                일정 만들기
              </Link>
            </CardContent>
          </Card>
        )}
      </section>

      {/* Recent Posts */}
      <section>
        <div className="flex items-center justify-between mb-2 px-1">
          <h3 className="font-bold text-lg text-stone-800">최근 게시글</h3>
          <Link to="posts" className="text-xs text-stone-500 hover:text-orange-500 flex items-center">
            더보기 <ChevronRight className="w-3 h-3 ml-0.5" />
          </Link>
        </div>
        {postsLoading ? (
          <Card className="border-stone-200 shadow-sm">
            <CardContent className="p-4 text-center text-stone-500">
              <p className="text-sm">로딩 중...</p>
            </CardContent>
          </Card>
        ) : recentPosts.length > 0 ? (
          <div className="space-y-2">
            {recentPosts.map((post) => (
              <Link key={post.postId} to={`posts/${post.postId}`}>
                <Card className="border-stone-200 shadow-sm hover:border-orange-300 transition-colors cursor-pointer">
                  <CardContent className="p-4">
                    <div className="flex gap-3">
                      {post.imagesUrl && post.imagesUrl.length > 0 && (
                        <div className="w-16 h-16 flex-shrink-0 rounded-lg overflow-hidden bg-stone-100">
                          <img 
                            src={post.imagesUrl[0]} 
                            alt="" 
                            className="w-full h-full object-cover"
                            onError={(e) => {
                              const target = e.target as HTMLImageElement;
                              target.style.display = 'none';
                            }}
                          />
                        </div>
                      )}
                      <div className="flex-1 min-w-0">
                        <h4 className="font-medium text-stone-900 text-sm line-clamp-1 mb-1">
                          {post.title || post.content.substring(0, 30)}
                        </h4>
                        <p className="text-xs text-stone-500 line-clamp-2">
                          {post.content}
                        </p>
                        <div className="flex items-center gap-3 mt-1.5 text-xs text-stone-400">
                          <span>{post.writerName}</span>
                          <span>좋아요 {post.postLikes}</span>
                          <span>댓글 {post.commentCount}</span>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        ) : (
          <Card className="border-stone-200 shadow-sm">
            <CardContent className="p-4 text-center text-stone-500">
              <p className="text-sm">아직 게시글이 없습니다</p>
              <Link to="posts" className="text-xs text-orange-500 hover:underline mt-2 inline-block">
                첫 게시글 작성하기
              </Link>
            </CardContent>
          </Card>
        )}
      </section>
    </div>
  );
}
