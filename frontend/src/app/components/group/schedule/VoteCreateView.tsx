import { useState, useEffect, useRef, type CSSProperties } from 'react';
import * as React from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Calendar, MapPin, Clock, AlertCircle, Copy, Check } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Textarea } from '../../ui/textarea';
import { createSchedule, ScheduleCreateRequest } from '../../../../api/schedule';
import { useUserPermissions } from '../../../data/userRoles';
import { NoPermissionView } from '../../common/NoPermissionView';
import { getBankAccount, type BankAccounts } from '../../../../api/bank';

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
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [bankAccount, setBankAccount] = useState<BankAccounts | null>(null);
  const [copied, setCopied] = useState(false);

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

  // 계좌 정보 조회
  useEffect(() => {
    async function fetchBankAccount() {
      if (!groupId) return;
      try {
        const account = await getBankAccount(Number(groupId));
        setBankAccount(account);
      } catch (error) {
        // 계좌가 없을 수 있으므로 에러는 무시 (조용히 처리)
      }
    }
    fetchBankAccount();
  }, [groupId]);

  // 계좌번호 복사
  const handleCopyAccount = () => {
    if (!bankAccount) return;
    navigator.clipboard.writeText(bankAccount.accountNumber.replace(/-/g, ''));
    setCopied(true);
    toast.success('계좌번호가 복사되었습니다');
    setTimeout(() => setCopied(false), 2000);
  };

  // 권한 체크: 일정 생성은 운영진 이상만 가능
  useEffect(() => {
    if (!permissions.canFinalizeSchedule && !permissions.canManageGroup) {
      // 운영진 이상 권한이 없으면 접근 불가
    }
  }, [permissions]);

  // selectstart는 div 표준 prop이 아니므로 ref + addEventListener 사용
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const handler = (e: Event) => {
      const t = e.target as Node;
      if (!(t instanceof HTMLTextAreaElement || t instanceof HTMLInputElement)) {
        e.preventDefault();
      }
    };
    el.addEventListener('selectstart', handler);
    return () => el.removeEventListener('selectstart', handler);
  }, []);

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

    // 날짜 논리 검사
    const start = new Date(startDate);
    const end = new Date(endDate);
    const now = new Date();

    if (end <= start) {
      toast.error('종료 일시는 시작 일시보다 뒤여야 합니다');
      return;
    }

    // 투표 마감일이 설정된 경우에만 검증
    if (voteDeadline) {
      const deadline = new Date(voteDeadline);

      if (deadline >= start) {
        toast.error('투표 마감일은 일정 시작 전이어야 합니다');
        return;
      }

      if (deadline <= now) {
        toast.error('투표 마감일은 현재 시간보다 미래여야 합니다');
        return;
      }
    }

    try {
      setIsSubmitting(true);
      if (!groupId) return;

      // 참가비 권한 체크: 총무 이상만 설정 가능
      const feeAmount = entryFee.trim() ? parseFloat(entryFee) : undefined;
      if (feeAmount !== undefined && feeAmount > 0 && !permissions.canWithdraw) {
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
    <div
      ref={containerRef}
      className="bg-white min-h-screen pb-20 select-none"
      onDragStart={(e) => {
        if (!(e.target instanceof HTMLTextAreaElement || e.target instanceof HTMLInputElement)) {
          e.preventDefault();
          e.dataTransfer.effectAllowed = 'none';
          const canvas = document.createElement('canvas');
          canvas.width = 1;
          canvas.height = 1;
          e.dataTransfer.setDragImage(canvas, 0, 0);
        }
      }}
      onDragOver={(e) => {
        if (!(e.target instanceof HTMLTextAreaElement || e.target instanceof HTMLInputElement)) {
          e.preventDefault();
          e.dataTransfer.dropEffect = 'none';
        }
      }}
      onDragEnd={(e) => {
        if (!(e.target instanceof HTMLTextAreaElement || e.target instanceof HTMLInputElement)) {
          e.preventDefault();
        }
      }}
      style={{
        userSelect: 'none',
        WebkitUserSelect: 'none',
        WebkitUserDrag: 'none',
        backgroundColor: '#ffffff'
      } as CSSProperties}
    >
      <header className="flex items-center p-4 border-b border-stone-100 sticky top-0 bg-white z-10" style={{ backgroundColor: '#ffffff' }}>
        <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
          <ArrowLeft className="w-5 h-5 text-stone-600" />
        </Button>
        <h1 className="text-lg font-semibold ml-2 text-stone-800">일정 투표 만들기</h1>
      </header>

      <div className="p-6 space-y-6" style={{ backgroundColor: '#ffffff' }}>
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
              className="h-12 bg-stone-50 border-stone-200 rounded-xl select-text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              onDragStart={(e) => e.stopPropagation()}
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
                className="h-12 pl-12 bg-stone-50 border-stone-200 rounded-xl select-text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                onDragStart={(e) => e.stopPropagation()}
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
                  className="h-12 bg-stone-50 border-stone-200 rounded-xl pl-11 [&::-webkit-calendar-picker-indicator]:opacity-100 [&::-webkit-calendar-picker-indicator]:cursor-pointer [&::-webkit-calendar-picker-indicator]:relative [&::-webkit-calendar-picker-indicator]:z-20 select-text"
                  onDragStart={(e) => e.stopPropagation()}
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
                  className="h-12 bg-stone-50 border-stone-200 rounded-xl pl-11 [&::-webkit-calendar-picker-indicator]:opacity-100 [&::-webkit-calendar-picker-indicator]:cursor-pointer [&::-webkit-calendar-picker-indicator]:relative [&::-webkit-calendar-picker-indicator]:z-20 select-text"
                  onDragStart={(e) => e.stopPropagation()}
                  placeholder="종료 일시 선택"
                />
              </div>
            </div>
          </div>
        </div>

        {/* 투표 마감일 (선택) */}
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
                value={voteDeadline}
                onChange={(e) => setVoteDeadline(e.target.value)}
                className="h-12 bg-stone-50 border-stone-200 rounded-xl pl-11 [&::-webkit-calendar-picker-indicator]:opacity-100 [&::-webkit-calendar-picker-indicator]:cursor-pointer [&::-webkit-calendar-picker-indicator]:relative [&::-webkit-calendar-picker-indicator]:z-20 select-text"
                onDragStart={(e) => e.stopPropagation()}
                placeholder="투표 마감일 선택"
              />
            </div>
            <p className="text-xs text-stone-500 pl-1">
              * 투표 마감일을 설정하지 않으면 수동으로 마감할 수 있습니다.
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
                placeholder="참가비를 입력하세요"
                className="h-12 bg-stone-50 border-stone-200 rounded-xl pr-12 select-text"
                value={entryFee}
                onChange={(e) => setEntryFee(e.target.value)}
                onDragStart={(e) => e.stopPropagation()}
                min="0"
              />
              <span className="absolute right-4 top-1/2 -translate-y-1/2 text-stone-500">원</span>
            </div>
            <p className="text-xs text-stone-500 pl-1">
              참가비를 설정하면 일정 참석 시 자동으로 입금 요청이 생성됩니다.
            </p>
            {/* 참가비가 입력되었을 때 계좌 정보 표시 */}
            {entryFee && parseFloat(entryFee) > 0 && bankAccount && (
              <div className="bg-orange-50 rounded-xl p-4 border border-orange-100 space-y-2 mt-3">
                <h4 className="text-sm font-semibold text-stone-700">입금 계좌</h4>
                <div className="bg-white rounded-lg p-3 space-y-2">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-stone-500">은행</span>
                    <span className="font-medium text-stone-900">{getBankName(bankAccount.bankCode)}</span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-stone-500">계좌번호</span>
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-stone-900 font-mono">{bankAccount.accountNumber}</span>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={handleCopyAccount}
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
              </div>
            )}
          </div>
        )}

        {/* 설명 */}
        <div className="space-y-2">
          <Label htmlFor="description" className="text-base font-medium">설명 (선택)</Label>
          <Textarea
            id="description"
            placeholder="일정에 대한 상세 설명을 입력하세요"
            className="bg-stone-50 border-stone-200 rounded-xl resize-none min-h-[100px] select-text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            onDragStart={(e) => e.stopPropagation()}
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
