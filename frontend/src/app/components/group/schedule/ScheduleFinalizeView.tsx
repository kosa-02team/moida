import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Check, X, UserCheck, UserX, AlertTriangle, Calculator, CheckCircle2 } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '../../ui/card';
import { Avatar, AvatarFallback, AvatarImage } from '../../ui/avatar';
import { Badge } from '../../ui/badge';
import { getSchedule, getScheduleParticipants, updateParticipantAttendance, finalizeSchedule, type ScheduleResponse, type ScheduleParticipantResponse, type ScheduleParticipantUpdateRequest } from '../../../../api/schedule';
import { useUserPermissions } from '../../../data/userRoles';
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

interface Participant {
  id: string;
  name: string;
  avatar?: string;
  voteStatus: 'yes' | 'no' | 'pending'; // 투표 상태
  actualStatus: 'attended' | 'absent' | 'pending'; // 실제 참석 상태
  amountPaid: number; // 납부 금액
  amountDue: number; // 정산 금액
}

export function ScheduleFinalizeView() {
  const navigate = useNavigate();
  const { groupId, scheduleId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [schedule, setSchedule] = useState<ScheduleResponse | null>(null);
  const [participants, setParticipants] = useState<ScheduleParticipantResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [totalSpent, setTotalSpent] = useState(0);
  const [refundPerPerson, setRefundPerPerson] = useState(0);
  const [updatingParticipantId, setUpdatingParticipantId] = useState<number | null>(null);

  useEffect(() => {
    async function fetchData() {
      if (!groupId || !scheduleId) return;
      try {
        setLoading(true);
        const [scheduleData, participantsData] = await Promise.all([
          getSchedule(Number(groupId), Number(scheduleId)),
          getScheduleParticipants(Number(groupId), Number(scheduleId))
        ]);
        setSchedule(scheduleData);
        setParticipants(participantsData);
        setTotalSpent(scheduleData.totalSpent || 0);
        
        // refundPerPerson 계산: 총 비용을 참석자 수로 나누기
        const attendingCount = participantsData.filter(p => p.attendanceStatus === 'ATTENDING').length;
        const calculatedRefundPerPerson = attendingCount > 0 && scheduleData.totalSpent 
          ? scheduleData.totalSpent / attendingCount 
          : 0;
        setRefundPerPerson(calculatedRefundPerPerson);
      } catch (error) {
        console.error('일정 정보 불러오기 실패:', error);
        toast.error('일정 정보를 불러오는데 실패했습니다.');
        navigate(-1);
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, [groupId, scheduleId, navigate]);

  // refundPerPerson 재계산 (participants, totalSpent 변경 시)
  // 돈 낸 사람들(PAID 상태) 기준으로 계산
  useEffect(() => {
    if (participants.length > 0 && schedule) {
      // 납부 완료한 사람들만 필터링
      const paidParticipants = participants.filter(p => p.feeStatus === 'PAID');
      const paidCount = paidParticipants.length;
      
      if (paidCount > 0 && schedule.entryFee) {
        // 총 수입 = 납부 완료 인원 × 참가비
        const totalIncome = paidCount * schedule.entryFee;
        // 잔액 = 총 수입 - 총 지출
        const balance = totalIncome - totalSpent;
        // 환급액 = 잔액 / 납부 인원수 (FLOOR 처리)
        const calculatedRefundPerPerson = balance > 0 
          ? Math.floor(balance / paidCount)
          : 0;
        setRefundPerPerson(calculatedRefundPerPerson);
      } else {
        setRefundPerPerson(0);
      }
    }
  }, [participants, totalSpent, schedule]);

  if (loading || !schedule) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  const participantsDisplay: Participant[] = participants.map(p => {
    const isAttending = p.attendanceStatus === 'ATTENDING';
    const isNotAttending = p.attendanceStatus === 'NOT_ATTENDING';
    const hasPaid = p.feeStatus === 'PAID';
    const entryFee = schedule.entryFee || 0;
    const amountPaid = hasPaid ? entryFee : 0;
    
    // 정산 계산 로직
    let amountDue = 0;
    if (isAttending) {
      // 참석한 경우: 1인당 비용 - 이미 낸 참가비
      amountDue = refundPerPerson - amountPaid;
    } else if (isNotAttending && hasPaid) {
      // 불참했지만 참가비를 낸 경우: 환불
      amountDue = -amountPaid;
    } else if (isNotAttending && !hasPaid) {
      // 불참하고 참가비도 안 낸 경우: 정산 없음
      amountDue = 0;
    }
    
    return {
      id: String(p.participantId),
      name: p.userName,
      avatar: '',
      voteStatus: isAttending ? 'yes' as const : isNotAttending ? 'no' as const : 'pending' as const,
      actualStatus: isAttending ? 'attended' as const : isNotAttending ? 'absent' as const : 'pending' as const,
      amountPaid,
      amountDue,
    };
  });

  const toggleActualStatus = async (id: string, status: 'attended' | 'absent') => {
    if (!groupId || !scheduleId) return;
    
    const participant = participants.find(p => String(p.participantId) === id);
    if (!participant) return;
    
    const attendanceStatus = status === 'attended' ? 'ATTENDING' : 'NOT_ATTENDING';
    
    try {
      setUpdatingParticipantId(participant.participantId);
      const updateRequest: ScheduleParticipantUpdateRequest = {
        attendanceStatus: attendanceStatus as 'ATTENDING' | 'NOT_ATTENDING'
      };
      await updateParticipantAttendance(
        Number(groupId),
        Number(scheduleId),
        participant.participantId,
        updateRequest
      );
      
      // 참여자 목록 다시 불러오기
      const participantsData = await getScheduleParticipants(Number(groupId), Number(scheduleId));
      setParticipants(participantsData);
      
      toast.success('참석 상태가 업데이트되었습니다');
    } catch (error) {
      console.error('참석 상태 업데이트 실패:', error);
      toast.error('참석 상태 업데이트에 실패했습니다');
    } finally {
      setUpdatingParticipantId(null);
    }
  };

  // 통계
  const stats = {
    totalAttended: participantsDisplay.filter(p => p.actualStatus === 'attended').length,
    totalAbsent: participantsDisplay.filter(p => p.actualStatus === 'absent').length,
    totalPending: participantsDisplay.filter(p => p.actualStatus === 'pending').length,
    totalRefund: participantsDisplay.filter(p => p.amountDue < 0).reduce((sum, p) => sum + Math.abs(p.amountDue), 0),
    totalCollect: participantsDisplay.filter(p => p.amountDue > 0).reduce((sum, p) => sum + p.amountDue, 0),
  };

  // 이상 케이스 분류
  const voteYesActualNo = participantsDisplay.filter(p => p.voteStatus === 'yes' && p.actualStatus === 'absent');
  const voteNoActualYes = participantsDisplay.filter(p => (p.voteStatus === 'no' || p.voteStatus === 'pending') && p.actualStatus === 'attended');

  const handleFinalize = () => {
    if (stats.totalPending > 0) {
      toast.error('모든 참가자의 참석 여부를 확인해주세요');
      return;
    }
    setShowConfirmDialog(true);
  };

  const confirmFinalize = async () => {
    if (!groupId || !scheduleId) return;
    
    setIsSubmitting(true);
    try {
      // 일정 마무리 (정산 및 환급 처리)
      await finalizeSchedule(Number(groupId), Number(scheduleId), {
        totalSpent: totalSpent || 0,
      });
      
      toast.success('일정이 마무리되었습니다. 정산이 처리됩니다.');
      setShowConfirmDialog(false);
      // 일정 상세 페이지로 이동 (데이터 새로고침을 위해)
      navigate(`/group/${groupId}/schedule/${scheduleId}`);
    } catch (error: any) {
      console.error('일정 마무리 실패:', error);
      const errorMessage = error?.response?.data?.message || error?.message || '일정 마무리에 실패했습니다.';
      toast.error(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-32">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center px-4 py-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="ml-2 text-lg font-semibold text-stone-800">일정 마무리</h1>
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Schedule Info */}
        <Card className="border-orange-200 bg-orange-50">
          <CardContent className="p-4">
            <h2 className="font-bold text-lg text-orange-900">{schedule.scheduleName}</h2>
            <p className="text-sm text-orange-700">
              {new Date(schedule.eventDate).toLocaleDateString('ko-KR')} · {schedule.location || '미정'}
            </p>
            <div className="mt-3 space-y-3">
              <div className="flex gap-4 text-sm">
                {schedule.entryFee && (
                  <div>
                    <span className="text-orange-600">참가비:</span>
                    <span className="font-bold text-orange-900 ml-1">{schedule.entryFee.toLocaleString()}원</span>
                  </div>
                )}
              </div>
              {permissions.canWithdraw && (
                <div className="space-y-2">
                  <Label htmlFor="totalSpent" className="text-sm font-medium text-orange-800">
                    총 지출 금액 (총무 이상 수정 가능)
                  </Label>
                  <Input
                    id="totalSpent"
                    type="number"
                    value={totalSpent}
                    onChange={(e) => {
                      const value = parseInt(e.target.value) || 0;
                      setTotalSpent(value);
                    }}
                    className="bg-white border-orange-200"
                    placeholder="0"
                    min="0"
                  />
                </div>
              )}
              {!permissions.canWithdraw && (
                <div>
                  <span className="text-orange-600">총 지출:</span>
                  <span className="font-bold text-orange-900 ml-1">{totalSpent.toLocaleString()}원</span>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-3">
          <Card className="border-green-200 bg-green-50">
            <CardContent className="p-3 text-center">
              <UserCheck className="w-6 h-6 text-green-600 mx-auto" />
              <p className="text-2xl font-bold text-green-700 mt-1">{stats.totalAttended}</p>
              <p className="text-xs text-green-600">참석</p>
            </CardContent>
          </Card>
          <Card className="border-red-200 bg-red-50">
            <CardContent className="p-3 text-center">
              <UserX className="w-6 h-6 text-red-600 mx-auto" />
              <p className="text-2xl font-bold text-red-700 mt-1">{stats.totalAbsent}</p>
              <p className="text-xs text-red-600">불참</p>
            </CardContent>
          </Card>
          <Card className="border-stone-200 bg-stone-100">
            <CardContent className="p-3 text-center">
              <AlertTriangle className="w-6 h-6 text-stone-500 mx-auto" />
              <p className="text-2xl font-bold text-stone-700 mt-1">{stats.totalPending}</p>
              <p className="text-xs text-stone-500">미확인</p>
            </CardContent>
          </Card>
        </div>

        {/* Issue Cases */}
        {(voteYesActualNo.length > 0 || voteNoActualYes.length > 0) && (
          <Card className="border-amber-200 bg-amber-50">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm flex items-center gap-2 text-amber-800">
                <AlertTriangle className="w-4 h-4" />
                확인 필요 ({voteYesActualNo.length + voteNoActualYes.length}명)
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0">
              {voteYesActualNo.length > 0 && (
                <div className="mb-2">
                  <p className="text-xs text-amber-700 font-medium">투표 참여 → 실제 불참 (환불 대상)</p>
                  <div className="flex flex-wrap gap-1 mt-1">
                    {voteYesActualNo.map(p => (
                      <Badge key={p.id} variant="secondary" className="bg-red-100 text-red-700">
                        {p.name}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}
              {voteNoActualYes.length > 0 && (
                <div>
                  <p className="text-xs text-amber-700 font-medium">투표 미참여/불참 → 실제 참석 (추가 납부 대상)</p>
                  <div className="flex flex-wrap gap-1 mt-1">
                    {voteNoActualYes.map(p => (
                      <Badge key={p.id} variant="secondary" className="bg-blue-100 text-blue-700">
                        {p.name}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {/* Participant List */}
        <div className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
          <div className="px-4 py-3 bg-stone-50 border-b border-stone-100">
            <h3 className="font-bold text-stone-900">참가자 확인</h3>
            <p className="text-xs text-stone-500">실제 참석 여부를 확인하세요</p>
          </div>
          <div className="divide-y divide-stone-100">
            {participantsDisplay.map(p => (
              <div key={p.id} className="p-4">
                <div className="flex items-center gap-3">
                  <Avatar className="w-10 h-10">
                    <AvatarImage src={p.avatar} />
                    <AvatarFallback>{p.name[0]}</AvatarFallback>
                  </Avatar>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <p className="font-medium text-stone-900">{p.name}</p>
                      <Badge variant="secondary" className={
                        p.voteStatus === 'yes' ? 'bg-green-100 text-green-700' :
                        p.voteStatus === 'no' ? 'bg-red-100 text-red-700' :
                        'bg-stone-100 text-stone-600'
                      }>
                        투표: {p.voteStatus === 'yes' ? '참여' : p.voteStatus === 'no' ? '불참' : '미응답'}
                      </Badge>
                    </div>
                    {p.amountDue !== 0 && (
                      <p className={`text-xs mt-1 ${p.amountDue > 0 ? 'text-blue-600' : 'text-red-600'}`}>
                        {p.amountDue > 0 ? `추가 납부: +${p.amountDue.toLocaleString()}원` : `환불: ${p.amountDue.toLocaleString()}원`}
                      </p>
                    )}
                  </div>
                  {permissions.canWithdraw && (
                    <div className="flex gap-2">
                      <button
                        onClick={() => toggleActualStatus(p.id, 'attended')}
                        disabled={updatingParticipantId === Number(p.id)}
                        className={`w-10 h-10 rounded-full flex items-center justify-center transition-colors ${
                          p.actualStatus === 'attended' 
                            ? 'bg-green-500 text-white' 
                            : 'bg-stone-100 text-stone-400 hover:bg-green-100 hover:text-green-600'
                        } disabled:opacity-50 disabled:cursor-not-allowed`}
                      >
                        <Check className="w-5 h-5" />
                      </button>
                      <button
                        onClick={() => toggleActualStatus(p.id, 'absent')}
                        disabled={updatingParticipantId === Number(p.id)}
                        className={`w-10 h-10 rounded-full flex items-center justify-center transition-colors ${
                          p.actualStatus === 'absent' 
                            ? 'bg-red-500 text-white' 
                            : 'bg-stone-100 text-stone-400 hover:bg-red-100 hover:text-red-600'
                        } disabled:opacity-50 disabled:cursor-not-allowed`}
                      >
                        <X className="w-5 h-5" />
                      </button>
                    </div>
                  )}
                  {!permissions.canWithdraw && (
                    <Badge variant="secondary" className={
                      p.actualStatus === 'attended' ? 'bg-green-100 text-green-700' :
                      p.actualStatus === 'absent' ? 'bg-red-100 text-red-700' :
                      'bg-stone-100 text-stone-600'
                    }>
                      {p.actualStatus === 'attended' ? '참석' : p.actualStatus === 'absent' ? '불참' : '미확인'}
                    </Badge>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Settlement Summary */}
        <Card className="border-blue-200 bg-blue-50">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm flex items-center gap-2 text-blue-800">
              <Calculator className="w-4 h-4" />
              정산 요약
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-0 space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-blue-700">환불 예정</span>
              <span className="font-bold text-red-600">-{stats.totalRefund.toLocaleString()}원</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-blue-700">추가 수금</span>
              <span className="font-bold text-blue-600">+{stats.totalCollect.toLocaleString()}원</span>
            </div>
            <div className="border-t border-blue-200 pt-2 flex justify-between">
              <span className="text-blue-800 font-medium">순 정산액</span>
              <span className={`font-bold ${stats.totalCollect - stats.totalRefund >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                {(stats.totalCollect - stats.totalRefund >= 0 ? '+' : '')}{(stats.totalCollect - stats.totalRefund).toLocaleString()}원
              </span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Bottom Button */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-stone-100 p-4 safe-area-pb">
        <div className="max-w-md mx-auto">
          <Button
            onClick={handleFinalize}
            disabled={stats.totalPending > 0}
            className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl disabled:opacity-50"
          >
            <CheckCircle2 className="w-5 h-5 mr-2" />
            일정 마무리 및 정산
          </Button>
          {stats.totalPending > 0 && (
            <p className="text-xs text-center text-red-500 mt-2">
              {stats.totalPending}명의 참석 여부를 확인해주세요
            </p>
          )}
        </div>
      </div>

      {/* Confirm Dialog */}
      <AlertDialog open={showConfirmDialog} onOpenChange={setShowConfirmDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>일정 마무리</AlertDialogTitle>
            <AlertDialogDescription asChild>
              <div className="space-y-3">
                <p>아래 내용으로 일정을 마무리하시겠습니까?</p>
                <div className="bg-stone-50 rounded-lg p-4 space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-stone-600">참석자</span>
                    <span className="font-bold">{stats.totalAttended}명</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-stone-600">불참자</span>
                    <span className="font-bold">{stats.totalAbsent}명</span>
                  </div>
                  <div className="flex justify-between border-t pt-2">
                    <span className="text-stone-600">환불 예정</span>
                    <span className="font-bold text-red-600">-{stats.totalRefund.toLocaleString()}원</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-stone-600">추가 수금</span>
                    <span className="font-bold text-blue-600">+{stats.totalCollect.toLocaleString()}원</span>
                  </div>
                </div>
                <p className="text-xs text-amber-600">
                  ⚠️ 마무리 후에는 수정할 수 없습니다.
                </p>
              </div>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={confirmFinalize}
              disabled={isSubmitting}
              className="bg-orange-500 hover:bg-orange-600"
            >
              {isSubmitting ? '처리 중...' : '마무리하기'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}



