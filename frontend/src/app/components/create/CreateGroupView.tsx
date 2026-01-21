import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Camera, Check, Info, Globe, Lock } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { RadioGroup, RadioGroupItem } from '../ui/radio-group';
import { Switch } from '../ui/switch';
import { Separator } from '../ui/separator';
import { Card, CardContent } from '../ui/card';
import { createClub, ClubCreateRequest } from '@/api/club-full';

// 백엔드 Category enum과 일치
type CategoryType = 'STUDY' | 'SPORTS' | 'SOCIAL' | 'HOBBY' | 'FINANCE' | 'ETC';

const CATEGORY_LABELS: Record<CategoryType, string> = {
  STUDY: '스터디',
  SPORTS: '운동',
  SOCIAL: '친목',
  HOBBY: '취미',
  FINANCE: '재테크',
  ETC: '기타',
};

export function CreateGroupView() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form State
  const [name, setName] = useState('');
  const [category, setCategory] = useState<CategoryType>('SOCIAL');
  const [isPublic, setIsPublic] = useState(true);
  const [postsPublic, setPostsPublic] = useState(false);
  const [image, setImage] = useState<string | null>(null);

  const handleNext = () => {
    if (step === 1) {
      if (!name.trim()) {
        toast.error('모임 이름을 입력해주세요');
        return;
      }
      setStep(2);
    }
  };

  const handleBack = () => {
    if (step === 1) {
      navigate(-1);
    } else {
      setStep(step - 1);
    }
  };

  const handleImageUpload = () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (file) {
        // 파일 크기 체크 (5MB 제한)
        if (file.size > 5 * 1024 * 1024) {
          toast.error('이미지 크기는 5MB 이하여야 합니다');
          return;
        }
        
        const reader = new FileReader();
        reader.onload = (event) => {
          const result = event.target?.result as string;
          setImage(result);
        };
        reader.readAsDataURL(file);
      }
    };
    input.click();
  };

  const removeImage = () => {
    setImage(null);
  };

  const handleSubmit = async () => {
    if (isSubmitting) return;

    try {
      setIsSubmitting(true);

      const request: ClubCreateRequest = {
        clubName: name.trim(),
        visibility: isPublic ? 'PUBLIC' : 'PRIVATE', // 모임 공개/비공개
        type: 'FAIR_SETTLEMENT', // 공정정산형으로 통합
        maxMembers: 100, // 자동으로 100명
        category: category,
      };

      console.log('Creating club with request:', request);

      const response = await createClub(request);
      
      // 모임 이미지를 로컬 스토리지에 저장
      if (image) {
        localStorage.setItem(`club_image_${response.clubId}`, image);
      }
      
      toast.success('모임이 생성되었습니다!');
      console.log('Created club:', response);

      // 생성된 모임으로 이동
      navigate(`/group/${response.clubId}`);
    } catch (error: any) {
      console.error('모임 생성 실패:', error);
      toast.error(error.message || '모임 생성에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-white pb-20">
      {/* Header */}
      <header className="flex items-center p-4 border-b border-stone-100 sticky top-0 bg-white z-10">
        <Button variant="ghost" size="icon" onClick={handleBack} className="-ml-2">
          <ArrowLeft className="w-5 h-5 text-stone-600" />
        </Button>
        <h1 className="text-lg font-semibold ml-2 text-stone-800">모임 만들기 ({step}/2)</h1>
      </header>

      {/* Progress Bar */}
      <div className="h-1 bg-stone-100">
        <div
          className="h-full bg-orange-500 transition-all duration-300"
          style={{ width: `${(step / 2) * 100}%` }}
        />
      </div>

      <div className="p-6 space-y-8">
        {/* Step 1: Basic Info */}
        {step === 1 && (
          <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
            {/* Name */}
            <div className="space-y-2">
              <Label htmlFor="name" className="text-base font-medium">모임 이름 <span className="text-red-500">*</span></Label>
              <Input
                id="name"
                placeholder="멋진 모임 이름을 입력하세요"
                className="h-12 text-lg bg-stone-50 border-stone-200 focus-visible:ring-orange-500 rounded-xl"
                value={name}
                onChange={(e) => setName(e.target.value)}
                maxLength={20}
              />
              <p className="text-xs text-stone-500">{name.length}/20자</p>
            </div>

            {/* Category */}
            <div className="space-y-3">
              <Label className="text-base font-semibold text-stone-900">카테고리</Label>
              <RadioGroup value={category} onValueChange={(v) => setCategory(v as CategoryType)} className="grid grid-cols-2 gap-3">
                {(Object.keys(CATEGORY_LABELS) as CategoryType[]).map((cat) => (
                  <div
                    key={cat}
                    className={`flex items-center space-x-2 border p-3 rounded-xl transition-all ${category === cat ? 'border-orange-500 bg-orange-50' : 'border-stone-200'}`}
                  >
                    <RadioGroupItem value={cat} id={cat} className="text-orange-500" />
                    <Label htmlFor={cat} className="flex-1 cursor-pointer font-medium text-stone-900">
                      {CATEGORY_LABELS[cat]}
                    </Label>
                  </div>
                ))}
              </RadioGroup>
            </div>

            {/* Image Upload */}
            <div className="flex justify-center py-4">
              {image ? (
                <div className="relative">
                  <div className="w-28 h-28 rounded-full overflow-hidden border-4 border-orange-100">
                    <img src={image} alt="모임 대표 이미지" className="w-full h-full object-cover" />
                  </div>
                  <button
                    onClick={removeImage}
                    className="absolute -top-1 -right-1 w-6 h-6 bg-red-500 rounded-full flex items-center justify-center text-white hover:bg-red-600 transition-colors"
                  >
                    <span className="text-xs">×</span>
                  </button>
                </div>
              ) : (
                <button
                  onClick={handleImageUpload}
                  className="w-28 h-28 bg-stone-100 rounded-full flex flex-col items-center justify-center border-2 border-dashed border-stone-300 text-stone-400 cursor-pointer hover:bg-stone-50 hover:border-orange-300 transition-colors"
                >
                  <Camera className="w-8 h-8 mb-1" />
                  <span className="text-xs">대표 이미지</span>
                </button>
              )}
            </div>

            <Button onClick={handleNext} className="w-full h-12 text-lg bg-stone-900 hover:bg-stone-800 mt-8 rounded-xl">
              다음
            </Button>
          </div>
        )}

        {/* Step 2: Privacy Settings */}
        {step === 2 && (
          <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
            <div className="text-center mb-8">
              <div className="w-16 h-16 bg-orange-100 rounded-full flex items-center justify-center mx-auto mb-4">
                {isPublic ? <Globe className="w-8 h-8 text-orange-600" /> : <Lock className="w-8 h-8 text-orange-600" />}
              </div>
              <h2 className="text-xl font-bold text-stone-900">공개 설정</h2>
              <p className="text-sm text-stone-500 mt-2">
                모임의 공개 범위를 설정해주세요
              </p>
            </div>

            {/* Public/Private Switch */}
            <div className="space-y-6">
              <div className="flex items-center justify-between p-4 border border-stone-200 rounded-xl">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                    {isPublic ? <Globe className="w-5 h-5 text-blue-600" /> : <Lock className="w-5 h-5 text-blue-600" />}
                  </div>
                  <div className="space-y-0.5">
                    <Label className="text-base text-stone-900">모임 공개</Label>
                    <div className="text-sm text-stone-500">
                      {isPublic ? '누구나 모임을 검색하고 참여 신청 가능' : '초대 코드로만 참여 가능'}
                    </div>
                  </div>
                </div>
                <Switch checked={isPublic} onCheckedChange={setIsPublic} className="data-[state=checked]:bg-orange-500" />
              </div>

              {/* Posts Public Switch */}
              <div className="flex items-center justify-between p-4 border border-stone-200 rounded-xl">
                <div className="space-y-0.5">
                  <Label className="text-base text-stone-900">게시글 공개</Label>
                  <div className="text-sm text-stone-500">비회원도 게시글을 볼 수 있습니다</div>
                </div>
                <Switch checked={postsPublic} onCheckedChange={setPostsPublic} className="data-[state=checked]:bg-orange-500" />
              </div>
            </div>

            {/* Info Box */}
            <Card className="border-blue-200 bg-blue-50">
              <CardContent className="p-4">
                <div className="flex gap-3">
                  <Info className="w-5 h-5 text-blue-600 shrink-0 mt-0.5" />
                  <div className="text-sm text-blue-800">
                    <p className="font-medium mb-1">자동 설정 안내</p>
                    <ul className="text-blue-700 space-y-1">
                      <li>• 최대 인원: 100명</li>
                      <li>• 통장 관리: 공정정산형</li>
                      <li>• 검색 허용: {isPublic ? '공개' : '비공개'}에 따라 자동 설정</li>
                    </ul>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Button
              onClick={handleSubmit}
              disabled={isSubmitting}
              className="w-full h-12 text-lg bg-orange-500 hover:bg-orange-600 mt-8 rounded-xl shadow-lg shadow-orange-200 text-white disabled:opacity-50"
            >
              <Check className="w-5 h-5 mr-2" />
              {isSubmitting ? '생성 중...' : '모임 만들기 완료'}
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
