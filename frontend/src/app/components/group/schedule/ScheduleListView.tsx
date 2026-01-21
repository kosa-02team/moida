import { useState, useEffect } from 'react';
import { Plus, Calendar as CalendarIcon, MapPin, CheckCircle2, ClipboardCheck } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { Button } from '../../ui/button';
import { Card, CardContent } from '../../ui/card';
import { Badge } from '../../ui/badge';
import { Link } from 'react-router-dom';
import { toast } from 'sonner';
import { 
  useUserRole,
  useUserPermissions,
  getRoleLabel,
  getRoleColor,
} from '../../../data/userRoles';
import { getSchedules, getScheduleParticipants, type ScheduleResponse } from '../../../../api/schedule';
import { getMembers } from '../../../../api/member';
import { getMyInfo } from '../../../../api/user';

interface Schedule {
  id: number;
  title: string;
  date: string;
  location: string;
  attendees: number;
  status: 'voting' | 'confirmed' | 'ongoing' | 'completed';
  dDay: number | null;
  type: 'schedule';
  isToday: boolean;
  isPast: boolean;
}

export function ScheduleListView() {
  const { groupId } = useParams();
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [loading, setLoading] = useState(true);
  const [userRole, setUserRole] = useState<string>('');
  const [roleColor, setRoleColor] = useState<string>('');
  
  // 모임별 역할 가져오기
  const { userRole: mockUserRole } = useUserRole(groupId || '1');
  const permissions = useUserPermissions(groupId || '1');
  
  // 일정 마무리 권한 체크
  const showFinalizeButton = permissions.canFinalizeSchedule;

  // 실제 역할 정보 가져오기 (운영진 이상만 표시)
  useEffect(() => {
    async function fetchUserRole() {
      if (!groupId) return;
      try {
        const [myInfo, members] = await Promise.all([
          getMyInfo(),
          getMembers(Number(groupId), 'ACTIVE')
        ]);
        const currentMember = members.find(m => m.userId === myInfo.userId);
        if (currentMember) {
          const roles = currentMember.roles || [];
          // 역할 우선순위: OWNER > ACCOUNTANT > STAFF > MEMBER
          if (roles.includes('OWNER')) {
            setUserRole('모임장');
            setRoleColor('bg-orange-500 text-white');
          } else if (roles.includes('ACCOUNTANT')) {
            setUserRole('총무');
            setRoleColor('bg-green-500 text-white');
          } else if (roles.includes('STAFF')) {
            setUserRole('운영진');
            setRoleColor('bg-blue-500 text-white');
          } else {
            setUserRole('회원');
            setRoleColor('bg-stone-500 text-white');
          }
        }
      } catch (error) {
        console.error('사용자 역할 조회 실패:', error);
      }
    }
    fetchUserRole();
  }, [groupId]);

  useEffect(() => {
    async function fetchData() {
      if (!groupId) return;
      try {
        setLoading(true);
        const scheduleData = await getSchedules(Number(groupId));

        // 각 일정별로 참석자 수 조회 (병렬 처리)
        const schedulesWithAttendees = await Promise.all(
          scheduleData.map(async (s) => {
            let attendees = 0;
            try {
              const participants = await getScheduleParticipants(Number(groupId), s.scheduleId);
              attendees = participants.filter(p => p.attendanceStatus === 'ATTENDING').length;
            } catch (error) {
              console.error(`일정 ${s.scheduleId} 참석자 조회 실패:`, error);
            }

            const eventDate = new Date(s.eventDate);
            const endDate = new Date(s.endDate);
            const now = new Date();
            const diffTime = eventDate.getTime() - now.getTime();
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            const isToday = diffDays === 0;
            const isPast = endDate < now;
            
            // voteDeadline이 있고 아직 마감 전이면 투표중 상태
            let status: 'voting' | 'confirmed' | 'ongoing' | 'completed' = 'confirmed';
            if (s.status === 'CLOSED') status = 'completed';
            else if (s.status === 'CANCELLED') status = 'completed';
            else if (isToday && !isPast) status = 'ongoing';
            else if (s.voteDeadline && new Date(s.voteDeadline) > now) status = 'voting';
            
            return {
              id: s.scheduleId,
              title: s.scheduleName,
              date: formatScheduleDate(s.eventDate, s.endDate),
              location: s.location || '미정',
              attendees,
              status,
              dDay: diffDays > 0 ? diffDays : null,
              type: 'schedule' as const,
              isToday,
              isPast,
            };
          })
        );
        
        // 날짜순 정렬 (최신순)
        schedulesWithAttendees.sort((a, b) => b.id - a.id);
        
        setSchedules(schedulesWithAttendees);
      } catch (error) {
        console.error('일정 목록 불러오기 실패:', error);
        toast.error('일정 목록을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, [groupId]);

  function formatScheduleDate(startDate: string, endDate: string): string {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const now = new Date();
    const diffTime = start.getTime() - now.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) return `오늘 ${formatTime(start)}`;
    if (diffDays === 1) return `내일 ${formatTime(start)}`;
    if (diffDays < 0) return `${formatDate(end)} 종료됨`;
    
    return `${formatDate(start)} ${formatTime(start)}`;
  }

  function formatDate(date: Date): string {
    return date.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' });
  }

  function formatTime(date: Date): string {
    return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  }

  if (loading) {
    return (
      <div className="space-y-4 pb-20">
        <div className="text-center py-8 text-stone-500">로딩 중...</div>
      </div>
    );
  }

  const getStatusBadge = (item: Schedule) => {
    switch (item.status) {
      case 'voting':
        return <Badge variant="outline" className="border-orange-500 text-orange-600 bg-orange-50">투표중</Badge>;
      case 'ongoing':
        return <Badge className="bg-blue-500 text-white animate-pulse">진행중</Badge>;
      case 'completed':
        return <Badge variant="secondary" className="bg-stone-100 text-stone-600">종료됨</Badge>;
      default:
        return <Badge variant="secondary" className="bg-green-100 text-green-700">확정</Badge>;
    }
  };

  return (
    <div className="space-y-4 pb-20" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* User Role Badge */}
      {userRole && userRole !== '회원' && (
        <div className="flex justify-end">
          <Badge className={`${roleColor} text-xs`}>
            {userRole}
          </Badge>
        </div>
      )}

      {schedules.length > 0 ? (
        schedules.map((item) => {
        return (
          <Card 
            key={item.id}
            className={`border-stone-100 shadow-sm bg-white ${
              item.status === 'ongoing' ? 'border-blue-300 border-2' : ''
            }`}
          >
            <CardContent className="p-4">
              <div className="flex justify-between items-start mb-2">
                <div className="flex gap-2 items-center">
                  {getStatusBadge(item)}
                  {item.dDay !== null && item.dDay > 0 && (
                    <span className="text-xs font-bold text-orange-500">D-{item.dDay}</span>
                  )}
                  {item.isToday && (
                    <Badge className="bg-red-500 text-white text-xs">오늘</Badge>
                  )}
                </div>
              </div>
              
              <Link to={`${item.id}`}>
                <h3 className="text-lg font-bold text-stone-900 mb-3 hover:text-orange-600 transition-colors">
                  {item.title}
                </h3>
              </Link>
              
              <div className="space-y-2 text-sm text-stone-600">
                <div className="flex items-center gap-2">
                  <CalendarIcon className="w-4 h-4 text-stone-400" />
                  <span>{item.date}</span>
                </div>
                <div className="flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-stone-400" />
                  <span>{item.location}</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-stone-400" />
                  <span>{item.attendees}명 {item.status === 'voting' ? '투표' : '참석'}</span>
                </div>
              </div>
              
              <div className="mt-4 pt-3 border-t border-stone-100 flex justify-between items-center">
                <div className="flex gap-2">
                  {/* 일정 마무리 버튼 - 모임장/총무/운영진만 보임 */}
                  {showFinalizeButton && (item.isToday || item.isPast) && item.status !== 'voting' && (
                    <Link to={`${item.id}/finalize`} onClick={(e) => e.stopPropagation()}>
                      <Button 
                        size="sm" 
                        variant="outline"
                        className="border-purple-300 text-purple-600 hover:bg-purple-50"
                      >
                        <ClipboardCheck className="w-4 h-4 mr-1" />
                        {item.isPast && item.status === 'completed' ? '정산 확인' : '일정 마무리'}
                      </Button>
                    </Link>
                  )}
                </div>
                
                <Link to={`${item.id}`}>
                  <Button 
                    size="sm" 
                    variant={item.status === 'voting' ? 'default' : 'outline'} 
                    className={item.status === 'voting' ? 'bg-orange-500 hover:bg-orange-600' : ''}
                  >
                    {item.status === 'voting' ? '투표하러 가기' : '상세보기'}
                  </Button>
                </Link>
              </div>
            </CardContent>
          </Card>
        );
        })
      ) : (
        <div className="text-center py-12 text-stone-500">
          <p className="text-sm">아직 일정이 없습니다</p>
        </div>
      )}
      
      {/* FAB */}
      <div className="fixed bottom-24 right-4 z-40">
        <Link to="create-vote">
          <Button className="rounded-full h-12 px-6 shadow-lg bg-stone-900 hover:bg-stone-800 text-white flex items-center gap-2">
            <Plus className="w-5 h-5" />
            <span>일정 만들기</span>
          </Button>
        </Link>
      </div>
    </div>
  );
}
