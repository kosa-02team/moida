import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Globe, Lock, Info } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Card, CardContent } from '../../ui/card';
import { RadioGroup, RadioGroupItem } from '../../ui/radio-group';
import { updateClub, getClub, ClubDetailResponse } from '../../../../api/club-full';

export function GroupPrivacySettingsView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const [isLoading, setIsLoading] = useState(false);
  const [clubData, setClubData] = useState<ClubDetailResponse | null>(null);

  // 공개 설정 상태 - 백엔드는 PUBLIC/PRIVATE만 지원
  const [visibility, setVisibility] = useState<'PUBLIC' | 'PRIVATE'>('PUBLIC');

  const visibilityOptions = [
    {
      value: 'PRIVATE',
      label: '비공개',
      description: '초대 코드로만 가입 가능, 멤버만 상세 정보 조회 가능',
      icon: Lock,
    },
    {
      value: 'PUBLIC',
      label: '공개',
      description: '누구나 모임 정보를 볼 수 있음',
      icon: Globe,
    },
  ];

  useEffect(() => {
    async function fetchClubData() {
      if (!groupId) return;
      try {
        const data = await getClub(Number(groupId));
        setClubData(data);
        // visibility 매핑: PRIVATE -> PRIVATE, PUBLIC -> PUBLIC
        if (data.visibility === 'PRIVATE') {
          setVisibility('PRIVATE');
        } else {
          setVisibility('PUBLIC');
        }
      } catch (error) {
        console.error('모임 정보 조회 실패:', error);
        toast.error('모임 정보를 불러올 수 없습니다');
      }
    }
    fetchClubData();
  }, [groupId]);

  const handleSave = async () => {
    if (!groupId || !clubData) {
      toast.error('모임 ID를 찾을 수 없습니다');
      return;
    }
    
    setIsLoading(true);
    try {
      // 백엔드 ClubRequest는 clubName이 필수이므로 현재 클럽 이름을 함께 전송
      await updateClub(Number(groupId), {
        clubName: clubData.clubName,
        visibility: visibility as 'PUBLIC' | 'PRIVATE'
      });
      toast.success('공개 설정이 저장되었습니다');
      navigate(-1);
    } catch (error: any) {
      console.error('공개 설정 저장 실패:', error);
      const errorMessage = error?.response?.data?.message || error?.message || '공개 설정 저장에 실패했습니다';
      toast.error(errorMessage);
    } finally {
      setIsLoading(false);
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
          
          <RadioGroup value={visibility} onValueChange={(v) => setVisibility(v as 'PUBLIC' | 'PRIVATE')}>
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


        {/* Info */}
        <Card className="border-blue-200 bg-blue-50">
          <CardContent className="p-4">
            <div className="flex items-start gap-3">
              <Info className="w-5 h-5 text-blue-600 mt-0.5" />
              <div className="space-y-2">
                <p className="text-sm font-medium text-blue-800">가입 및 초대 코드 안내</p>
                <ul className="text-xs text-blue-700 space-y-1 list-disc list-inside">
                  <li>가입 신청 시 자동으로 승인 대기 상태로 전환됩니다.</li>
                  <li>초대 코드는 모임 생성 시 자동으로 발급되며, 모임 관리 페이지에서 재발급할 수 있습니다.</li>
                  <li>회비 현황 및 관련 정보는 설정과 관계없이 모임 회원에게만 공개됩니다.</li>
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}



