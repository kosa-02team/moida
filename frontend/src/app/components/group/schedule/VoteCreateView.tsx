import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Calendar, MapPin, Clock, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Textarea } from '../../ui/textarea';
import { createSchedule, ScheduleCreateRequest } from '../../../../api/schedule';
import { useUserPermissions } from '../../../data/userRoles';
import { NoPermissionView } from '../../common/NoPermissionView';

export function VoteCreateView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const permissions = useUserPermissions(groupId || '1');

  // 폼 상태
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [location, setLocation] = useState('');
  const [voteDeadline, setVoteDeadline] = useState('');
  const [entryFee, setEntryFee] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 권한 체크: 일정 생성은 운영진 이상만 가능
  useEffect(() => {
    if (!permissions.canFinalizeSchedule && !permissions.canManageGroup) {
      // 운영진 이상 권한이 없으면 접근 불가
    }
  }, [permissions]);

  if (!permissions.canFinalizeSchedule && !permissions.canManageGroup) {
    return <NoPermissionView message="일정 생성은 운영진 이상만 가능합니다." />;
  }

  const handleSubmit = async () => {
    // 1. 유효성 검사
    if (!title.trim()) {
      toast.error('일정 제목을 입력해주세요');
      return;
    }
    if (!startDate) {
      toast.error('시작 일시를 선택해주세요');
      return;
    }
    if (!endDate) {
      toast.error('종료 일시를 선택해주세요');
      return;
    }
    if (!location.trim()) {
      toast.error('장소를 입력해주세요');
      return;
    }
    if (!voteDeadline) {
      toast.error('투표 마감일을 선택해주세요');
      return;
    }

    // 날짜 논리 검사
    const start = new Date(startDate);
    const end = new Date(endDate);
    const deadline = new Date(voteDeadline);
    const now = new Date();

    if (end <= start) {
      toast.error('종료 일시는 시작 일시보다 뒤여야 합니다');
      return;
    }

    if (deadline >= start) {
      toast.error('투표 마감일은 일정 시작 전이어야 합니다');
      return;
    }

    if (deadline <= now) {
      toast.error('투표 마감일은 현재 시간보다 미래여야 합니다');
      return;
    }

    try {
      setIsSubmitting(true);
      if (!groupId) return;

      // 참가비 권한 체크: 총무 이상만 설정 가능
      const feeAmount = entryFee.trim() ? parseFloat(entryFee) : 0;
      if (feeAmount > 0 && !permissions.canWithdraw) {
        toast.error('참가비 설정은 총무 이상만 가능합니다');
        return;
      }

      // 2. 일정 생성 API 호출 (투표 마감일을 포함하면 자동으로 참/불 투표가 생성됨)
      const request: ScheduleCreateRequest = {
        scheduleName: title,
        eventDate: startDate, // "YYYY-MM-DDTHH:mm" 형식 그대로 전송
        endDate: endDate,
        location: location,
        description: description,
        voteDeadline: voteDeadline,
        entryFee: feeAmount
      };

      await createSchedule(Number(groupId), request);

      toast.success('일정 및 투표가 생성되었습니다');
      navigate('..'); // 목록으로 돌아가기

    } catch (error) {
      console.error('Failed to create schedule/vote:', error);
      toast.error('일정 생성에 실패했습니다');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="bg-white min-h-screen pb-20" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      <header className="flex items-center p-4 border-b border-stone-100 sticky top-0 bg-white z-10">
        <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
          <ArrowLeft className="w-5 h-5 text-stone-600" />
        </Button>
        <h1 className="text-lg font-semibold ml-2 text-stone-800">일정 투표 만들기</h1>
      </header>

      <div className="p-6 space-y-6">
        {/* 안내 메시지 */}
        <div className="bg-orange-50 p-4 rounded-xl border border-orange-100 flex gap-3">
          <AlertCircle className="w-5 h-5 text-orange-600 shrink-0 mt-0.5" />
          <div className="text-sm text-orange-800">
            <p className="font-medium mb-1">참석 여부 투표가 자동으로 생성됩니다</p>
            <p className="text-orange-700/80">일정을 확정하고 멤버들의 참석 여부를 조사합니다.</p>
          </div>
        </div>

        {/* 기본 정보 */}
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="title" className="text-base font-medium">
              일정 제목 <span className="text-red-500">*</span>
            </Label>
            <Input
              id="title"
              placeholder="예: 4월 정기 산행"
              className="h-12 bg-stone-50 border-stone-200 rounded-xl"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="location" className="text-base font-medium">
              장소 <span className="text-red-500">*</span>
            </Label>
            <div className="relative">
              <MapPin className="absolute left-4 top-3.5 w-5 h-5 text-stone-400" />
              <Input
                id="location"
                placeholder="모임 장소를 입력하세요"
                className="h-12 pl-12 bg-stone-50 border-stone-200 rounded-xl"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
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
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
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
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="h-12 bg-stone-50 border-stone-200 rounded-xl pl-11 [&::-webkit-calendar-picker-indicator]:opacity-100 [&::-webkit-calendar-picker-indicator]:cursor-pointer [&::-webkit-calendar-picker-indicator]:relative [&::-webkit-calendar-picker-indicator]:z-20"
                  placeholder="종료 일시 선택"
                />
              </div>
            </div>
          </div>
        </div>

        {/* 투표 마감일 */}
        <div className="space-y-4 pt-2">
          <h3 className="font-medium text-stone-900 flex items-center gap-2">
            <Clock className="w-5 h-5 text-stone-500" />
            투표 마감일
          </h3>

          <div className="space-y-2">
            <Label className="text-sm text-stone-600">언제까지 투표를 받을까요?</Label>
            <div className="relative">
              <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400 pointer-events-none z-10" />
              <Input
                type="datetime-local"
                value={voteDeadline}
                onChange={(e) => setVoteDeadline(e.target.value)}
                className="h-12 bg-stone-50 border-stone-200 rounded-xl pl-11 [&::-webkit-calendar-picker-indicator]:opacity-100 [&::-webkit-calendar-picker-indicator]:cursor-pointer [&::-webkit-calendar-picker-indicator]:relative [&::-webkit-calendar-picker-indicator]:z-20"
                placeholder="투표 마감일 선택"
              />
            </div>
            <p className="text-xs text-stone-500 pl-1">
              * 투표 마감일은 일정 시작 시간보다 전이어야 합니다.
            </p>
          </div>
        </div>

        {/* 참가비 설정 */}
        {permissions.canWithdraw && (
          <div className="space-y-2">
            <Label htmlFor="entryFee" className="text-base font-medium">
              참가비 (선택) <span className="text-xs text-stone-500">총무 이상만 설정 가능</span>
            </Label>
            <div className="relative">
              <Input
                id="entryFee"
                type="number"
                placeholder="0"
                className="h-12 bg-stone-50 border-stone-200 rounded-xl pr-12"
                value={entryFee}
                onChange={(e) => setEntryFee(e.target.value)}
                min="0"
              />
              <span className="absolute right-4 top-1/2 -translate-y-1/2 text-stone-500">원</span>
            </div>
            <p className="text-xs text-stone-500 pl-1">
              참가비를 설정하면 일정 참석 시 자동으로 입금 요청이 생성됩니다.
            </p>
          </div>
        )}

        {/* 설명 */}
        <div className="space-y-2">
          <Label htmlFor="description" className="text-base font-medium">설명 (선택)</Label>
          <Textarea
            id="description"
            placeholder="일정에 대한 상세 설명을 입력하세요"
            className="bg-stone-50 border-stone-200 rounded-xl resize-none min-h-[100px]"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>

        {/* 제출 버튼 */}
        <div className="pt-4">
          <Button
            onClick={handleSubmit}
            disabled={isSubmitting}
            className="w-full h-12 text-lg bg-orange-500 hover:bg-orange-600 rounded-xl text-white shadow-md disabled:opacity-50"
          >
            {isSubmitting ? '생성 중...' : '투표 올리기'}
          </Button>
        </div>
      </div>
    </div>
  );
}
