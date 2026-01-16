import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Camera, Check, Wallet, Scale, Info, Users, Lock, Globe } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { RadioGroup, RadioGroupItem } from '../ui/radio-group';
import { Switch } from '../ui/switch';
import { Badge } from '../ui/badge';
import { Separator } from '../ui/separator';
import { Card, CardContent } from '../ui/card';

export function CreateGroupView() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  
  // Form State - Step 1
  const [type, setType] = useState('club');
  const [isPublic, setIsPublic] = useState(true);
  const [allowSearch, setAllowSearch] = useState(true);
  const [postsPublic, setPostsPublic] = useState(false); // 비회원 게시글 공개
  
  // Form State - Step 2
  const [name, setName] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState('');
  const [maxMembers, setMaxMembers] = useState(20);

  // Form State - Step 3 (Account Management)
  const [managementType, setManagementType] = useState<'operating' | 'fair'>('operating');

  const handleAddTag = () => {
    if (tagInput.trim() && !tags.includes(tagInput.trim())) {
      setTags([...tags, tagInput.trim()]);
      setTagInput('');
    }
  };

  const handleNext = () => {
    if (step === 1) {
      setStep(2);
    } else if (step === 2) {
      if (!name.trim()) {
        toast.error('모임 이름을 입력해주세요');
        return;
      }
      setStep(3);
    }
  };

  const handleBack = () => {
    if (step === 1) {
      navigate(-1);
    } else {
      setStep(step - 1);
    }
  };
  
  const handleSubmit = () => {
    toast.success('모임이 생성되었습니다!');
    // Logic to create group would go here
    console.log({ 
      type, 
      isPublic, 
      allowSearch, 
      postsPublic,
      name, 
      tags, 
      maxMembers,
      managementType 
    });
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-white pb-20">
      {/* Header */}
      <header className="flex items-center p-4 border-b border-stone-100 sticky top-0 bg-white z-10">
        <Button variant="ghost" size="icon" onClick={handleBack} className="-ml-2">
          <ArrowLeft className="w-5 h-5 text-stone-600" />
        </Button>
        <h1 className="text-lg font-semibold ml-2 text-stone-800">모임 만들기 ({step}/3)</h1>
      </header>

      {/* Progress Bar */}
      <div className="h-1 bg-stone-100">
        <div 
          className="h-full bg-orange-500 transition-all duration-300"
          style={{ width: `${(step / 3) * 100}%` }}
        />
      </div>

      <div className="p-6 space-y-8">
        {/* Step 1: Type & Privacy */}
        {step === 1 && (
          <div className="space-y-8 animate-in fade-in slide-in-from-right-4 duration-300">
            {/* Type Selection */}
            <div className="space-y-4">
              <Label className="text-base font-semibold text-stone-900">어떤 모임인가요?</Label>
              <RadioGroup value={type} onValueChange={setType} className="grid grid-cols-1 gap-3">
                <div className={`flex items-center space-x-3 border p-4 rounded-xl transition-all ${type === 'club' ? 'border-orange-500 bg-orange-50' : 'border-stone-200'}`}>
                  <RadioGroupItem value="club" id="club" className="text-orange-500" />
                  <Label htmlFor="club" className="flex-1 cursor-pointer">
                    <div className="font-medium text-stone-900">동아리</div>
                    <div className="text-sm text-stone-500">취미, 운동 등 지속적인 활동</div>
                  </Label>
                </div>
                <div className={`flex items-center space-x-3 border p-4 rounded-xl transition-all ${type === 'meetup' ? 'border-orange-500 bg-orange-50' : 'border-stone-200'}`}>
                  <RadioGroupItem value="meetup" id="meetup" className="text-orange-500" />
                  <Label htmlFor="meetup" className="flex-1 cursor-pointer">
                    <div className="font-medium text-stone-900">정모 / 번개</div>
                    <div className="text-sm text-stone-500">일회성 만남, 친목 도모</div>
                  </Label>
                </div>
                <div className={`flex items-center space-x-3 border p-4 rounded-xl transition-all ${type === 'study' ? 'border-orange-500 bg-orange-50' : 'border-stone-200'}`}>
                  <RadioGroupItem value="study" id="study" className="text-orange-500" />
                  <Label htmlFor="study" className="flex-1 cursor-pointer">
                    <div className="font-medium text-stone-900">스터디</div>
                    <div className="text-sm text-stone-500">자기계발, 학습 목표 달성</div>
                  </Label>
                </div>
              </RadioGroup>
            </div>

            <Separator />

            {/* Public/Private & Search */}
            <div className="space-y-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                    {isPublic ? <Globe className="w-5 h-5 text-blue-600" /> : <Lock className="w-5 h-5 text-blue-600" />}
                  </div>
                  <div className="space-y-0.5">
                    <Label className="text-base text-stone-900">공개 모임</Label>
                    <div className="text-sm text-stone-500">누구나 모임을 검색하고 참여 신청 가능</div>
                  </div>
                </div>
                <Switch checked={isPublic} onCheckedChange={setIsPublic} className="data-[state=checked]:bg-orange-500" />
              </div>
              
              {isPublic && (
                <>
                  <div className="flex items-center justify-between ml-13">
                    <div className="space-y-0.5">
                      <Label className="text-base text-stone-900">검색 허용</Label>
                      <div className="text-sm text-stone-500">모임 탐색에 노출됩니다</div>
                    </div>
                    <Switch checked={allowSearch} onCheckedChange={setAllowSearch} className="data-[state=checked]:bg-orange-500" />
                  </div>

                  <div className="flex items-center justify-between ml-13">
                    <div className="space-y-0.5">
                      <Label className="text-base text-stone-900">게시글 공개</Label>
                      <div className="text-sm text-stone-500">비회원도 게시글을 볼 수 있습니다</div>
                    </div>
                    <Switch checked={postsPublic} onCheckedChange={setPostsPublic} className="data-[state=checked]:bg-orange-500" />
                  </div>
                </>
              )}
            </div>

            <Button onClick={handleNext} className="w-full h-12 text-lg bg-stone-900 hover:bg-stone-800 mt-8 rounded-xl">
              다음
            </Button>
          </div>
        )}

        {/* Step 2: Basic Info */}
        {step === 2 && (
          <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
            {/* Image Upload */}
            <div className="flex justify-center py-4">
              <div className="w-28 h-28 bg-stone-100 rounded-full flex flex-col items-center justify-center border-2 border-dashed border-stone-300 text-stone-400 cursor-pointer hover:bg-stone-50 hover:border-orange-300 transition-colors">
                <Camera className="w-8 h-8 mb-1" />
                <span className="text-xs">대표 이미지</span>
              </div>
            </div>

            {/* Name */}
            <div className="space-y-2">
              <Label htmlFor="name" className="text-base font-medium">모임 이름 <span className="text-red-500">*</span></Label>
              <Input 
                id="name" 
                placeholder="멋진 모임 이름을 입력하세요" 
                className="h-12 text-lg bg-stone-50 border-stone-200 focus-visible:ring-orange-500 rounded-xl"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </div>

            {/* Tags */}
            <div className="space-y-2">
              <Label className="text-base font-medium">관심사 태그</Label>
              <div className="flex gap-2 mb-2 flex-wrap">
                {tags.map(tag => (
                  <Badge key={tag} variant="secondary" className="px-3 py-1 text-sm rounded-full flex items-center gap-1 bg-orange-100 text-orange-700 hover:bg-orange-200">
                    #{tag}
                    <button onClick={() => setTags(tags.filter(t => t !== tag))} className="ml-1 hover:text-orange-900">×</button>
                  </Badge>
                ))}
              </div>
              <div className="flex gap-2">
                <Input 
                  placeholder="태그 입력 (예: 맛집)" 
                  value={tagInput}
                  onChange={(e) => setTagInput(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), handleAddTag())}
                  className="rounded-xl"
                />
                <Button variant="outline" onClick={handleAddTag} className="rounded-xl border-stone-300">추가</Button>
              </div>
            </div>

            {/* Max Members */}
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <Label className="text-base font-medium">최대 인원</Label>
                <span className="text-lg font-bold text-orange-600">{maxMembers}명</span>
              </div>
              <input 
                type="range" 
                min="2" 
                max="100" 
                value={maxMembers} 
                onChange={(e) => setMaxMembers(Number(e.target.value))}
                className="w-full accent-orange-500 h-2 bg-stone-200 rounded-lg appearance-none cursor-pointer"
              />
              <p className="text-xs text-stone-500">
                * 인원이 가득 차면 더 이상 참여할 수 없습니다.
              </p>
            </div>

            <Button onClick={handleNext} className="w-full h-12 text-lg bg-stone-900 hover:bg-stone-800 mt-8 rounded-xl">
              다음
            </Button>
          </div>
        )}

        {/* Step 3: Account Management Type */}
        {step === 3 && (
          <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
            <div className="text-center mb-8">
              <div className="w-16 h-16 bg-orange-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Wallet className="w-8 h-8 text-orange-600" />
              </div>
              <h2 className="text-xl font-bold text-stone-900">모임 통장 관리 유형</h2>
              <p className="text-sm text-stone-500 mt-2">
                회비 관리 방식을 선택해주세요<br/>
                나중에 관리 페이지에서 변경할 수 있습니다
              </p>
            </div>

            <RadioGroup value={managementType} onValueChange={(v) => setManagementType(v as 'operating' | 'fair')} className="space-y-4">
              {/* Operating Type */}
              <Card className={`border-2 transition-all cursor-pointer ${managementType === 'operating' ? 'border-blue-500 bg-blue-50' : 'border-stone-200 hover:border-stone-300'}`}>
                <CardContent className="p-4">
                  <div className="flex items-start gap-3">
                    <RadioGroupItem value="operating" id="operating" className="mt-1" />
                    <Label htmlFor="operating" className="flex-1 cursor-pointer">
                      <div className="flex items-center gap-2 mb-2">
                        <Users className="w-5 h-5 text-blue-600" />
                        <span className="font-bold text-stone-900 text-lg">운영비형</span>
                        <Badge variant="secondary" className="bg-blue-100 text-blue-700 text-xs">인원 고정형</Badge>
                      </div>
                      <p className="text-sm text-stone-600 mb-3">
                        인원 변동이 적은 모임에 적합합니다
                      </p>
                      <div className="bg-white rounded-lg p-3 space-y-2 text-sm">
                        <div className="flex items-center gap-2">
                          <span className="w-2 h-2 bg-blue-500 rounded-full"></span>
                          <span className="text-stone-700">탈퇴 시 환불 없음</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="w-2 h-2 bg-blue-500 rounded-full"></span>
                          <span className="text-stone-700">가입 시 모임 규칙에 따른 가입비</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="w-2 h-2 bg-blue-500 rounded-full"></span>
                          <span className="text-stone-700">간단한 운영비 관리</span>
                        </div>
                      </div>
                    </Label>
                  </div>
                </CardContent>
              </Card>

              {/* Fair Settlement Type */}
              <Card className={`border-2 transition-all cursor-pointer ${managementType === 'fair' ? 'border-green-500 bg-green-50' : 'border-stone-200 hover:border-stone-300'}`}>
                <CardContent className="p-4">
                  <div className="flex items-start gap-3">
                    <RadioGroupItem value="fair" id="fair" className="mt-1" />
                    <Label htmlFor="fair" className="flex-1 cursor-pointer">
                      <div className="flex items-center gap-2 mb-2">
                        <Scale className="w-5 h-5 text-green-600" />
                        <span className="font-bold text-stone-900 text-lg">공정정산형</span>
                        <Badge variant="secondary" className="bg-green-100 text-green-700 text-xs">추천</Badge>
                      </div>
                      <p className="text-sm text-stone-600 mb-3">
                        인원 변동이 많은 모임에 적합합니다
                      </p>
                      <div className="bg-white rounded-lg p-3 space-y-2 text-sm">
                        <div className="flex items-center gap-2">
                          <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                          <span className="text-stone-700">탈퇴 시 지분만큼 환불</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                          <span className="text-stone-700">가입 시 동일 지분을 위한 가입비</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                          <span className="text-stone-700">개인별 지분 자동 계산</span>
                        </div>
                      </div>
                    </Label>
                  </div>
                </CardContent>
              </Card>
            </RadioGroup>

            {/* Info Box */}
            <Card className="border-amber-200 bg-amber-50">
              <CardContent className="p-4">
                <div className="flex gap-3">
                  <Info className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                  <div className="text-sm text-amber-800">
                    <p className="font-medium mb-1">왜 이 선택이 중요한가요?</p>
                    <p className="text-amber-700">
                      인원 변동(가입·탈퇴) 시 가입금/환불 기준이 애매해 불공정 논란이 생길 수 있습니다.
                      미리 규칙을 정해두면 분쟁을 예방할 수 있어요.
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Button onClick={handleSubmit} className="w-full h-12 text-lg bg-orange-500 hover:bg-orange-600 mt-8 rounded-xl shadow-lg shadow-orange-200 text-white">
              <Check className="w-5 h-5 mr-2" />
              모임 만들기 완료
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
