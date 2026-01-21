import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Plus, X, Clock, Users, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Textarea } from '../../ui/textarea';
import { Switch } from '../../ui/switch';
import { createVote, type VoteCreateRequest, type VoteOptionCreateRequest } from '../../../../api/vote';
import { useUserPermissions } from '../../../data/userRoles';
import { NoPermissionView } from '../../common/NoPermissionView';

export function VoteCreateView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const permissions = useUserPermissions(groupId || '1');

  // 폼 상태
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [isAnonymous, setIsAnonymous] = useState(false);
  const [allowMultiple, setAllowMultiple] = useState(false);
  const [deadline, setDeadline] = useState('');
  const [options, setOptions] = useState<VoteOptionCreateRequest[]>([
    { optionText: '', order: 1 }
  ]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 권한 체크: 운영진 이상만 가능
  if (!permissions.canManageGroup) {
    return <NoPermissionView message="투표 생성은 운영진 이상만 가능합니다." />;
  }

  const addOption = () => {
    if (options.length >= 10) {
      toast.error('투표 옵션은 최대 10개까지 추가할 수 있습니다');
      return;
    }
    setOptions([...options, { optionText: '', order: options.length + 1 }]);
  };

  const removeOption = (index: number) => {
    if (options.length <= 2) {
      toast.error('투표 옵션은 최소 2개 이상 필요합니다');
      return;
    }
    const newOptions = options.filter((_, i) => i !== index).map((opt, i) => ({
      ...opt,
      order: i + 1
    }));
    setOptions(newOptions);
  };

  const updateOption = (index: number, text: string) => {
    const newOptions = [...options];
    newOptions[index] = { ...newOptions[index], optionText: text };
    setOptions(newOptions);
  };

  const handleSubmit = async () => {
    // 유효성 검사
    if (!title.trim()) {
      toast.error('투표 제목을 입력해주세요');
      return;
    }

    if (options.length < 2) {
      toast.error('투표 옵션은 최소 2개 이상 필요합니다');
      return;
    }

    const validOptions = options.filter(opt => opt.optionText.trim());
    if (validOptions.length < 2) {
      toast.error('모든 투표 옵션에 내용을 입력해주세요');
      return;
    }

    if (deadline) {
      const deadlineDate = new Date(deadline);
      const now = new Date();
      if (deadlineDate <= now) {
        toast.error('마감일은 현재 시간보다 미래여야 합니다');
        return;
      }
    }

    try {
      setIsSubmitting(true);
      if (!groupId) return;

      const request: VoteCreateRequest = {
        voteType: 'GENERAL', // 일반 투표
        title: title.trim(),
        description: description.trim() || undefined,
        isAnonymous,
        allowMultiple,
        deadline: deadline || undefined,
        options: validOptions.map((opt, index) => ({
          optionText: opt.optionText.trim(),
          order: index + 1
        }))
      };

      const response = await createVote(Number(groupId), request);
      toast.success('투표가 생성되었습니다');
      navigate(`/group/${groupId}/vote/${response.voteId}`);
    } catch (error) {
      console.error('투표 생성 실패:', error);
      toast.error('투표 생성에 실패했습니다');
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
        <h1 className="text-lg font-semibold ml-2 text-stone-800">투표 만들기</h1>
      </header>

      <div className="p-6 space-y-6">
        {/* 기본 정보 */}
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="title" className="text-base font-medium">
              투표 제목 <span className="text-red-500">*</span>
            </Label>
            <Input
              id="title"
              placeholder="예: 다음 모임 장소 선택"
              className="h-12 bg-stone-50 border-stone-200 rounded-xl"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="description" className="text-base font-medium">설명 (선택)</Label>
            <Textarea
              id="description"
              placeholder="투표에 대한 설명을 입력하세요"
              className="bg-stone-50 border-stone-200 rounded-xl resize-none min-h-[100px]"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
        </div>

        {/* 투표 옵션 */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <Label className="text-base font-medium">
              투표 옵션 <span className="text-red-500">*</span>
            </Label>
            <Button
              variant="outline"
              size="sm"
              onClick={addOption}
              disabled={options.length >= 10}
              className="text-sm"
            >
              <Plus className="w-4 h-4 mr-1" />
              옵션 추가
            </Button>
          </div>

          <div className="space-y-3">
            {options.map((option, index) => (
              <div key={index} className="flex items-center gap-2">
                <div className="flex-1">
                  <Input
                    placeholder={`옵션 ${index + 1}`}
                    className="h-12 bg-stone-50 border-stone-200 rounded-xl"
                    value={option.optionText}
                    onChange={(e) => updateOption(index, e.target.value)}
                  />
                </div>
                {options.length > 2 && (
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => removeOption(index)}
                    className="text-red-500 hover:text-red-600 hover:bg-red-50"
                  >
                    <X className="w-5 h-5" />
                  </Button>
                )}
              </div>
            ))}
          </div>
          <p className="text-xs text-stone-500 pl-1">
            최소 2개, 최대 10개의 옵션을 추가할 수 있습니다.
          </p>
        </div>

        {/* 투표 설정 */}
        <div className="space-y-4 pt-2">
          <h3 className="font-medium text-stone-900">투표 설정</h3>

          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-stone-50 rounded-xl">
              <div className="space-y-0.5">
                <Label htmlFor="allowMultiple" className="text-sm font-medium cursor-pointer">
                  복수 선택 허용
                </Label>
                <p className="text-xs text-stone-500">여러 옵션을 선택할 수 있습니다</p>
              </div>
              <Switch
                id="allowMultiple"
                checked={allowMultiple}
                onCheckedChange={setAllowMultiple}
              />
            </div>

            <div className="flex items-center justify-between p-4 bg-stone-50 rounded-xl">
              <div className="space-y-0.5">
                <Label htmlFor="isAnonymous" className="text-sm font-medium cursor-pointer">
                  익명 투표
                </Label>
                <p className="text-xs text-stone-500">투표한 사람을 숨깁니다</p>
              </div>
              <Switch
                id="isAnonymous"
                checked={isAnonymous}
                onCheckedChange={setIsAnonymous}
              />
            </div>
          </div>
        </div>

        {/* 마감일 설정 */}
        <div className="space-y-4 pt-2">
          <h3 className="font-medium text-stone-900 flex items-center gap-2">
            <Clock className="w-5 h-5 text-stone-500" />
            마감일 (선택)
          </h3>

          <div className="space-y-2">
            <Label className="text-sm text-stone-600">언제까지 투표를 받을까요?</Label>
            <div className="relative">
              <Clock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400 pointer-events-none z-10" />
              <Input
                type="datetime-local"
                value={deadline}
                onChange={(e) => setDeadline(e.target.value)}
                className="h-12 bg-stone-50 border-stone-200 rounded-xl pl-11 [&::-webkit-datetime-edit-text]:opacity-0 [&::-webkit-datetime-edit-month-field]:opacity-0 [&::-webkit-datetime-edit-day-field]:opacity-0 [&::-webkit-datetime-edit-year-field]:opacity-0 [&::-webkit-datetime-edit-hour-field]:opacity-0 [&::-webkit-datetime-edit-minute-field]:opacity-0 [&::-webkit-datetime-edit-second-field]:opacity-0 [&::-webkit-datetime-edit-ampm-field]:opacity-0 [&::-webkit-calendar-picker-indicator]:opacity-100 [&::-webkit-calendar-picker-indicator]:cursor-pointer [&::-webkit-calendar-picker-indicator]:relative [&::-webkit-calendar-picker-indicator]:z-20"
                style={{ color: deadline ? 'inherit' : 'transparent' }}
              />
            </div>
            <p className="text-xs text-stone-500 pl-1">
              마감일을 설정하지 않으면 수동으로 종료할 때까지 투표가 진행됩니다.
            </p>
          </div>
        </div>

        {/* 안내 메시지 */}
        <div className="bg-blue-50 p-4 rounded-xl border border-blue-100 flex gap-3">
          <AlertCircle className="w-5 h-5 text-blue-600 shrink-0 mt-0.5" />
          <div className="text-sm text-blue-800">
            <p className="font-medium mb-1">독립적인 투표입니다</p>
            <p className="text-blue-700/80">이 투표는 일정과 무관하게 생성되며, 모임의 다양한 의사결정에 활용할 수 있습니다.</p>
          </div>
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
