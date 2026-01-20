import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Globe, Lock, Search, Eye, Users, KeyRound, Info } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Switch } from '../../ui/switch';
import { Label } from '../../ui/label';
import { Card, CardContent } from '../../ui/card';
import { RadioGroup, RadioGroupItem } from '../../ui/radio-group';
import { GroupVisibility } from '../../../types';

export function GroupPrivacySettingsView() {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);

  // 공개 설정 상태 - 통합된 3가지 옵션만 사용
  const [visibility, setVisibility] = useState<GroupVisibility>('searchable');
  const [requireApproval, setRequireApproval] = useState(true);
  const [allowInviteCode, setAllowInviteCode] = useState(true);

  const visibilityOptions = [
    {
      value: 'private',
      label: '비공개',
      description: '초대 코드로만 가입 가능',
      icon: Lock,
    },
    {
      value: 'searchable',
      label: '검색 허용',
      description: '검색 결과에 표시, 상세 정보는 비공개',
      icon: Search,
    },
    {
      value: 'public',
      label: '완전 공개',
      description: '누구나 모임 정보를 볼 수 있음',
      icon: Globe,
    },
  ];

  const handleSave = async () => {
    setIsLoading(true);
    // TODO: API 호출 - updateClub with visibility
    // const { groupId } = useParams();
    // await updateClub(Number(groupId), { visibility: visibility === 'private' ? 'PRIVATE' : visibility === 'public' ? 'PUBLIC' : 'SEARCHABLE' });
    setTimeout(() => {
      setIsLoading(false);
      toast.success('공개 설정이 저장되었습니다');
      navigate(-1);
    }, 1000);
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
            <h1 className="ml-2 text-lg font-semibold text-stone-800">공개 설정</h1>
          </div>
          <Button
            onClick={handleSave}
            disabled={isLoading}
            className="bg-orange-500 hover:bg-orange-600 text-white rounded-full px-4"
          >
            {isLoading ? '저장 중...' : '저장'}
          </Button>
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Visibility */}
        <div className="bg-white rounded-2xl p-5 border border-stone-100 space-y-4">
          <h3 className="font-bold text-stone-900">모임 공개 범위</h3>
          
          <RadioGroup value={visibility} onValueChange={(v) => setVisibility(v as GroupVisibility)}>
            <div className="space-y-3">
              {visibilityOptions.map((option) => (
                <label
                  key={option.value}
                  className={`flex items-start gap-3 p-4 rounded-xl border-2 cursor-pointer transition-colors ${
                    visibility === option.value
                      ? 'border-orange-500 bg-orange-50'
                      : 'border-stone-100 hover:border-stone-200'
                  }`}
                >
                  <RadioGroupItem 
                    value={option.value} 
                    className="mt-1 data-[state=checked]:border-orange-500 data-[state=checked]:bg-orange-500" 
                  />
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <option.icon className={`w-4 h-4 ${
                        visibility === option.value ? 'text-orange-600' : 'text-stone-500'
                      }`} />
                      <p className="font-medium text-stone-900">{option.label}</p>
                    </div>
                    <p className="text-sm text-stone-500 mt-1">{option.description}</p>
                  </div>
                </label>
              ))}
            </div>
          </RadioGroup>
        </div>

        {/* 완전 공개일 때만 게시글/멤버 목록 공개 옵션 표시 */}
        {visibility === 'public' && (
          <div className="bg-white rounded-2xl border border-stone-100 divide-y divide-stone-100">
            <div className="px-4 py-3 bg-stone-50">
              <h3 className="font-medium text-stone-700">완전 공개 설정</h3>
            </div>

            {/* Show Posts - 완전 공개일 때만 활성화 */}
            <div className="p-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
                  <Eye className="w-5 h-5 text-purple-600" />
                </div>
                <div>
                  <p className="font-medium text-stone-900">게시글 공개</p>
                  <p className="text-xs text-stone-500">비회원에게 게시글 표시</p>
                </div>
              </div>
              <Switch
                defaultChecked
                className="data-[state=checked]:bg-orange-500"
              />
            </div>

            {/* Show Members - 완전 공개일 때만 활성화 */}
            <div className="p-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                  <Users className="w-5 h-5 text-green-600" />
                </div>
                <div>
                  <p className="font-medium text-stone-900">멤버 목록 공개</p>
                  <p className="text-xs text-stone-500">비회원에게 멤버 목록 표시</p>
                </div>
              </div>
              <Switch
                defaultChecked
                className="data-[state=checked]:bg-orange-500"
              />
            </div>
          </div>
        )}

        {/* Join Settings */}
        <div className="bg-white rounded-2xl border border-stone-100 divide-y divide-stone-100">
          <div className="px-4 py-3 bg-stone-50">
            <h3 className="font-medium text-stone-700">가입 설정</h3>
          </div>

          {/* Require Approval */}
          <div className="p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center">
                <Users className="w-5 h-5 text-orange-600" />
              </div>
              <div>
                <p className="font-medium text-stone-900">가입 승인 필요</p>
                <p className="text-xs text-stone-500">관리자 승인 후 가입</p>
              </div>
            </div>
            <Switch
              checked={requireApproval}
              onCheckedChange={setRequireApproval}
              className="data-[state=checked]:bg-orange-500"
            />
          </div>

          {/* Invite Code */}
          <div className="p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-stone-100 rounded-lg flex items-center justify-center">
                <KeyRound className="w-5 h-5 text-stone-600" />
              </div>
              <div>
                <p className="font-medium text-stone-900">초대 코드 허용</p>
                <p className="text-xs text-stone-500">초대 코드로 직접 가입 가능</p>
              </div>
            </div>
            <Switch
              checked={allowInviteCode}
              onCheckedChange={setAllowInviteCode}
              className="data-[state=checked]:bg-orange-500"
            />
          </div>
        </div>

        {/* Info */}
        <Card className="border-blue-200 bg-blue-50">
          <CardContent className="p-4">
            <div className="flex items-start gap-3">
              <Info className="w-5 h-5 text-blue-600 mt-0.5" />
              <div>
                <p className="text-sm font-medium text-blue-800">회비 현황은 항상 비공개</p>
                <p className="text-xs text-blue-700 mt-1">
                  회비 현황 및 관련 정보는 설정과 관계없이 모임 회원에게만 공개됩니다.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}



