import { useState, useEffect } from 'react';
import { Plus, Calendar as CalendarIcon, MapPin, CheckCircle2, ClipboardCheck, ArrowUp, ArrowDown, Check, X, TrendingUp, Clock } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { Button } from '../../ui/button';
import { Card, CardContent } from '../../ui/card';
import { Badge } from '../../ui/badge';
import { Link } from 'react-router-dom';
import { toast } from 'sonner';
import { 
  useUserRole,
  useUserPermissions,
} from '../../../data/userRoles';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../ui/select';
import { getSchedules, getScheduleParticipants, type ScheduleResponse, type ScheduleParticipantResponse } from '../../../../api/schedule';
import { getVotes, getVote, answerVote, type VoteDetailResponse, type VoteAnswerRequest } from '../../../../api/vote';
import { getMyInfo } from '../../../../api/user';
import { getRecentPosts, type PostCardResponse } from '../../../../api/post';

interface Schedule {
  id: number;
  title: string;
  date: string;
  location: string;
  attendees: number;
  status: 'voting' | 'confirmed' | 'ongoing' | 'completed' | 'cancelled';
  dDay: number | null;
  type: 'schedule';
  isToday: boolean;
  isPast: boolean;
  cancelReason?: string;
  isFinalized?: boolean; // 정산 완료 여부
  voteId?: number; // 투표 ID
  myResponse?: 'attending' | 'not_attending' | null; // 내 투표 상태
  entryFee?: number; // 참가비
  myFeeStatus?: 'PAID' | 'PENDING'; // 내 납부 상태
  postLikes?: number; // 연결된 게시글의 좋아요 수
}

export function ScheduleListView() {
  const { groupId } = useParams();
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [loading, setLoading] = useState(true);
  const [sortOrder, setSortOrder] = useState<'newest' | 'oldest' | 'popular' | 'upcoming'>('newest');
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [votingScheduleId, setVotingScheduleId] = useState<number | null>(null);
  
  // 모임별 역할 가져오기
  const permissions = useUserPermissions(groupId || '1');
  
  // 일정 마무리 권한 체크
  const showFinalizeButton = permissions.canFinalizeSchedule;

  // 현재 사용자 정보 조회
  useEffect(() => {
    async function fetchMyInfo() {
      try {
        const userInfo = await getMyInfo();
        setCurrentUserId(userInfo.userId);
      } catch (error) {
        console.error('사용자 정보 조회 실패:', error);
      }
    }
    fetchMyInfo();
  }, []);

  useEffect(() => {
    async function fetchData() {
      if (!groupId) return;
      try {
        setLoading(true);
        const scheduleData = await getSchedules(Number(groupId));

        // 게시글 목록 조회 (일정과 연결된 게시글의 좋아요 수를 위해)
        let posts: PostCardResponse[] = [];
        try {
          posts = await getRecentPosts(Number(groupId), 0, 100);
        } catch (error) {
          console.error('게시글 목록 조회 실패:', error);
        }

        // 투표 목록 조회
        let votes: VoteDetailResponse[] = [];
        try {
          const voteList = await getVotes(Number(groupId));
          // ATTENDANCE 타입 투표만 필터링하고 상세 정보 가져오기
          const attendanceVotes = voteList.filter(v => v.voteType === 'ATTENDANCE');
          votes = await Promise.all(
            attendanceVotes.map(v => getVote(Number(groupId), v.voteId).catch(() => null))
          ).then(results => results.filter((v): v is VoteDetailResponse => v !== null));
        } catch (error) {
          console.error('투표 목록 조회 실패:', error);
        }

        // 각 일정별로 참석자 수 및 투표 정보 조회 (병렬 처리)
        const schedulesWithAttendees = await Promise.all(
          scheduleData.map(async (s) => {
            let attendees = 0;
            let myResponse: 'attending' | 'not_attending' | null = null;
            let myFeeStatus: 'PAID' | 'PENDING' | undefined = undefined;
            let voteId: number | undefined = undefined;
            
            try {
              const participants: ScheduleParticipantResponse[] = await getScheduleParticipants(Number(groupId), s.scheduleId);
              attendees = participants.filter(p => p.attendanceStatus === 'ATTENDING').length;
              
              // 내 참석 상태 및 납부 상태 확인
              if (currentUserId) {
                const myParticipant = participants.find(p => p.userId === currentUserId);
                if (myParticipant) {
                  if (myParticipant.attendanceStatus === 'ATTENDING') {
                    myResponse = 'attending';
                  } else if (myParticipant.attendanceStatus === 'NOT_ATTENDING') {
                    myResponse = 'not_attending';
                  }
                  myFeeStatus = myParticipant.feeStatus as 'PAID' | 'PENDING' | undefined;
                }
              }
            } catch (error) {
              console.error(`일정 ${s.scheduleId} 참석자 조회 실패:`, error);
            }

            // 해당 일정의 투표 찾기
            const scheduleVote = votes.find(v => v.scheduleId === s.scheduleId);
            if (scheduleVote && currentUserId) {
              voteId = scheduleVote.voteId;
              // 내 투표 상태 확인 (투표에서)
              const mySelectedOptions = scheduleVote.options.filter(opt => 
                opt.voters?.some(v => v.userId === currentUserId)
              );
              if (mySelectedOptions.length > 0) {
                const selectedOption = mySelectedOptions[0];
                if (selectedOption.optionText === '참석' || selectedOption.optionText.includes('참석')) {
                  myResponse = 'attending';
                } else if (selectedOption.optionText === '불참' || selectedOption.optionText.includes('불참')) {
                  myResponse = 'not_attending';
                }
              }
            }

            const eventDate = new Date(s.eventDate);
            const endDate = new Date(s.endDate);
            const now = new Date();
            const diffTime = eventDate.getTime() - now.getTime();
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            const isToday = diffDays === 0;
            const isPast = endDate < now;
            
            // 투표 진행 상태 판단: voteDeadline이 있거나 없어도 vote.status === 'OPEN'이면 voting
            let status: 'voting' | 'confirmed' | 'ongoing' | 'completed' | 'cancelled' = 'confirmed';
            if (s.status === 'CANCELLED') status = 'cancelled';
            else if (s.status === 'CLOSED') status = 'completed';
            else if (isToday && !isPast) status = 'ongoing';
            else {
              // 해당 일정의 투표 찾기 (이미 위에서 찾았지만 다시 확인)
              const scheduleVote = votes.find(v => v.scheduleId === s.scheduleId);
              if (scheduleVote && scheduleVote.status === 'OPEN') {
                // voteDeadline이 있고 미래이거나, voteDeadline이 없어도 OPEN이면 voting
                if (!s.voteDeadline || new Date(s.voteDeadline) > now) {
                  status = 'voting';
                }
              }
            }
            
            // 정산 완료 여부 (totalSpent가 존재하면 정산 완료로 간주)
            const isFinalized = s.status === 'CLOSED' && s.totalSpent !== undefined && s.totalSpent > 0;
            
            // 일정과 연결된 게시글 찾기 (좋아요 수를 위해)
            const linkedPost = posts.find(p => p.scheduleId === s.scheduleId);
            const postLikes = linkedPost?.postLikes || 0;
            
            return {
              id: s.scheduleId,
              title: s.scheduleName,
              date: formatScheduleDate(s.eventDate, s.endDate, s.status === 'CANCELLED'),
              location: s.location || '미정',
              attendees,
              status,
              dDay: diffDays > 0 ? diffDays : null,
              type: 'schedule' as const,
              isToday,
              isPast,
              cancelReason: s.cancelReason,
              isFinalized,
              voteId,
              myResponse,
              entryFee: s.entryFee,
              myFeeStatus,
              postLikes,
            };
          })
        );
        
        setSchedules(schedulesWithAttendees as Schedule[]);
      } catch (error) {
        console.error('일정 목록 불러오기 실패:', error);
        toast.error('일정 목록을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, [groupId, currentUserId]);

  function formatScheduleDate(startDate: string, endDate: string, isCancelled: boolean = false): string {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const now = new Date();
    const diffTime = start.getTime() - now.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (isCancelled) return `${formatDate(start)} (취소됨)`;
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
      case 'cancelled':
        return <Badge variant="secondary" className="bg-red-100 text-red-600">취소됨</Badge>;
      case 'completed':
        return <Badge variant="secondary" className="bg-stone-100 text-stone-600">종료됨</Badge>;
      default:
        return <Badge variant="secondary" className="bg-green-100 text-green-700">확정</Badge>;
    }
  };

  // 정렬 적용
  const sortedSchedules = [...schedules].sort((a, b) => {
    if (sortOrder === 'newest') {
      return b.id - a.id;
    } else if (sortOrder === 'oldest') {
      return a.id - b.id;
    } else if (sortOrder === 'popular') {
      // 좋아요 수 기준 내림차순 (좋아요가 같으면 최신순)
      const likesDiff = (b.postLikes || 0) - (a.postLikes || 0);
      return likesDiff !== 0 ? likesDiff : b.id - a.id;
    } else if (sortOrder === 'upcoming') {
      // 일정순: 가장 빨리 다가오는 일정 순 (과거 일정은 뒤로)
      const now = new Date().getTime();
      // date 문자열에서 실제 날짜 추출 (date 필드는 formatScheduleDate 결과이므로 원본 데이터 필요)
      // isPast와 dDay를 기준으로 정렬
      // 과거 일정은 뒤로
      if (a.isPast !== b.isPast) return a.isPast ? 1 : -1;
      // 둘 다 미래 일정인 경우: dDay가 작은 것(더 가까운 일정)이 앞으로
      if (!a.isPast && !b.isPast) {
        const aDDay = a.dDay ?? Infinity;
        const bDDay = b.dDay ?? Infinity;
        return aDDay - bDDay;
      }
      // 둘 다 과거 일정인 경우: 최신순
      return b.id - a.id;
    }
    return b.id - a.id;
  });

  const handleSortChange = (order: 'newest' | 'oldest' | 'popular' | 'upcoming') => {
    setSortOrder(order);
  };

  // 목록 페이지에서 투표 처리
  const handleVoteInList = async (scheduleId: number, response: 'attending' | 'not_attending', voteId: number) => {
    if (!groupId || votingScheduleId === scheduleId) return; // 이미 처리 중이면 스킵
    
    try {
      setVotingScheduleId(scheduleId);
      
      // 투표 옵션 찾기
      const voteDetail = await getVote(Number(groupId), voteId);
      const attendingOption = voteDetail.options.find(opt => 
        opt.optionText === '참석' || opt.optionText.includes('참석')
      );
      const notAttendingOption = voteDetail.options.find(opt => 
        opt.optionText === '불참' || opt.optionText.includes('불참')
      );
      
      // 토글 기능: 같은 버튼을 다시 누르면 취소 (null)
      const currentSchedule = schedules.find(s => s.id === scheduleId);
      let selectedOptionId: number | undefined;
      let isCancelling = false;
      
      if ((response === 'attending' && currentSchedule?.myResponse === 'attending') || 
          (response === 'not_attending' && currentSchedule?.myResponse === 'not_attending')) {
        // 이미 선택된 옵션을 다시 클릭하면 취소
        isCancelling = true;
        // 빈 배열을 보내면 투표 취소 (백엔드가 지원하는 경우)
        // 지원하지 않으면 다른 옵션으로 변경
        selectedOptionId = response === 'attending' 
          ? notAttendingOption?.optionId 
          : attendingOption?.optionId;
      } else {
        // 새로운 선택
        selectedOptionId = response === 'attending' 
          ? attendingOption?.optionId 
          : notAttendingOption?.optionId;
      }
      
      if (!selectedOptionId) {
        toast.error('투표 옵션을 찾을 수 없습니다');
        return;
      }
      
      const request: VoteAnswerRequest = {
        optionIds: isCancelling ? [] : [selectedOptionId]
      };
      await answerVote(Number(groupId), voteId, request);
      
      // 즉시 서버 데이터로 동기화
      const scheduleData = await getSchedules(Number(groupId));
      const participantsData = await Promise.all(
        scheduleData.map(s => getScheduleParticipants(Number(groupId), s.scheduleId).catch(() => []))
      );
      
      // 게시글 목록 조회 (좋아요 수를 위해)
      let refreshedPosts: PostCardResponse[] = [];
      try {
        refreshedPosts = await getRecentPosts(Number(groupId), 0, 100);
      } catch (error) {
        console.error('게시글 목록 조회 실패:', error);
      }
      
      // 투표 목록 다시 조회
      const voteList = await getVotes(Number(groupId));
      const attendanceVotes = voteList.filter(v => v.voteType === 'ATTENDANCE');
      const votes = await Promise.all(
        attendanceVotes.map(v => getVote(Number(groupId), v.voteId).catch(() => null))
      ).then(results => results.filter((v): v is VoteDetailResponse => v !== null));
      
      // 상태 업데이트
      const updatedSchedules = await Promise.all(
        scheduleData.map(async (s, idx) => {
          const participants: ScheduleParticipantResponse[] = participantsData[idx];
          const attendees = participants.filter(p => p.attendanceStatus === 'ATTENDING').length;
          
          let myResponse: 'attending' | 'not_attending' | null = null;
          let myFeeStatus: 'PAID' | 'PENDING' | undefined = undefined;
          
          if (currentUserId) {
            const myParticipant = participants.find(p => p.userId === currentUserId);
            if (myParticipant) {
              if (myParticipant.attendanceStatus === 'ATTENDING') {
                myResponse = 'attending';
              } else if (myParticipant.attendanceStatus === 'NOT_ATTENDING') {
                myResponse = 'not_attending';
              }
              myFeeStatus = myParticipant.feeStatus as 'PAID' | 'PENDING' | undefined;
            }
          }
          
          const scheduleVote = votes.find(v => v.scheduleId === s.scheduleId);
          const eventDate = new Date(s.eventDate);
          const endDate = new Date(s.endDate);
          const now = new Date();
          const diffTime = eventDate.getTime() - now.getTime();
          const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
          const isToday = diffDays === 0;
          const isPast = endDate < now;
          
          // 투표 진행 상태 판단: voteDeadline이 있거나 없어도 vote.status === 'OPEN'이면 voting
          let status: 'voting' | 'confirmed' | 'ongoing' | 'completed' | 'cancelled' = 'confirmed';
          if (s.status === 'CANCELLED') status = 'cancelled';
          else if (s.status === 'CLOSED') status = 'completed';
          else if (isToday && !isPast) status = 'ongoing';
          else {
            if (scheduleVote && scheduleVote.status === 'OPEN') {
              // voteDeadline이 있고 미래이거나, voteDeadline이 없어도 OPEN이면 voting
              if (!s.voteDeadline || new Date(s.voteDeadline) > now) {
                status = 'voting';
              }
            }
          }
          
          const isFinalized = s.status === 'CLOSED' && s.totalSpent !== undefined && s.totalSpent > 0;
          
          // 일정과 연결된 게시글 찾기 (좋아요 수를 위해)
          const linkedPost = refreshedPosts.find(p => p.scheduleId === s.scheduleId);
          const postLikes = linkedPost?.postLikes || 0;
          
          return {
            id: s.scheduleId,
            title: s.scheduleName,
            date: formatScheduleDate(s.eventDate, s.endDate, s.status === 'CANCELLED'),
            location: s.location || '미정',
            attendees,
            status,
            dDay: diffDays > 0 ? diffDays : null,
            type: 'schedule' as const,
            isToday,
            isPast,
            cancelReason: s.cancelReason,
            isFinalized,
            voteId: scheduleVote?.voteId,
            myResponse,
            entryFee: s.entryFee,
            myFeeStatus,
            postLikes,
          };
        })
      );
      
      setSchedules(updatedSchedules as Schedule[]);
      
      if (isCancelling) {
        toast.success('투표가 취소되었습니다');
      } else {
        toast.success(response === 'attending' ? '참석으로 응답했습니다' : '불참으로 응답했습니다');
      }
    } catch (error) {
      console.error('투표 실패:', error);
      toast.error('투표에 실패했습니다.');
    } finally {
      setVotingScheduleId(null);
    }
  };

  return (
    <div className="space-y-4 pb-20" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* 정렬 옵션 */}
      <div className="flex justify-start items-center">
        <Select value={sortOrder} onValueChange={(value) => handleSortChange(value as 'newest' | 'oldest' | 'popular' | 'upcoming')}>
          <SelectTrigger className="w-[130px] h-9 text-stone-600 px-3">
            <SelectValue>
              <>
                {sortOrder === 'newest' && (
                  <span className="flex items-center gap-2">
                    <ArrowDown className="w-4 h-4" />
                    최신순
                  </span>
                )}
                {sortOrder === 'oldest' && (
                  <span className="flex items-center gap-2">
                    <ArrowUp className="w-4 h-4" />
                    오래된순
                  </span>
                )}
                {sortOrder === 'popular' && (
                  <span className="flex items-center gap-2">
                    <TrendingUp className="w-4 h-4" />
                    인기순
                  </span>
                )}
                {sortOrder === 'upcoming' && (
                  <span className="flex items-center gap-2">
                    <Clock className="w-4 h-4" />
                    일정순
                  </span>
                )}
              </>
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="newest">
              <span className="flex items-center gap-2">
                <ArrowDown className="w-4 h-4" />
                최신순
              </span>
            </SelectItem>
            <SelectItem value="oldest">
              <span className="flex items-center gap-2">
                <ArrowUp className="w-4 h-4" />
                오래된순
              </span>
            </SelectItem>
            <SelectItem value="popular">
              <span className="flex items-center gap-2">
                <TrendingUp className="w-4 h-4" />
                인기순
              </span>
            </SelectItem>
            <SelectItem value="upcoming">
              <span className="flex items-center gap-2">
                <Clock className="w-4 h-4" />
                일정순
              </span>
            </SelectItem>
          </SelectContent>
        </Select>
      </div>

      {sortedSchedules.length > 0 ? (
        sortedSchedules.map((item) => {
        const detailVariant: 'default' | 'outline' = item.status === 'voting' ? 'default' : 'outline';
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
                {/* 취소된 일정의 취소 사유 표시 */}
                {item.status === 'cancelled' && item.cancelReason && (
                  <div className="mt-2 p-2 bg-red-50 rounded-lg border border-red-100">
                    <p className="text-xs text-red-700">
                      <span className="font-medium">취소 사유:</span> {item.cancelReason}
                    </p>
                  </div>
                )}
              </div>
              
              <div className="mt-4 pt-3 border-t border-stone-100 space-y-3">
                {/* 투표 버튼 (투표 중인 일정만) */}
                {item.status === 'voting' && item.voteId && item.status !== 'cancelled' && (
                  <div className="flex gap-2">
                    {(() => {
                      const attendingVariant: 'default' | 'outline' = item.myResponse === 'attending' ? 'default' : 'outline';
                      return (
                        <Button
                          size="sm"
                          variant={attendingVariant}
                          className={`flex-1 h-10 ${
                            item.myResponse === 'attending' 
                              ? 'bg-green-500 hover:bg-green-600 text-white' 
                              : 'border-stone-200'
                          }`}
                          onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            handleVoteInList(item.id, 'attending', item.voteId!);
                          }}
                          disabled={votingScheduleId === item.id}
                        >
                          <Check className="w-4 h-4 mr-1" />
                          참석
                        </Button>
                      );
                    })()}
                    {(() => {
                      const notAttendingVariant: 'default' | 'outline' = item.myResponse === 'not_attending' ? 'default' : 'outline';
                      return (
                        <Button
                          size="sm"
                          variant={notAttendingVariant}
                          className={`flex-1 h-10 ${
                            item.myResponse === 'not_attending' 
                              ? 'bg-red-500 hover:bg-red-600 text-white' 
                              : 'border-stone-200'
                          }`}
                          onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            handleVoteInList(item.id, 'not_attending', item.voteId!);
                          }}
                          disabled={votingScheduleId === item.id}
                        >
                          <X className="w-4 h-4 mr-1" />
                          불참
                        </Button>
                      );
                    })()}
                  </div>
                )}
                
                {/* 입금 필요 알림 (참석했는데 입금 안한 경우) */}
                {item.myResponse === 'attending' && item.entryFee && item.entryFee > 0 && item.myFeeStatus !== 'PAID' && (
                  <div className="p-2 bg-orange-50 rounded-lg border border-orange-200">
                    <p className="text-xs font-medium text-orange-700">
                      입금 필요: {item.entryFee.toLocaleString()}원
                    </p>
                  </div>
                )}
                
                <div className="flex justify-between items-center">
                  <div className="flex gap-2">
                    {/* 일정 마무리 버튼 - 모임장/총무/운영진만 보임, 취소된 일정 제외 */}
                    {showFinalizeButton && (item.isToday || item.isPast) && item.status !== 'voting' && item.status !== 'cancelled' && (
                      <Link to={`${item.id}`} onClick={(e) => e.stopPropagation()}>
                        <Button 
                          size="sm" 
                          variant="outline"
                          className="border-purple-300 text-purple-600 hover:bg-purple-50"
                        >
                          <ClipboardCheck className="w-4 h-4 mr-1" />
                          {item.isFinalized ? '정산 확인' : '일정 마무리'}
                        </Button>
                      </Link>
                    )}
                  </div>
                  
                  <Link to={`${item.id}`}>
                    <Button 
                      size="sm" 
                      variant={detailVariant} 
                      className={item.status === 'voting' ? 'bg-orange-500 hover:bg-orange-600' : ''}
                    >
                      상세보기
                    </Button>
                  </Link>
                </div>
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
      <div className="fixed bottom-24 right-8 z-40">
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
