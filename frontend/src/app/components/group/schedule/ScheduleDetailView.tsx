import { useState, useEffect, useCallback, useMemo } from 'react';
import * as React from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Calendar, Clock, MapPin, MessageCircle, Share2, Edit3, Check, X, AlertCircle, AlertTriangle, Copy } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Avatar, AvatarFallback } from '../../ui/avatar';
import { Badge } from '../../ui/badge';
import { Textarea } from '../../ui/textarea';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '../../ui/alert-dialog';
import { getSchedule, getScheduleParticipants, updateSchedule, closeSchedule, cancelSchedule, finalizeSchedule, updateParticipantFeeStatus, updateParticipantRefundStatus, updateParticipantAttendance, type ScheduleResponse, type ScheduleParticipantResponse, type ScheduleUpdateRequest, type ScheduleCancelRequest } from '../../../../api/schedule';
import { getMyInfo } from '../../../../api/user';
import { getVotes, getVote, answerVote, type VoteDetailResponse, type VoteAnswerRequest, type VoteListResponse } from '../../../../api/vote';
import { getRecentPosts } from '../../../../api/post';
import { getPostComments, createComment, deleteComment, type PostCommentItem } from '../../../../api/comment';
import { getMembers, type MemberListResponse } from '../../../../api/member';
import { getBankAccount, type BankAccounts } from '../../../../api/bank';
import { useUserPermissions } from '../../../data/userRoles';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../ui/dialog';

export function ScheduleDetailView() {
  const navigate = useNavigate();
  const { groupId, scheduleId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [schedule, setSchedule] = useState<ScheduleResponse | null>(null);
  const [participants, setParticipants] = useState<ScheduleParticipantResponse[]>([]);
  const [vote, setVote] = useState<VoteDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [myResponse, setMyResponse] = useState<'attending' | 'not_attending' | null>(null);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [comment, setComment] = useState('');
  const [comments, setComments] = useState<PostCommentItem[]>([]);
  const [linkedPostId, setLinkedPostId] = useState<number | null>(null);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [loadingComments, setLoadingComments] = useState(false);
  const [members, setMembers] = useState<MemberListResponse[]>([]);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editStartDate, setEditStartDate] = useState('');
  const [editEndDate, setEditEndDate] = useState('');
  const [editLocation, setEditLocation] = useState('');
  const [editEntryFee, setEditEntryFee] = useState('');
  const [editVoteDeadline, setEditVoteDeadline] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);
  const [showCancelDialog, setShowCancelDialog] = useState(false);
  const [showCloseDialog, setShowCloseDialog] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [isCancelling, setIsCancelling] = useState(false);
  const [isClosing, setIsClosing] = useState(false);
  const [showFinalizeDialog, setShowFinalizeDialog] = useState(false);
  const [isFinalizing, setIsFinalizing] = useState(false);
  const [finalizeTotalSpent, setFinalizeTotalSpent] = useState(0);
  const [finalizeRefundPerPerson, setFinalizeRefundPerPerson] = useState(0);
  const [finalizeParticipantIds, setFinalizeParticipantIds] = useState<Set<number>>(new Set());
  const [bankAccount, setBankAccount] = useState<BankAccounts | null>(null);
  const [copied, setCopied] = useState(false);
  const [, setLoadingBankAccount] = useState(false);

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

  // 일정과 연결된 게시글 찾기 및 댓글 조회
  const fetchLinkedPostComments = useCallback(async () => {
    if (!groupId || !scheduleId) return;
    try {
      // 일정과 연결된 게시글 찾기
      const posts = await getRecentPosts(Number(groupId), 0, 100);
      const linkedPost = posts.find(p => p.scheduleId === Number(scheduleId));

      if (linkedPost) {
        setLinkedPostId(linkedPost.postId);
        // 댓글 조회
        setLoadingComments(true);
        const response = await getPostComments(Number(groupId), linkedPost.postId, 0, 20);
        setComments(response.comments);
      } else {
        setLinkedPostId(null);
        setComments([]);
      }
    } catch (error) {
      console.error('연결된 게시글 또는 댓글 조회 실패:', error);
      setLinkedPostId(null);
      setComments([]);
    } finally {
      setLoadingComments(false);
    }
  }, [groupId, scheduleId]);

  useEffect(() => {
    async function fetchData() {
      if (!groupId || !scheduleId) return;
      try {
        setLoading(true);

        // 핵심 데이터를 병렬로 가져오기 (투표 정보 포함)
        const schedulePromise = getSchedule(Number(groupId), Number(scheduleId));
        const participantsPromise = getScheduleParticipants(Number(groupId), Number(scheduleId));
        const votesPromise = getVotes(Number(groupId)).catch(() => [] as VoteListResponse[]);
        const membersPromise = getMembers(Number(groupId), 'ACTIVE').catch(() => [] as MemberListResponse[]);
        const accountPromise = getBankAccount(Number(groupId)).catch((error: any) => {
          // 에러 발생 시 null 반환 (나중에 별도로 재시도)
          const errorMessage = error?.message || String(error) || '';
          const status = error?.status || error?.response?.status;
          const isNotFound = status === 404 ||
            status === 400 ||
            errorMessage.toLowerCase().includes('404') ||
            errorMessage.toLowerCase().includes('400') ||
            errorMessage.toLowerCase().includes('not found') ||
            errorMessage.toLowerCase().includes('존재하지') ||
            errorMessage.includes('찾을 수 없습니다') ||
            errorMessage.includes('계좌를 찾을 수 없습니다');
          if (!isNotFound) {
            console.warn('계좌 정보 조회 실패 (재시도 예정):', { status, message: errorMessage, error });
          }
          return null as BankAccounts | null;
        });

        const [scheduleData, participantsData, votesData, membersData, accountData] = await Promise.all([
          schedulePromise,
          participantsPromise,
          votesPromise,
          membersPromise,
          accountPromise
        ] as any) as [
            ScheduleResponse,
            ScheduleParticipantResponse[],
            VoteListResponse[],
            MemberListResponse[],
            BankAccounts | null
          ];

        setSchedule(scheduleData);
        
        // 모임의 모든 ACTIVE 멤버를 포함하도록 participants 확장
        // 투표를 안 한 멤버들은 UNDECIDED 상태로 추가
        const participantUserIds = new Set(participantsData.map(p => p.userId));
        const allParticipants: ScheduleParticipantResponse[] = [
          ...participantsData,
          ...membersData
            .filter(member => !participantUserIds.has(member.userId))
            .map(member => ({
              participantId: 0, // 아직 participant가 생성되지 않음
              scheduleId: scheduleData.scheduleId,
              userId: member.userId,
              userName: member.realName || 'Unknown',
              attendanceStatus: 'UNDECIDED' as const,
              feeStatus: 'PENDING' as const,
              isRefunded: false,
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString()
            }))
        ];
        
        setParticipants(allParticipants);
        setMembers(membersData);
        setBankAccount(accountData);

        // votes를 명시적으로 VoteListResponse[] 타입으로 지정
        const votes: VoteListResponse[] = Array.isArray(votesData) ? votesData : [];

        // 계좌 정보가 없으면 재시도 (계좌가 방금 생성되었을 수 있음)
        if (!accountData && scheduleData.entryFee && scheduleData.entryFee > 0) {
          // 즉시 재시도
          (async () => {
            try {
              setLoadingBankAccount(true);
              const retryAccount = await getBankAccount(Number(groupId));
              setBankAccount(retryAccount);
            } catch (retryError: any) {
              // 재시도 실패 시 조용히 처리 (에러 로그만)
            } finally {
              setLoadingBankAccount(false);
            }
          })();
        }

        // 일정의 ATTENDANCE 투표 조회 (scheduleId로 바로 필터링 가능)
        const attendanceVote = votes.find(v =>
          v.voteType === 'ATTENDANCE' && v.scheduleId === Number(scheduleId)
        );

        // 투표 상세 정보를 병렬로 가져오기 (로딩 완료 전에 가져와야 투표 창이 바로 표시됨)
        if (attendanceVote) {
          try {
            const voteDetail = await getVote(Number(groupId), attendanceVote.voteId);
            setVote(voteDetail);
          } catch (error) {
            console.error('투표 상세 조회 실패:', error);
          }
        }

        // 일정과 연결된 게시글 및 댓글 조회 (비동기로 처리하여 블로킹 방지)
        fetchLinkedPostComments();
      } catch (error) {
        console.error('일정 상세 불러오기 실패:', error);
        toast.error('일정 정보를 불러오는데 실패했습니다.');
        navigate(-1);
      } finally {
        setLoading(false);
      }
    }
    fetchData();

    // 실시간 업데이트는 제거 - 사용자 액션 후 명시적 새로고침만 수행
    // 이유: 불필요한 API 호출 최소화, 명확한 데이터 흐름
  }, [groupId, scheduleId, navigate, fetchLinkedPostComments]);

  // currentUserId가 설정되면 투표 상태 업데이트
  useEffect(() => {
    if (!currentUserId || !vote || !participants.length) return;

    // 내 투표 확인 (optionText가 "참석" 또는 "불참"인 옵션 찾기)
    const mySelectedOptions = vote.options.filter(opt =>
      opt.voters?.some(v => v.userId === currentUserId)
    );

    if (mySelectedOptions.length > 0) {
      const selectedOption = mySelectedOptions[0];
      if (selectedOption.optionText === '참석' || selectedOption.optionText.includes('참석')) {
        setMyResponse('attending');
      } else if (selectedOption.optionText === '불참' || selectedOption.optionText.includes('불참')) {
        setMyResponse('not_attending');
      }
    } else {
      // participants에서도 확인
      const myParticipant = participants.find(p => p.userId === currentUserId);
      if (myParticipant) {
        if (myParticipant.attendanceStatus === 'ATTENDING') {
          setMyResponse('attending');
        } else if (myParticipant.attendanceStatus === 'NOT_ATTENDING') {
          setMyResponse('not_attending');
        }
      }
    }
  }, [currentUserId, vote, participants]);

  // 참석자 수 즉시 반영을 위해 useMemo 사용 (early return 전에 호출해야 함 - Hook 규칙)
  const { attendingCount, notAttendingCount, pendingCount, paidCount } = useMemo(() => {
    const attending = participants.filter(p => p.attendanceStatus === 'ATTENDING').length;
    const notAttending = participants.filter(p => p.attendanceStatus === 'NOT_ATTENDING').length;
    const pending = participants.filter(p => p.attendanceStatus === 'PENDING' || p.attendanceStatus === 'UNDECIDED').length;
    const paid = participants.filter(p => p.feeStatus === 'PAID').length;
    return { attendingCount: attending, notAttendingCount: notAttending, pendingCount: pending, paidCount: paid };
  }, [participants]);

  // 총 지출·환급 대상 변경 시 1인당 환급 재계산 (참석 N빵 기본) — early return 전에 훅 호출 필수
  useEffect(() => {
    if (!schedule || !showFinalizeDialog) return;
    const collected = schedule.collectedEntryFee ?? 0;
    const n = finalizeParticipantIds.size;
    const refund = n > 0 && collected >= finalizeTotalSpent
      ? Math.floor((collected - finalizeTotalSpent) / n)
      : 0;
    setFinalizeRefundPerPerson(Math.max(0, refund));
  }, [schedule, showFinalizeDialog, finalizeTotalSpent, finalizeParticipantIds]);

  if (loading || !schedule) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  const eventDate = new Date(schedule.eventDate);
  const endDate = new Date(schedule.endDate);
  const now = new Date();
  const isEventStarted = eventDate <= now; // 일정이 시작되었는지 확인
  const isEventEnded = endDate < now; // 일정이 종료되었는지 확인

  const handleResponse = async (response: 'attending' | 'not_attending') => {
    if (!groupId || !scheduleId || !vote) return;

    try {
      // 투표 옵션에서 참석/불참 옵션 찾기
      const attendingOption = vote.options.find(opt =>
        opt.optionText === '참석' || opt.optionText.includes('참석')
      );
      const notAttendingOption = vote.options.find(opt =>
        opt.optionText === '불참' || opt.optionText.includes('불참')
      );

      // 토글 기능: 같은 버튼을 다시 누르면 취소(미정으로)
      const isToggling = (response === 'attending' && myResponse === 'attending') ||
        (response === 'not_attending' && myResponse === 'not_attending');

      if (isToggling) {
        // 이미 선택된 옵션을 다시 클릭하면 취소 (미정으로 변경)
        if (!currentUserId) {
          toast.error('사용자 정보를 찾을 수 없습니다');
          return;
        }

        // 현재 사용자의 participant 찾기
        const myParticipant = participants.find(p => p.userId === currentUserId);
        if (!myParticipant) {
          toast.error('참가자 정보를 찾을 수 없습니다');
          return;
        }

        // 직접 참가자 상태를 UNDECIDED로 변경
        await updateParticipantAttendance(Number(groupId), Number(scheduleId), myParticipant.participantId, {
          attendanceStatus: 'UNDECIDED'
        });

        // 약간의 딜레이 후 서버 데이터로 동기화
        await new Promise(resolve => setTimeout(resolve, 300));

        const participantsData = await getScheduleParticipants(Number(groupId), Number(scheduleId));
        const updatedVote = await getVote(Number(groupId), vote.voteId);

        // 참가자 목록 새로고침 (멤버 전체를 다시 합쳐야 함)
        const participantUserIds = new Set(participantsData.map(p => p.userId));
        const allParticipants: ScheduleParticipantResponse[] = [
          ...participantsData,
          ...members
            .filter(member => !participantUserIds.has(member.userId))
            .map(member => ({
              participantId: 0,
              scheduleId: Number(scheduleId),
              userId: member.userId,
              userName: member.realName || 'Unknown',
              attendanceStatus: 'UNDECIDED' as const,
              feeStatus: 'PENDING' as const,
              isRefunded: false,
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString()
            }))
        ];
        setParticipants(allParticipants);
        setVote(updatedVote);
        setMyResponse(null);

        toast.success('투표가 취소되었습니다');
      } else {
        // 새로운 선택
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

        await answerVote(Number(groupId), vote.voteId, request);

        // 약간의 딜레이 후 서버 데이터로 동기화 (백엔드에서 ScheduleParticipants 생성/업데이트 시간 확보)
        await new Promise(resolve => setTimeout(resolve, 300));

        const participantsData = await getScheduleParticipants(Number(groupId), Number(scheduleId));
        const updatedVote = await getVote(Number(groupId), vote.voteId);

        // 참가자 목록 새로고침 (멤버 전체를 다시 합쳐야 함)
        const participantUserIds = new Set(participantsData.map(p => p.userId));
        const allParticipants: ScheduleParticipantResponse[] = [
          ...participantsData,
          ...members
            .filter(member => !participantUserIds.has(member.userId))
            .map(member => ({
              participantId: 0,
              scheduleId: Number(scheduleId),
              userId: member.userId,
              userName: member.realName || 'Unknown',
              attendanceStatus: 'UNDECIDED' as const,
              feeStatus: 'PENDING' as const,
              isRefunded: false,
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString()
            }))
        ];

        // 상태 업데이트
        setParticipants(allParticipants);
        setVote(updatedVote);

        // 서버 응답을 기반으로 myResponse 상태 업데이트
        if (currentUserId) {
          // 먼저 participants에서 확인
          const myParticipant = participantsData.find(p => p.userId === currentUserId);
          if (myParticipant) {
            if (myParticipant.attendanceStatus === 'ATTENDING') {
              setMyResponse('attending');
            } else if (myParticipant.attendanceStatus === 'NOT_ATTENDING') {
              setMyResponse('not_attending');
            } else {
              setMyResponse(null);
            }
          } else if (updatedVote.mySelectedOptionIds && updatedVote.mySelectedOptionIds.length > 0) {
            // participants에 없으면 투표 데이터로 확인
            const selectedOption = updatedVote.options.find(opt =>
              updatedVote.mySelectedOptionIds?.includes(opt.optionId)
            );
            if (selectedOption) {
              if (selectedOption.optionText === '참석' || selectedOption.optionText.includes('참석')) {
                setMyResponse('attending');
              } else if (selectedOption.optionText === '불참' || selectedOption.optionText.includes('불참')) {
                setMyResponse('not_attending');
              } else {
                setMyResponse(null);
              }
            } else {
              setMyResponse(null);
            }
          } else {
            setMyResponse(null);
          }
        }

        toast.success(response === 'attending' ? '참석으로 응답했습니다' : '불참으로 응답했습니다');
      }
    } catch (error) {
      console.error('참석 응답 실패:', error);
      toast.error('참석 응답에 실패했습니다.');
    }
  };

  const handleDelete = () => {
    toast.success('일정이 삭제되었습니다');
    navigate(-1);
  };

  const handleAddComment = async () => {
    if (!comment.trim() || !groupId || !linkedPostId) {
      if (!linkedPostId) {
        toast.error('일정과 연결된 게시글이 없어 댓글을 작성할 수 없습니다.');
      }
      return;
    }
    try {
      await createComment(Number(groupId), linkedPostId, {
        content: comment.trim()
      });
      // 댓글 목록 새로고침
      await fetchLinkedPostComments();
      setComment('');
      toast.success('댓글이 등록되었습니다');
    } catch (error) {
      console.error('댓글 작성 실패:', error);
      toast.error('댓글 작성에 실패했습니다.');
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    if (!groupId || !linkedPostId) return;
    try {
      await deleteComment(Number(groupId), linkedPostId, commentId);
      // 댓글 목록 새로고침
      await fetchLinkedPostComments();
      toast.success('댓글이 삭제되었습니다');
    } catch (error) {
      console.error('댓글 삭제 실패:', error);
      toast.error('댓글 삭제에 실패했습니다.');
    }
  };

  const handleShare = () => {
    navigator.clipboard.writeText(window.location.href);
    toast.success('링크가 복사되었습니다');
  };


  // 일정 마무리 다이얼로그 열기
  const handleOpenFinalizeDialog = () => {
    if (!schedule || !participants.length) return;
    const paid = participants.filter(p => p.feeStatus === 'PAID');
    const ids = new Set(paid.map(p => p.participantId));
    setFinalizeParticipantIds(ids);
    setFinalizeTotalSpent(schedule.totalSpent ?? 0);
    const collected = schedule.collectedEntryFee ?? 0;
    const n = ids.size;
    const refund = n > 0 && collected > 0 ? Math.floor((collected - (schedule.totalSpent ?? 0)) / n) : 0;
    setFinalizeRefundPerPerson(Math.max(0, refund));
    setShowFinalizeDialog(true);
  };

  // 환급 대상 참가자 토글
  const toggleFinalizeParticipant = (participantId: number) => {
    setFinalizeParticipantIds(prev => {
      const next = new Set(prev);
      if (next.has(participantId)) next.delete(participantId);
      else next.add(participantId);
      return next;
    });
  };

  // 일정 마무리 처리 (정산 + 환급 + 마감)
  const handleFinalize = async () => {
    if (!groupId || !scheduleId || !schedule) return;

    try {
      setIsFinalizing(true);
      await finalizeSchedule(Number(groupId), Number(scheduleId), {
        totalSpent: finalizeTotalSpent,
      });
      toast.success('일정 마무리가 완료되었습니다. 환급이 처리됩니다.');
      setShowFinalizeDialog(false);
      const scheduleData = await getSchedule(Number(groupId), Number(scheduleId));
      const participantsData = await getScheduleParticipants(Number(groupId), Number(scheduleId));
      setSchedule(scheduleData);
      // 참가자 목록 새로고침 (멤버 전체를 다시 합쳐야 함)
      const participantUserIds = new Set(participantsData.map(p => p.userId));
      const allParticipants: ScheduleParticipantResponse[] = [
        ...participantsData,
        ...members
          .filter(member => !participantUserIds.has(member.userId))
          .map(member => ({
            participantId: 0,
            scheduleId: scheduleData.scheduleId,
            userId: member.userId,
            userName: member.realName || 'Unknown',
            attendanceStatus: 'UNDECIDED' as const,
            feeStatus: 'PENDING' as const,
            isRefunded: false,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          }))
      ];
      setParticipants(allParticipants);
    } catch (error) {
      console.error('일정 마무리 실패:', error);
      toast.error('일정 마무리에 실패했습니다.');
    } finally {
      setIsFinalizing(false);
    }
  };

  // 참가자 납부 상태 변경
  const handleUpdateFeeStatus = async (participantId: number, newStatus: 'PENDING' | 'PAID') => {
    if (!groupId || !scheduleId) return;

    try {
      await updateParticipantFeeStatus(Number(groupId), Number(scheduleId), participantId, { feeStatus: newStatus });
      toast.success(newStatus === 'PAID' ? '납부 확인되었습니다.' : '납부 취소되었습니다.');
      // 참가자 목록 새로고침 (멤버 전체를 다시 합쳐야 함)
      const participantsData = await getScheduleParticipants(Number(groupId), Number(scheduleId));
      const participantUserIds = new Set(participantsData.map(p => p.userId));
      const allParticipants: ScheduleParticipantResponse[] = [
        ...participantsData,
        ...members
          .filter(member => !participantUserIds.has(member.userId))
          .map(member => ({
            participantId: 0,
            scheduleId: Number(scheduleId),
            userId: member.userId,
            userName: member.realName || 'Unknown',
            attendanceStatus: 'UNDECIDED' as const,
            feeStatus: 'PENDING' as const,
            isRefunded: false,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          }))
      ];
      setParticipants(allParticipants);
      // 일정 정보도 새로고침 (집계 금액 업데이트)
      const scheduleData = await getSchedule(Number(groupId), Number(scheduleId));
      setSchedule(scheduleData);

      // 목록 페이지에 알림을 위한 커스텀 이벤트 발생
      window.dispatchEvent(new CustomEvent('scheduleFeeStatusUpdated', {
        detail: { scheduleId: Number(scheduleId), groupId: Number(groupId) }
      }));
    } catch (error) {
      console.error('납부 상태 변경 실패:', error);
      toast.error('납부 상태 변경에 실패했습니다.');
    }
  };

  // 참가자 환급 상태 변경
  const handleUpdateRefundStatus = async (participantId: number, isRefunded: boolean) => {
    if (!groupId || !scheduleId) return;

    try {
      await updateParticipantRefundStatus(Number(groupId), Number(scheduleId), participantId, { isRefunded });
      toast.success(isRefunded ? '환급 완료 처리되었습니다.' : '환급 상태가 초기화되었습니다.');
      // 참가자 목록 새로고침 (멤버 전체를 다시 합쳐야 함)
      const participantsData = await getScheduleParticipants(Number(groupId), Number(scheduleId));
      const participantUserIds = new Set(participantsData.map(p => p.userId));
      const allParticipants: ScheduleParticipantResponse[] = [
        ...participantsData,
        ...members
          .filter(member => !participantUserIds.has(member.userId))
          .map(member => ({
            participantId: 0,
            scheduleId: Number(scheduleId),
            userId: member.userId,
            userName: member.realName || 'Unknown',
            attendanceStatus: 'UNDECIDED' as const,
            feeStatus: 'PENDING' as const,
            isRefunded: false,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          }))
      ];
      setParticipants(allParticipants);
    } catch (error) {
      console.error('환급 상태 변경 실패:', error);
      toast.error('환급 상태 변경에 실패했습니다.');
    }
  };

  // 참가자 참석 상태 변경 (총무 이상)
  const handleUpdateAttendanceStatus = async (participantId: number, newStatus: 'ATTENDING' | 'NOT_ATTENDING' | 'UNDECIDED') => {
    if (!groupId || !scheduleId) return;

    try {
      await updateParticipantAttendance(Number(groupId), Number(scheduleId), participantId, { attendanceStatus: newStatus });
      const statusLabel = newStatus === 'ATTENDING' ? '참석' : newStatus === 'NOT_ATTENDING' ? '불참' : '미정';
      toast.success(`참석 상태가 '${statusLabel}'으로 변경되었습니다.`);
      // 참가자 목록 새로고침 (멤버 전체를 다시 합쳐야 함)
      const participantsData = await getScheduleParticipants(Number(groupId), Number(scheduleId));
      const participantUserIds = new Set(participantsData.map(p => p.userId));
      const allParticipants: ScheduleParticipantResponse[] = [
        ...participantsData,
        ...members
          .filter(member => !participantUserIds.has(member.userId))
          .map(member => ({
            participantId: 0,
            scheduleId: Number(scheduleId),
            userId: member.userId,
            userName: member.realName || 'Unknown',
            attendanceStatus: 'UNDECIDED' as const,
            feeStatus: 'PENDING' as const,
            isRefunded: false,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          }))
      ];
      setParticipants(allParticipants);
    } catch (error) {
      console.error('참석 상태 변경 실패:', error);
      toast.error('참석 상태 변경에 실패했습니다.');
    }
  };

  const handleStartEdit = () => {
    if (!schedule) return;
    setEditTitle(schedule.scheduleName);
    setEditDescription(schedule.description || '');
    // 날짜 형식 변환: ISO string -> datetime-local 형식
    const startDate = new Date(schedule.eventDate);
    const endDate = new Date(schedule.endDate);
    setEditStartDate(startDate.toISOString().slice(0, 16));
    setEditEndDate(endDate.toISOString().slice(0, 16));
    setEditLocation(schedule.location || '');
    setEditEntryFee(schedule.entryFee ? schedule.entryFee.toString() : '');
    // 투표 마감일 초기화
    if (schedule.voteDeadline) {
      const voteDeadlineDate = new Date(schedule.voteDeadline);
      setEditVoteDeadline(voteDeadlineDate.toISOString().slice(0, 16));
    } else {
      setEditVoteDeadline('');
    }
    setShowEditDialog(true);
  };

  const handleUpdateSchedule = async () => {
    if (!groupId || !scheduleId || !schedule) return;

    // 유효성 검사
    if (!editTitle.trim()) {
      toast.error('일정 제목을 입력해주세요');
      return;
    }
    if (!editStartDate) {
      toast.error('시작 일시를 선택해주세요');
      return;
    }
    if (!editEndDate) {
      toast.error('종료 일시를 선택해주세요');
      return;
    }

    // 날짜 논리 검사
    const start = new Date(editStartDate);
    const end = new Date(editEndDate);

    if (end <= start) {
      toast.error('종료 일시는 시작 일시보다 뒤여야 합니다');
      return;
    }

    // 참가비 변경 여부 확인 및 권한 체크
    const currentEntryFee = schedule.entryFee || 0;
    const newEntryFee = editEntryFee.trim() ? parseFloat(editEntryFee) : undefined;
    const isEntryFeeChanged = currentEntryFee !== (newEntryFee ?? 0);

    if (isEntryFeeChanged && newEntryFee !== undefined && newEntryFee !== null && newEntryFee > 0 && !permissions.canWithdraw) {
      toast.error('참가비 변경은 총무 이상만 가능합니다');
      return;
    }

    // 이미 종료되거나 취소된 일정은 수정 불가
    if (schedule.status === 'CLOSED' || schedule.status === 'CANCELLED') {
      toast.error('종료되거나 취소된 일정은 수정할 수 없습니다');
      return;
    }

    try {
      setIsUpdating(true);
      const request: ScheduleUpdateRequest = {
        scheduleName: editTitle.trim(),
        eventDate: editStartDate,
        endDate: editEndDate,
        location: editLocation.trim() || undefined,
        description: editDescription.trim() || undefined,
        entryFee: newEntryFee, // 백엔드에서 BigDecimal이고 nullable이 아니므로 0을 보냄
        voteDeadline: editVoteDeadline || undefined,
      };
      await updateSchedule(Number(groupId), Number(scheduleId), request);
      toast.success('일정이 수정되었습니다');
      setShowEditDialog(false);
      // 일정 정보 새로고침
      const scheduleData = await getSchedule(Number(groupId), Number(scheduleId));
      setSchedule(scheduleData);
    } catch (error) {
      console.error('일정 수정 실패:', error);
      toast.error('일정 수정에 실패했습니다.');
    } finally {
      setIsUpdating(false);
    }
  };

  const handleCancelSchedule = async () => {
    if (!groupId || !scheduleId) return;

    try {
      setIsCancelling(true);
      const request: ScheduleCancelRequest | undefined = cancelReason.trim()
        ? { cancelReason: cancelReason.trim() }
        : undefined;
      await cancelSchedule(Number(groupId), Number(scheduleId), request);
      toast.success('일정이 취소되었습니다');
      setShowCancelDialog(false);
      setCancelReason('');
      // 일정 정보 새로고침
      const scheduleData = await getSchedule(Number(groupId), Number(scheduleId));
      setSchedule(scheduleData);
    } catch (error) {
      console.error('일정 취소 실패:', error);
      toast.error('일정 취소에 실패했습니다.');
    } finally {
      setIsCancelling(false);
    }
  };

  const handleCloseSchedule = async () => {
    if (!groupId || !scheduleId) return;

    try {
      setIsClosing(true);
      await closeSchedule(Number(groupId), Number(scheduleId));
      
      // 참가비 유무와 관계없이 투표만 마감되고 일정은 OPEN 유지
      // 일정 마감은 "일정 마무리" 기능에서만 수행
      toast.success('참석 투표가 마감되었습니다');
      
      setShowCloseDialog(false);
      
      // 일정 정보 새로고침
      const scheduleData = await getSchedule(Number(groupId), Number(scheduleId));
      setSchedule(scheduleData);
      
      // 투표 정보도 새로고침
      if (vote) {
        try {
          const voteDetail = await getVote(Number(groupId), vote.voteId);
          setVote(voteDetail);
        } catch (error) {
          console.error('투표 정보 새로고침 실패:', error);
        }
      }
    } catch (error) {
      console.error('일정 마감 실패:', error);
      toast.error('일정 마감에 실패했습니다.');
    } finally {
      setIsClosing(false);
    }
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-24" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigate(-1)}
              className="-ml-2"
            >
              <ArrowLeft className="w-6 h-6 text-stone-800" />
            </Button>
            <h1 className="ml-2 text-lg font-semibold text-stone-800">일정 상세</h1>
          </div>
          <div className="flex items-center gap-1">
            <Button variant="ghost" size="icon" onClick={handleShare}>
              <Share2 className="w-5 h-5 text-stone-600" />
            </Button>
            {/* 수정 버튼: 운영진 이상 또는 참가비 변경 시 총무 이상 */}
            {schedule && (schedule.status === 'OPEN' || schedule.status === 'PENDING') && (
              (permissions.canManageGroup || (schedule.entryFee && schedule.entryFee > 0 ? permissions.canWithdraw : true)) && (
                <Button variant="ghost" size="icon" onClick={handleStartEdit}>
                  <Edit3 className="w-5 h-5 text-stone-600" />
                </Button>
              )
            )}
          </div>
        </div>
      </header>

      <div className="p-5 space-y-5">
        {/* Title & Badge */}
        <div>
          <Badge className="bg-orange-100 text-orange-700 mb-2">일정</Badge>
          <h2 className="text-2xl font-bold text-stone-900">{schedule.scheduleName}</h2>
        </div>

        {/* Date & Time & Location */}
        <div className="bg-white rounded-2xl p-4 space-y-4 border border-stone-100">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center">
              <Calendar className="w-5 h-5 text-orange-600" />
            </div>
            <div>
              <p className="font-medium text-stone-900">
                {eventDate.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' })}
              </p>
              <p className="text-sm text-stone-500">
                {eventDate.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })} - {endDate.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
              </p>
            </div>
          </div>

          {schedule.location && (
            <div className="flex items-start gap-3">
              <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                <MapPin className="w-5 h-5 text-blue-600" />
              </div>
              <div>
                <p className="font-medium text-stone-900">{schedule.location}</p>
                <button className="text-xs text-orange-600 mt-1">지도 보기</button>
              </div>
            </div>
          )}
        </div>

        {/* Description */}
        {schedule.description && (
          <div className="bg-white rounded-2xl p-4 border border-stone-100">
            <h3 className="font-bold text-stone-900 mb-2">상세 내용</h3>
            <p className="text-sm text-stone-600 leading-relaxed">{schedule.description}</p>
          </div>
        )}

        {/* 참가비 정보 및 집계 */}
        {(schedule.entryFee ?? 0) > 0 && (
          <div className="bg-white rounded-2xl p-4 border border-stone-100 space-y-3">
            <h3 className="font-bold text-stone-900">참가비</h3>
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-stone-600">참가비 금액</span>
                <p className="text-lg font-semibold text-orange-600">
                  {(schedule.entryFee || 0).toLocaleString()}원
                </p>
              </div>
              <div className="flex items-center justify-between pt-2 border-t border-stone-100">
                <span className="text-stone-600">집계된 참가비</span>
                <span className="font-medium text-stone-900">
                  {(schedule.collectedEntryFee || 0).toLocaleString()}원
                </span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-stone-500">납부 인원</span>
                <span className="text-stone-600">
                  {schedule.paidParticipantsCount || 0}명
                </span>
              </div>
            </div>
            {/* 모임 통장 계좌 정보 - 참가비가 있을 때 항상 표시 */}
            <div className="pt-3 border-t border-stone-100 space-y-2">
              <h4 className="text-sm font-semibold text-stone-700">입금 계좌</h4>
              {bankAccount ? (
                <>
                  <div className="bg-stone-50 rounded-lg p-3 space-y-2">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-stone-500">은행</span>
                      <span className="font-medium text-stone-900">{getBankName(bankAccount.bankCode)}</span>
                    </div>
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-stone-500">계좌번호</span>
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-stone-900 font-mono">{bankAccount.accountNumber || '계좌번호 없음'}</span>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => {
                            const accountNumber = bankAccount?.accountNumber;
                            if (!accountNumber) return;
                            navigator.clipboard.writeText(accountNumber.replace(/-/g, ''));
                            setCopied(true);
                            toast.success('계좌번호가 복사되었습니다');
                            setTimeout(() => setCopied(false), 2000);
                          }}
                          className="h-6 px-2"
                        >
                          {(() => {
                            if (copied) {
                              return <Check className="w-3 h-3 text-green-600" /> as React.ReactNode;
                            }
                            return <Copy className="w-3 h-3" /> as React.ReactNode;
                          })()}
                        </Button>
                      </div>
                    </div>
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-stone-500">예금주</span>
                      <span className="font-medium text-stone-900">{bankAccount.depositorName}</span>
                    </div>
                  </div>
                  <p className="text-xs text-stone-500">
                    💡 이체 시 입금자명을 꼭 본인 이름으로 남겨주세요.
                  </p>
                </>
              ) : (
                <div className="bg-stone-50 rounded-lg p-3">
                  <p className="text-sm text-stone-500 text-center">
                    모임 계좌 정보를 불러올 수 없습니다.
                  </p>
                  <p className="text-xs text-stone-400 text-center mt-1">
                    관리 페이지에서 계좌를 생성해주세요.
                  </p>
                </div>
              )}
            </div>
            {/* 정산 정보 (마감된 경우) */}
            {schedule.status === 'CLOSED' && schedule.totalSpent !== undefined && schedule.totalSpent > 0 && (
              <div className="pt-2 border-t border-stone-100">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-stone-600">총 지출</span>
                  <span className="font-medium">{schedule.totalSpent.toLocaleString()}원</span>
                </div>
                {schedule.refundPerPerson !== undefined && schedule.refundPerPerson > 0 && (
                  <div className="flex items-center justify-between text-sm mt-1">
                    <span className="text-stone-600">1인당 환급액</span>
                    <span className="font-medium text-blue-600">{schedule.refundPerPerson.toLocaleString()}원</span>
                  </div>
                )}
              </div>
            )}
            {/* 일정 마무리 버튼 (OPEN 상태이고, 참가비가 있으면 총무 이상, 없으면 운영진 이상, 일정이 시작된 후) */}
            {schedule.status === 'OPEN' && isEventStarted && (
              (schedule.entryFee && schedule.entryFee > 0
                ? permissions.canWithdraw
                : permissions.canManageGroup) && (
                <div className="pt-3 border-t border-stone-100">
                  <Button
                    onClick={handleOpenFinalizeDialog}
                    size="sm"
                    className="w-full bg-orange-500 hover:bg-orange-600 text-white"
                  >
                    {isEventEnded ? '일정 마무리 (정산 및 환급)' : '일정 조기 종료 (정산 및 환급)'}
                  </Button>
                  <p className="text-xs text-stone-500 mt-2 text-center">
                    총 지출을 입력하면 자동으로 환급액이 계산됩니다.
                  </p>
                </div>
              )
            )}
          </div>
        )}

        {/* Attendance Response - 투표 진행중이거나 총무 이상이면 표시 */}
        {vote && (vote.status === 'OPEN' || permissions.canWithdraw) && schedule.status !== 'CANCELLED' && (
          <div className="bg-white rounded-2xl p-4 border border-stone-100">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold text-stone-900">참석 여부 투표</h3>
              <Badge className={vote.status === 'OPEN' ? 'bg-orange-100 text-orange-700' : 'bg-stone-100 text-stone-600'}>
                {vote.status === 'OPEN' ? '투표 진행중' : '투표 마감됨'}
              </Badge>
            </div>
            {/* 본인 입금 상태 표시 (참석한 경우) */}
            {myResponse === 'attending' && (schedule.entryFee ?? 0) > 0 && currentUserId && (() => {
              const myParticipant = participants.find(p => p.userId === currentUserId);
              const isPaid = myParticipant?.feeStatus === 'PAID';
              return (
                <div className={`mb-3 p-2 rounded-lg ${isPaid ? 'bg-green-50 border border-green-200' : 'bg-orange-50 border border-orange-200'}`}>
                  <p className={`text-xs font-medium ${isPaid ? 'text-green-700' : 'text-orange-700'}`}>
                    {isPaid ? '✓ 입금 완료' : `입금 필요: ${(schedule.entryFee || 0).toLocaleString()}원`}
                  </p>
                </div>
              );
            })()}
            <div className={`grid grid-cols-2 gap-3 ${(vote.status !== 'OPEN' && !permissions.canWithdraw) || schedule.status === 'CLOSED' ? 'opacity-60' : ''}`}>
              {(() => {
                const attendingVariant: 'default' | 'outline' = myResponse === 'attending' ? 'default' : 'outline';
                return (
                  <Button
                    variant={attendingVariant}
                    className={`h-12 rounded-xl ${myResponse === 'attending'
                        ? 'bg-green-500 hover:bg-green-600 text-white'
                        : 'border-stone-200'
                      }`}
                    onClick={() => handleResponse('attending')}
                    disabled={(vote.status !== 'OPEN' && !permissions.canWithdraw) || schedule.status === 'CLOSED'}
                  >
                    <Check className="w-5 h-5 mr-2" />
                    참석
                  </Button>
                );
              })()}
              {(() => {
                const notAttendingVariant: 'default' | 'outline' = myResponse === 'not_attending' ? 'default' : 'outline';
                return (
                  <Button
                    variant={notAttendingVariant}
                    className={`h-12 rounded-xl ${myResponse === 'not_attending'
                        ? 'bg-red-500 hover:bg-red-600 text-white'
                        : 'border-stone-200'
                      }`}
                    onClick={() => handleResponse('not_attending')}
                    disabled={(vote.status !== 'OPEN' && !permissions.canWithdraw) || schedule.status === 'CLOSED'}
                  >
                    <X className="w-5 h-5 mr-2" />
                    불참
                  </Button>
                );
              })()}
            </div>
            {schedule.voteDeadline && vote.status === 'OPEN' && (
              <p className="text-xs text-stone-500 mt-2 text-center">
                투표 마감: {new Date(schedule.voteDeadline).toLocaleString('ko-KR')}
              </p>
            )}
          </div>
        )}

        {/* Attendees */}
        <div className="bg-white rounded-2xl p-4 border border-stone-100">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-bold text-stone-900">참석자</h3>
            <div className="flex items-center gap-2 text-xs">
              <span className="text-green-600">참석 {attendingCount}</span>
              <span className="text-red-500">불참 {notAttendingCount}</span>
              <span className="text-stone-400">미정 {pendingCount}</span>
              {(schedule.entryFee ?? 0) > 0 && (
                <>
                  <span className="text-stone-300">|</span>
                  <span className="text-blue-600">납부 {paidCount}/{attendingCount}</span>
                </>
              )}
            </div>
          </div>

          <div className="space-y-1">
            {participants.length > 0 ? (
              // 참석자 목록 정렬
              [...participants].sort((a, b) => {
                const aIsPaid = a.feeStatus === 'PAID';
                const bIsPaid = b.feeStatus === 'PAID';
                const aIsAttending = a.attendanceStatus === 'ATTENDING';
                const bIsAttending = b.attendanceStatus === 'ATTENDING';

                // 1. 이상 케이스 (불참/미정인데 돈 낸 사람) 맨 위
                const aIsAnomaly = aIsPaid && !aIsAttending;
                const bIsAnomaly = bIsPaid && !bIsAttending;
                if (aIsAnomaly !== bIsAnomaly) return aIsAnomaly ? -1 : 1;

                // 2. 참석 → 불참 → 미정 순서
                const statusOrder: Record<string, number> = { 'ATTENDING': 0, 'NOT_ATTENDING': 1, 'UNDECIDED': 2 };
                if (statusOrder[a.attendanceStatus] !== statusOrder[b.attendanceStatus]) {
                  return statusOrder[a.attendanceStatus] - statusOrder[b.attendanceStatus];
                }

                // 3. 각 그룹 내에서 돈 낸 사람 우선
                return aIsPaid === bIsPaid ? 0 : (aIsPaid ? -1 : 1);
              }).map(participant => {
                const hasEntryFee = !!(schedule.entryFee != null && schedule.entryFee > 0);
                const isPaid = participant.feeStatus === 'PAID';
                const isRefunded = participant.isRefunded;
                const isScheduleClosed = schedule.status === 'CLOSED';
                const isAttending = participant.attendanceStatus === 'ATTENDING';
                // 이상 케이스: 불참/미정인데 돈을 낸 경우
                const isAnomaly = hasEntryFee && isPaid && !isAttending;

                return (
                  <div
                    key={participant.participantId}
                    className={`flex items-center justify-between py-2 px-2 rounded-lg border-b border-stone-50 last:border-0 ${isAnomaly ? 'bg-yellow-50 border border-yellow-200' : ''
                      }`}
                  >
                    <div className="flex items-center gap-3">
                      <Avatar className="w-9 h-9" draggable={false}>
                        <AvatarFallback>{participant.userName[0]}</AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="flex items-center gap-1">
                          <span className="text-sm font-medium text-stone-900">{participant.userName}</span>
                          {/* 이상 케이스 경고 아이콘 */}
                          {isAnomaly && (
                            <div title="불참/미정인데 납부함">
                              <AlertTriangle className="w-4 h-4 text-yellow-600" />
                            </div>
                          )}
                        </div>
                        {/* 참가비가 있는 경우에만 납부/환급 상태 표시 (참가비 0일 때는 아예 표시 안 함, && 대신 ? : null 사용해 0 렌더 방지) */}
                        {hasEntryFee ? (
                          <div className="flex items-center gap-2 mt-0.5">
                            {isAttending && !isPaid && schedule.status !== 'CANCELLED' ? (
                              <span className="text-xs font-medium text-orange-600">
                                입금 필요: {(schedule.entryFee ?? 0).toLocaleString()}원
                              </span>
                            ) : null}
                            {isAttending && isPaid ? (
                              <span className="text-xs text-green-600">입금 완료</span>
                            ) : null}
                            {!isAttending ? (
                              <span className={`text-xs ${isPaid ? 'text-green-600' : 'text-stone-400'}`}>
                                {isPaid ? '납부완료' : '미납'}
                              </span>
                            ) : null}
                            {isScheduleClosed && isPaid ? (
                              <span className={`text-xs ${isRefunded ? 'text-blue-600' : 'text-stone-400'}`}>
                                • {isRefunded ? '환급완료' : '환급대기'}
                              </span>
                            ) : null}
                          </div>
                        ) : null}
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {/* 운영진 이상인 경우 상태 변경 버튼 표시 */}
                      {permissions.canManageGroup && schedule.status !== 'CANCELLED' && (
                        <div className="flex gap-1 flex-wrap">
                          {/* 참석 상태 변경 버튼 (총무 이상) - 순서 고정: 참석, 불참 - CLOSED여도 활성화 */}
                          <Button
                            variant="ghost"
                            size="sm"
                            className={`h-6 px-1.5 text-xs ${participant.attendanceStatus === 'ATTENDING'
                                ? 'bg-green-100 text-green-700 hover:bg-green-200'
                                : 'text-green-600 hover:text-green-700 hover:bg-green-50'
                              }`}
                            onClick={() => {
                              const newStatus = participant.attendanceStatus === 'ATTENDING' ? 'UNDECIDED' : 'ATTENDING';
                              handleUpdateAttendanceStatus(participant.participantId, newStatus);
                            }}
                          >
                            참석
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className={`h-6 px-1.5 text-xs ${participant.attendanceStatus === 'NOT_ATTENDING'
                                ? 'bg-red-100 text-red-700 hover:bg-red-200'
                                : 'text-red-600 hover:text-red-700 hover:bg-red-50'
                              }`}
                            onClick={() => {
                              const newStatus = participant.attendanceStatus === 'NOT_ATTENDING' ? 'UNDECIDED' : 'NOT_ATTENDING';
                              handleUpdateAttendanceStatus(participant.participantId, newStatus);
                            }}
                          >
                            불참
                          </Button>
                          {/* 납부 상태 토글 (참가비가 있을 때) - CLOSED여도 활성화 */}
                          {hasEntryFee ? (
                            <Button
                              variant="ghost"
                              size="sm"
                              className={`h-6 px-1.5 text-xs ${isPaid ? 'text-red-600 hover:text-red-700' : 'text-green-600 hover:text-green-700'}`}
                              onClick={() => handleUpdateFeeStatus(participant.participantId, isPaid ? 'PENDING' : 'PAID')}
                            >
                              {isPaid ? '납부취소' : '납부확인'}
                            </Button>
                          ) : null}
                          {/* 환급 상태 토글 (일정이 마감되고 납부한 사람만) */}
                          {hasEntryFee && isScheduleClosed && isPaid ? (
                            <Button
                              variant="ghost"
                              size="sm"
                              className={`h-6 px-1.5 text-xs ${isRefunded ? 'text-stone-600 hover:text-stone-700' : 'text-blue-600 hover:text-blue-700'}`}
                              onClick={() => handleUpdateRefundStatus(participant.participantId, !isRefunded)}
                            >
                              {isRefunded ? '환급취소' : '환급완료'}
                            </Button>
                          ) : null}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })
            ) : (
              <p className="text-sm text-stone-500 text-center py-4">아직 참가자가 없습니다</p>
            )}
          </div>
        </div>

        {/* 일정 관리 버튼 - 운영진 이상만, 투표 진행 중일 때만 표시 */}
        {schedule.status === 'OPEN' && permissions.canManageGroup && vote && vote.status === 'OPEN' && (() => {
          // 참가비 여부에 따른 권한 체크
          const hasPermission = (schedule.entryFee ?? 0) > 0 
            ? permissions.canWithdraw  // 참가비 있으면 총무 이상 필요
            : true;                     // 참가비 없으면 운영진(이미 canManageGroup 통과) OK
          
          // 권한 없으면 전체 섹션 숨김
          if (!hasPermission) return null;
          
          return (
            <div className="bg-white rounded-2xl p-4 border border-stone-100">
              <h3 className="font-bold text-stone-900 mb-3">일정 관리</h3>
              <div className="flex gap-3">
                <Button
                  variant="outline"
                  className="flex-1 h-12 rounded-xl border-orange-300 text-orange-600 hover:bg-orange-50"
                  onClick={() => setShowCloseDialog(true)}
                >
                  <Check className="w-5 h-5 mr-2" />
                  참석 투표 마감
                </Button>
                <Button
                  variant="outline"
                  className="flex-1 h-12 rounded-xl border-red-300 text-red-600 hover:bg-red-50"
                  onClick={() => setShowCancelDialog(true)}
                >
                  <X className="w-5 h-5 mr-2" />
                  일정 취소
                </Button>
              </div>
              {(schedule.entryFee ?? 0) > 0 && (
                <p className="text-xs text-stone-500 mt-2">
                  * 참가비가 설정된 일정의 마감/취소는 총무 이상만 가능합니다. 환급은 일정 마무리에서 진행하세요.
                </p>
              )}
            </div>
          );
        })()}

        {/* 취소된 일정 안내 */}
        {schedule.status === 'CANCELLED' && (
          <div className="bg-red-50 rounded-2xl p-4 border border-red-200">
            <div className="flex items-center gap-2 mb-2">
              <AlertCircle className="w-5 h-5 text-red-600" />
              <h3 className="font-bold text-red-900">일정이 취소되었습니다</h3>
            </div>
            {schedule.cancelReason && (
              <p className="text-sm text-red-700">취소 사유: {schedule.cancelReason}</p>
            )}
          </div>
        )}

        {/* 일정/투표 상태 안내 - 하나의 블록, schedule.status 우선 (종료되면 "종료된 일정") */}
        {schedule.status !== 'CANCELLED' && ((vote && vote.status !== 'OPEN') || schedule.status === 'CLOSED') && (
          <div className="bg-stone-100 rounded-2xl p-4 border border-stone-200">
            {schedule.status === 'CLOSED' ? (
              <div className="flex items-center gap-2">
                <Check className="w-5 h-5 text-stone-600" />
                <h3 className="font-bold text-stone-700">종료된 일정입니다</h3>
              </div>
            ) : vote && vote.status !== 'OPEN' ? (
              <>
                <div className="flex items-center gap-2 mb-3">
                  <Check className="w-5 h-5 text-stone-600" />
                  <h3 className="font-bold text-stone-700">투표가 마감된 일정입니다</h3>
                </div>
                {schedule.status === 'OPEN' && permissions.canManageGroup && (
                  <>
                    <div className="flex gap-3">
                      {/* 참가비 여부에 따라 권한 체크 후 렌더링 */}
                      {((schedule.entryFee ?? 0) > 0 ? permissions.canWithdraw : true) && (
                        <Button
                          variant="outline"
                          className="flex-1 h-12 rounded-xl border-orange-300 text-orange-600 hover:bg-orange-50"
                          onClick={() => navigate(`/group/${groupId}/schedule/${scheduleId}/finalize`)}
                        >
                          <Check className="w-5 h-5 mr-2" />
                          일정 종료
                        </Button>
                      )}
                      {((schedule.entryFee ?? 0) > 0 ? permissions.canWithdraw : true) && (
                        <Button
                          variant="outline"
                          className="flex-1 h-12 rounded-xl border-red-300 text-red-600 hover:bg-red-50"
                          onClick={() => setShowCancelDialog(true)}
                        >
                          <X className="w-5 h-5 mr-2" />
                          일정 취소
                        </Button>
                      )}
                    </div>
                    {(schedule.entryFee ?? 0) > 0 && (
                      <p className="text-xs text-stone-500 mt-2">
                        * 참가비가 설정된 일정의 마감/취소는 총무 이상만 가능합니다. 환급은 일정 마무리에서 진행하세요.
                      </p>
                    )}
                  </>
                )}
              </>
            ) : null}
          </div>
        )}

        {/* Comments - 일정과 연결된 게시글이 있을 때만 표시 */}
        {linkedPostId !== null && (
          <div className="bg-white rounded-2xl p-4 border border-stone-100">
            <div className="flex items-center gap-2 mb-4">
              <MessageCircle className="w-5 h-5 text-stone-600" />
              <h3 className="font-bold text-stone-900">댓글</h3>
              <span className="text-sm text-stone-500">{comments.length}</span>
            </div>

            {loadingComments ? (
              <div className="text-center py-8 text-stone-500">댓글을 불러오는 중...</div>
            ) : comments.length > 0 ? (
              <div className="space-y-4 mb-4">
                {comments.map(c => {
                  // 댓글 작성자 정보 조회
                  const commentWriter = members.find(m => m.userId === c.writerId);
                  const commentWriterName = commentWriter?.clubNickname || commentWriter?.realName || `사용자${c.writerId}`;

                  return (
                    <div key={c.commentId} className="flex gap-3">
                      <Avatar className="w-8 h-8" draggable={false}>
                        <AvatarFallback>
                          {commentWriterName[0] || 'U'}
                        </AvatarFallback>
                      </Avatar>
                      <div className="flex-1">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <span className="text-sm font-medium text-stone-900">
                              {commentWriterName}
                            </span>
                            <span className="text-xs text-stone-400">
                              {new Date(c.createdAt).toLocaleDateString('ko-KR', {
                                month: 'short',
                                day: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit'
                              })}
                            </span>
                          </div>
                          {c.writerId === currentUserId && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDeleteComment(c.commentId)}
                              className="h-6 px-2 text-xs text-red-600 hover:text-red-700"
                            >
                              삭제
                            </Button>
                          )}
                        </div>
                        <p className="text-sm text-stone-600 mt-0.5">{c.content}</p>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="text-sm text-stone-500 text-center py-4">아직 댓글이 없습니다</p>
            )}

            <div className="flex gap-2">
              <Textarea
                placeholder="댓글을 입력하세요"
                className="min-h-10 resize-none"
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                maxLength={500}
              />
              <Button
                onClick={handleAddComment}
                disabled={!comment.trim()}
                className="bg-orange-500 hover:bg-orange-600 px-4"
              >
                등록
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Delete Dialog */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>일정 삭제</AlertDialogTitle>
            <AlertDialogDescription>
              정말 이 일정을 삭제하시겠습니까?
              삭제된 일정은 복구할 수 없습니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-red-500 hover:bg-red-600"
            >
              삭제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* 일정 수정 다이얼로그 */}
      <Dialog open={showEditDialog} onOpenChange={setShowEditDialog}>
        <DialogContent className="max-w-[600px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>일정 수정</DialogTitle>
            <DialogDescription>
              일정 정보를 수정할 수 있습니다.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            {/* 기본 정보 */}
            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="edit-title" className="text-base font-medium">
                  일정 제목 <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="edit-title"
                  placeholder="예: 4월 정기 산행"
                  className="h-12 bg-stone-50 border-stone-200 rounded-xl"
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="edit-location" className="text-base font-medium">
                  장소
                </Label>
                <div className="relative">
                  <MapPin className="absolute left-4 top-3.5 w-5 h-5 text-stone-400" />
                  <Input
                    id="edit-location"
                    placeholder="모임 장소를 입력하세요"
                    className="h-12 pl-12 bg-stone-50 border-stone-200 rounded-xl"
                    value={editLocation}
                    onChange={(e) => setEditLocation(e.target.value)}
                  />
                </div>
              </div>
            </div>

            {/* 일시 설정 */}
            <div className="space-y-4 pt-2">
              <h3 className="font-medium text-stone-900 flex items-center gap-2">
                <Calendar className="w-5 h-5 text-stone-500" />
                일시 설정
              </h3>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label className="text-sm text-stone-600">시작 시간</Label>
                  <div className="relative">
                    <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400 pointer-events-none z-10" />
                    <Input
                      type="datetime-local"
                      value={editStartDate}
                      onChange={(e) => setEditStartDate(e.target.value)}
                      className="h-12 bg-stone-50 border-stone-200 rounded-xl pl-11 [&::-webkit-calendar-picker-indicator]:opacity-100 [&::-webkit-calendar-picker-indicator]:cursor-pointer [&::-webkit-calendar-picker-indicator]:relative [&::-webkit-calendar-picker-indicator]:z-20"
                      placeholder="시작 일시 선택"
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label className="text-sm text-stone-600">종료 시간</Label>
                  <div className="relative">
                    <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400 pointer-events-none z-10" />
                    <Input
                      type="datetime-local"
                      value={editEndDate}
                      onChange={(e) => setEditEndDate(e.target.value)}
                      className="h-12 bg-stone-50 border-stone-200 rounded-xl pl-11 [&::-webkit-calendar-picker-indicator]:opacity-100 [&::-webkit-calendar-picker-indicator]:cursor-pointer [&::-webkit-calendar-picker-indicator]:relative [&::-webkit-calendar-picker-indicator]:z-20"
                      placeholder="종료 일시 선택"
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* 참가비 설정 */}
            {permissions.canWithdraw && (
              <div className="space-y-2">
                <Label htmlFor="edit-entryFee" className="text-base font-medium">
                  참가비 (선택) <span className="text-xs text-stone-500">총무 이상만 설정 가능</span>
                </Label>
                <div className="relative">
                  <Input
                    id="edit-entryFee"
                    type="number"
                    placeholder="0"
                    className="h-12 bg-stone-50 border-stone-200 rounded-xl pr-12"
                    value={editEntryFee}
                    onChange={(e) => setEditEntryFee(e.target.value)}
                    min="0"
                  />
                  <span className="absolute right-4 top-1/2 -translate-y-1/2 text-stone-500">원</span>
                </div>
                {schedule && (schedule.entryFee ?? 0) > 0 && (
                  <div className="bg-orange-50 p-3 rounded-xl border border-orange-100 flex gap-2">
                    <AlertCircle className="w-4 h-4 text-orange-600 shrink-0 mt-0.5" />
                    <p className="text-xs text-orange-800">
                      참가비를 변경하면 총무 이상 권한이 필요합니다.
                    </p>
                  </div>
                )}
              </div>
            )}

            {/* 투표 마감일 설정 */}
            <div className="space-y-4 pt-2">
              <h3 className="font-medium text-stone-900 flex items-center gap-2">
                <Clock className="w-5 h-5 text-stone-500" />
                투표 마감일 (선택)
              </h3>
              <div className="space-y-2">
                <Label className="text-sm text-stone-600">언제까지 투표를 받을까요?</Label>
                <div className="relative">
                  <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400 pointer-events-none z-10" />
                  <Input
                    type="datetime-local"
                    value={editVoteDeadline}
                    onChange={(e) => setEditVoteDeadline(e.target.value)}
                    className="h-12 bg-stone-50 border-stone-200 rounded-xl pl-11 [&::-webkit-calendar-picker-indicator]:opacity-100 [&::-webkit-calendar-picker-indicator]:cursor-pointer [&::-webkit-calendar-picker-indicator]:relative [&::-webkit-calendar-picker-indicator]:z-20"
                    placeholder="투표 마감일 선택"
                  />
                </div>
                <p className="text-xs text-stone-500 pl-1">
                  * 투표 마감일은 일정 시작 시간보다 전이어야 합니다. 비워두면 수동으로 마감할 수 있습니다.
                </p>
              </div>
            </div>

            {/* 설명 */}
            <div className="space-y-2">
              <Label htmlFor="edit-description" className="text-base font-medium">설명 (선택)</Label>
              <Textarea
                id="edit-description"
                placeholder="일정에 대한 상세 설명을 입력하세요"
                className="bg-stone-50 border-stone-200 rounded-xl resize-none min-h-[100px]"
                value={editDescription}
                onChange={(e) => setEditDescription(e.target.value)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowEditDialog(false)}>
              취소
            </Button>
            <Button
              onClick={handleUpdateSchedule}
              disabled={isUpdating}
              className="bg-orange-500 hover:bg-orange-600"
            >
              {isUpdating ? '수정 중...' : '수정하기'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 일정 취소 다이얼로그 */}
      <AlertDialog open={showCancelDialog} onOpenChange={setShowCancelDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>일정 취소</AlertDialogTitle>
            <AlertDialogDescription>
              이 일정을 취소하시겠습니까? 취소 사유를 입력해주세요.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <div className="py-4">
            <Textarea
              placeholder="취소 사유를 입력하세요 (필수)"
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              className="min-h-[100px] resize-none"
              maxLength={500}
            />
            {!cancelReason.trim() && (
              <p className="text-xs text-red-500 mt-2">* 취소 사유는 필수 입력입니다.</p>
            )}
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => setCancelReason('')}>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleCancelSchedule}
              disabled={isCancelling || !cancelReason.trim()}
              className="bg-red-500 hover:bg-red-600"
            >
              {isCancelling ? '취소 중...' : '일정 취소'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* 일정 참석 투표 마감 다이얼로그 */}
      <AlertDialog open={showCloseDialog} onOpenChange={setShowCloseDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>일정 참석 투표 마감</AlertDialogTitle>
            <AlertDialogDescription>
              참석 투표를 마감하시겠습니까? 마감 후에는 일반 회원의 투표가 불가능합니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleCloseSchedule}
              disabled={isClosing}
              className="bg-orange-500 hover:bg-orange-600"
            >
              {isClosing ? '마감 중...' : '투표 마감'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* 일정 마무리 다이얼로그 */}
      <Dialog open={showFinalizeDialog} onOpenChange={setShowFinalizeDialog}>
        <DialogContent className="max-w-[420px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>일정 마무리</DialogTitle>
            <DialogDescription>
              총 지출을 입력하고, 환급 대상 참석자를 선택하세요. 기본값은 납부한 사람 N빵입니다.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            {/* 총 지출 입력 */}
            <div className="space-y-2">
              <Label className="text-sm font-medium">총 지출 (원)</Label>
              <Input
                type="number"
                min={0}
                placeholder="0"
                value={finalizeTotalSpent || ''}
                onChange={e => setFinalizeTotalSpent(Number(e.target.value) || 0)}
                className="h-11 bg-stone-50"
              />
            </div>

            {/* 정산 요약 */}
            <div className="bg-stone-50 rounded-xl p-4 space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-stone-600">집계된 참가비</span>
                <span className="font-medium text-stone-900">
                  {(schedule?.collectedEntryFee || 0).toLocaleString()}원
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-stone-600">1인당 환급액 (기본 N빵)</span>
                <span className="font-medium text-blue-600">
                  {finalizeRefundPerPerson.toLocaleString()}원
                </span>
              </div>
              <div className="flex justify-between text-sm text-stone-500">
                <span>환급 대상</span>
                <span>{finalizeParticipantIds.size}명</span>
              </div>
            </div>

            {/* 환급 대상 참석자 (납부 완료) */}
            {schedule && participants.filter(p => p.feeStatus === 'PAID').length > 0 && (
              <div className="space-y-2">
                <Label className="text-sm font-medium">환급 대상 참석자</Label>
                <div className="rounded-xl border border-stone-200 divide-y divide-stone-100 max-h-40 overflow-y-auto">
                  {participants
                    .filter(p => p.feeStatus === 'PAID')
                    .map(p => (
                      <label
                        key={p.participantId}
                        className="flex items-center gap-3 px-3 py-2 hover:bg-stone-50 cursor-pointer"
                      >
                        <input
                          type="checkbox"
                          checked={finalizeParticipantIds.has(p.participantId)}
                          onChange={() => toggleFinalizeParticipant(p.participantId)}
                          className="rounded border-stone-300"
                        />
                        <Avatar className="w-7 h-7">
                          <AvatarFallback className="text-xs">{p.userName[0]}</AvatarFallback>
                        </Avatar>
                        <span className="text-sm font-medium text-stone-800">{p.userName}</span>
                      </label>
                    ))}
                </div>
              </div>
            )}

            <div className="bg-orange-50 p-3 rounded-xl border border-orange-100 flex gap-2">
              <AlertCircle className="w-4 h-4 text-orange-600 shrink-0 mt-0.5" />
              <p className="text-xs text-orange-800">
                일정 마무리 후에는 수정이 불가능합니다. 장부 지출 내역을 확인한 뒤 진행하세요.
              </p>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowFinalizeDialog(false)}>
              취소
            </Button>
            <Button
              onClick={handleFinalize}
              disabled={isFinalizing || ((schedule.entryFee ?? 0) > 0 && finalizeParticipantIds.size === 0)}
              className="bg-orange-500 hover:bg-orange-600"
            >
              {isFinalizing ? '처리 중...' : '마무리 완료'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}



