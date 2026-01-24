import { useState, useEffect, useCallback } from 'react';
import { Plus, Calendar as CalendarIcon, MapPin, CheckCircle2, ClipboardCheck, ArrowUp, ArrowDown, Check, X, Search } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { Button } from '../../ui/button';
import { Card, CardContent } from '../../ui/card';
import { Badge } from '../../ui/badge';
import { Input } from '../../ui/input';
import { Link } from 'react-router-dom';
import { toast } from 'sonner';
import {
  useUserPermissions,
} from '../../../data/userRoles';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../ui/select';
import { getSchedules, getScheduleParticipants, type ScheduleParticipantResponse } from '../../../../api/schedule';
import { getVotes, getVote, answerVote, type VoteDetailResponse, type VoteAnswerRequest } from '../../../../api/vote';
import { getMyInfo } from '../../../../api/user';
import { getRecentPosts, type PostCardResponse } from '../../../../api/post';
import { getBankAccount, type BankAccounts } from '../../../../api/bank';
import { Copy } from 'lucide-react';
import { QAChatWidget } from '../stories/QAChatWidget';

interface Schedule {
  id: number;
  title: string;
  date: string;
  eventDate: string; // 일정 시작일 (정렬용)
  location: string;
  attendees: number;
  status: 'voting' | 'confirmed' | 'ongoing' | 'completed' | 'cancelled';
  dDay: number | null;
  type: 'schedule';
  isToday: boolean;
  isPast: boolean;
  isEventStarted: boolean; // 일정이 시작되었는지
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
  const [sortOrder, setSortOrder] = useState<'newest' | 'oldest'>('newest');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [votingScheduleId, setVotingScheduleId] = useState<number | null>(null);
  const [bankAccount, setBankAccount] = useState<BankAccounts | null>(null);
  const [copiedAccount, setCopiedAccount] = useState(false);

  // 모임별 역할 가져오기
  const permissions = useUserPermissions(groupId || '1');

  // 일정 마무리 버튼 표시 여부 체크 함수
  const canShowFinalizeButton = (entryFee?: number) => {
    if (entryFee && entryFee > 0) {
      // 참가비가 있으면 총무 이상만
      return permissions.canWithdraw;
    } else {
      // 참가비가 없으면 운영진 이상
      return permissions.canFinalizeSchedule;
    }
  };

  // 은행 코드를 은행 이름으로 변환
  const getBankName = (bankCode: string): string => {
    const bankMap: Record<string, string> = {
      'KB': 'KB국민은행',
      'NH': 'NH농협은행',
      'SHINHAN': '신한은행',
      'WOORI': '우리은행',
      'HANA': '하나은행',
      'KAKAO': '카카오뱅크',
      'TOSS': '토스뱅크',
      'STUB': '오픈은행',
    };
    return bankMap[bankCode] || bankCode;
  };

  // 현재 사용자 정보 및 계좌 정보 조회
  useEffect(() => {
    async function fetchMyInfo() {
      try {
        const userInfo = await getMyInfo();
        setCurrentUserId(userInfo.userId);
      } catch (error) {
        console.error('사용자 정보 조회 실패:', error);
      }
    }
    async function fetchBankAccount() {
      if (!groupId) return;
      try {
        const account = await getBankAccount(Number(groupId));
        setBankAccount(account);
      } catch (error) {
        console.error('계좌 정보 조회 실패:', error);
      }
    }
    fetchMyInfo();
    fetchBankAccount();
  }, [groupId]);

  // fetchData 함수를 useCallback으로 정의하여 이벤트 리스너에서도 사용 가능하도록
  const fetchData = useCallback(async () => {
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
          const isEventStarted = eventDate <= now; // 일정이 시작되었는지 확인

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
            eventDate: s.eventDate, // 정렬용 원본 날짜
            location: s.location || '미정',
            attendees,
            status,
            dDay: diffDays > 0 ? diffDays : null,
            type: 'schedule' as const,
            isToday,
            isPast,
            isEventStarted,
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
  }, [groupId, currentUserId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // 납부 상태 업데이트 이벤트 리스너
  useEffect(() => {
    const handleFeeStatusUpdate = (event: Event) => {
      const customEvent = event as CustomEvent<{ scheduleId: number; groupId: number }>;
      // 같은 그룹의 일정이면 새로고침
      if (customEvent.detail.groupId === Number(groupId)) {
        fetchData();
      }
    };

    window.addEventListener('scheduleFeeStatusUpdated', handleFeeStatusUpdate);
    return () => window.removeEventListener('scheduleFeeStatusUpdated', handleFeeStatusUpdate);
  }, [groupId, fetchData]);

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

  const statusGroup: Record<Schedule['status'], number> = {
    ongoing: 0,
    confirmed: 0,
    voting: 0,
    completed: 1,
    cancelled: 1,
  };

  const activeStatusPriority: Record<Schedule['status'], number> = {
    ongoing: 0,
    confirmed: 1,
    voting: 2,
    completed: 0,
    cancelled: 0,
  };

  // 검색 및 정렬 적용 (일정 시작일 기준)
  const filteredAndSortedSchedules = [...schedules]
    .filter(schedule => {
      if (!searchQuery.trim()) return true;
      return schedule.title.toLowerCase().includes(searchQuery.toLowerCase());
    })
    .sort((a, b) => {
      const aDate = new Date(a.eventDate).getTime();
      const bDate = new Date(b.eventDate).getTime();
      const aGroup = statusGroup[a.status] ?? 1;
      const bGroup = statusGroup[b.status] ?? 1;

      if (aGroup !== bGroup) {
        return aGroup - bGroup;
      }

      if (aGroup === 0) {
        const aPriority = activeStatusPriority[a.status] ?? 99;
        const bPriority = activeStatusPriority[b.status] ?? 99;
        if (aPriority !== bPriority) {
          return aPriority - bPriority;
        }
      }

      if (sortOrder === 'newest') {
        // 최신순: 일정 시작일이 늦은 순서 (내림차순)
        return bDate - aDate;
      }

      // 오래된순: 일정 시작일이 빠른 순서 (오름차순)
      return aDate - bDate;
    });

  const handleSortChange = (order: 'newest' | 'oldest') => {
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

      // 선택한 옵션 결정
      const selectedOptionId = response === 'attending'
        ? attendingOption?.optionId
        : notAttendingOption?.optionId;

      if (!selectedOptionId) {
        toast.error('투표 옵션을 찾을 수 없습니다');
        return;
      }

      const request: VoteAnswerRequest = {
        optionIds: [selectedOptionId]
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
          const isEventStarted = eventDate <= now; // 일정이 시작되었는지 확인

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
            eventDate: s.eventDate, // 정렬용 원본 날짜
            location: s.location || '미정',
            attendees,
            status,
            dDay: diffDays > 0 ? diffDays : null,
            type: 'schedule' as const,
            isToday,
            isPast,
            isEventStarted,
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

      toast.success(response === 'attending' ? '참석으로 응답했습니다' : '불참으로 응답했습니다');
    } catch (error) {
      console.error('투표 실패:', error);
      toast.error('투표에 실패했습니다.');
    } finally {
      setVotingScheduleId(null);
    }
  };

  return (
    <div className="space-y-4 pb-20" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* 검색 및 정렬 옵션 */}
      <div className="flex gap-2 items-center">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400" />
          <Input
            type="text"
            placeholder="일정 제목으로 검색..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-9 h-9 bg-white border-stone-200"
          />
        </div>
        <Select value={sortOrder} onValueChange={(value) => handleSortChange(value as 'newest' | 'oldest')}>
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
          </SelectContent>
        </Select>
      </div>

      {filteredAndSortedSchedules.length > 0 ? (
        filteredAndSortedSchedules.map((item) => {
          const detailVariant: 'default' | 'outline' = item.status === 'voting' ? 'default' : 'outline';
          return (
            <Card
              key={item.id}
              className={`border-stone-100 shadow-sm bg-white ${item.status === 'ongoing' ? 'border-blue-300 border-2' : ''
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
                  {item.status === 'voting' && item.voteId && (
                    <div className="flex gap-2">
                      {(() => {
                        const attendingVariant: 'default' | 'outline' = item.myResponse === 'attending' ? 'default' : 'outline';
                        return (
                          <Button
                            size="sm"
                            variant={attendingVariant}
                            className={`flex-1 h-10 ${item.myResponse === 'attending'
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
                            className={`flex-1 h-10 ${item.myResponse === 'not_attending'
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

                  {/* 입금 상태 알림 (참석한 경우, 취소된 일정 제외, 참가비가 0보다 클 때만) */}
                  {item.myResponse === 'attending' && item.entryFee != null && item.entryFee > 0 && item.status !== 'cancelled' ? (
                    <>
                      {item.myFeeStatus === 'PAID' ? (
                        <div className="p-2 bg-green-50 rounded-lg border border-green-200">
                          <p className="text-xs font-medium text-green-700">
                            ✓ 납부 완료
                          </p>
                        </div>
                      ) : (
                        <div className="p-2 bg-orange-50 rounded-lg border border-orange-200">
                          <p className="text-xs font-medium text-orange-700">
                            입금 필요: {item.entryFee.toLocaleString()}원
                          </p>
                          {bankAccount ? (
                            <div className="mt-2 space-y-1">
                              <div className="flex items-center justify-between text-xs">
                                <span className="text-orange-600">입금 계좌:</span>
                                <span className="font-medium text-orange-700">{getBankName(bankAccount.bankCode)}</span>
                              </div>
                              <div className="flex items-center justify-between text-xs">
                                <span className="text-orange-600">계좌번호:</span>
                                <div className="flex items-center gap-1">
                                  <span className="font-mono font-medium text-orange-700">{bankAccount.accountNumber}</span>
                                  <Button
                                    size="sm"
                                    variant="ghost"
                                    onClick={() => {
                                      if ("accountNumber" in bankAccount) {
                                        navigator.clipboard.writeText(bankAccount.accountNumber.replace(/-/g, ''));
                                      }
                                      setCopiedAccount(true);
                                      toast.success('계좌번호가 복사되었습니다');
                                      setTimeout(() => setCopiedAccount(false), 2000);
                                    }}
                                    className="h-4 px-1"
                                  >
                                    {(() => {
                                      if (copiedAccount) {
                                        return <Check className="w-3 h-3 text-green-600" />;
                                      }
                                      return <Copy className="w-3 h-3 text-orange-600" />;
                                    })()}
                                  </Button>
                                </div>
                              </div>
                              <div className="flex items-center justify-between text-xs">
                                <span className="text-orange-600">예금주:</span>
                                <span className="font-medium text-orange-700">{bankAccount.depositorName}</span>
                              </div>
                              <p className="text-xs text-orange-500 mt-1">
                                💡 이체 시 입금자명을 꼭 본인 이름으로 남겨주세요
                              </p>
                            </div>
                          ) : (
                            <p className="text-xs text-orange-600 mt-1">
                              💡 상세보기에서 입금 계좌 확인
                            </p>
                          )}
                        </div>
                      )}
                    </>
                  ) : null}

                  <div className="flex justify-between items-center">
                    <div className="flex gap-2">
                      {/* 일정 마무리 버튼 - 참가비가 있으면 총무 이상, 없으면 운영진 이상, 취소된 일정 제외, 일정 시작 후 */}
                      {canShowFinalizeButton(item.entryFee) && item.isEventStarted && item.status !== 'voting' && item.status !== 'cancelled' && (
                        <Link to={`${item.id}`} onClick={(e) => e.stopPropagation()}>
                          <Button
                            size="sm"
                            variant="outline"
                            className="border-purple-300 text-purple-600 hover:bg-purple-50"
                          >
                            <ClipboardCheck className="w-4 h-4 mr-1" />
                            {item.isFinalized ? '정산 확인' : (item.isPast ? '일정 마무리' : '일정 조기 종료')}
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
          <p className="text-sm">
            {searchQuery.trim() ? `"${searchQuery}" 검색 결과가 없습니다` : '아직 일정이 없습니다'}
          </p>
        </div>
      )}

      {/* FAB - 일정 만들기 */}
      <div className="fixed bottom-0 left-1/2 -translate-x-1/2 z-40 w-full max-w-md md:max-w-2xl lg:max-w-4xl px-4 pb-4 pointer-events-none">
        <div className="flex justify-between items-end pointer-events-auto">
          <QAChatWidget groupId={groupId ? Number(groupId) : undefined} />
          <Link to="create-vote">
            <Button className="rounded-full h-14 w-14 shadow-lg bg-lime-300 hover:bg-lime-400 text-white p-0">
              <Plus className="w-6 h-6" />
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
