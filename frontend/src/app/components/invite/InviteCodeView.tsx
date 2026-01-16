import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, KeyRound, Users } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { InputOTP, InputOTPGroup, InputOTPSlot } from '../ui/input-otp';
import { Input } from '../ui/input';
import { Label } from '../ui/label';

export function InviteCodeView() {
  const navigate = useNavigate();
  const [inviteCode, setInviteCode] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [foundGroup, setFoundGroup] = useState<{
    id: string;
    name: string;
    image: string;
    memberCount: number;
  } | null>(null);

  const handleSearch = () => {
    if (inviteCode.length < 6) {
      toast.error('초대 코드 6자리를 입력해주세요');
      return;
    }

    setIsLoading(true);
    // 실제로는 API 호출
    setTimeout(() => {
      setIsLoading(false);
      // Mock 데이터
      setFoundGroup({
        id: '1',
        name: '주말 등산 클럽',
        image: 'https://images.unsplash.com/photo-1551632811-561732d1e306?w=400&h=400&fit=crop',
        memberCount: 15,
      });
    }, 1000);
  };

  const handleJoin = () => {
    if (!foundGroup) return;
    
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      toast.success('모임에 가입되었습니다!');
      navigate(`/group/${foundGroup.id}`);
    }, 1000);
  };

  const handleReset = () => {
    setFoundGroup(null);
    setInviteCode('');
  };

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center px-4 py-3">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigate(-1)}
            className="-ml-2"
          >
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="ml-2 text-lg font-semibold text-stone-800">초대 코드로 가입</h1>
        </div>
      </header>

      <div className="p-6">
        {!foundGroup ? (
          <div className="space-y-8 animate-in fade-in duration-300">
            {/* Icon */}
            <div className="flex justify-center">
              <div className="w-20 h-20 bg-orange-100 rounded-full flex items-center justify-center">
                <KeyRound className="w-10 h-10 text-orange-500" />
              </div>
            </div>

            {/* Description */}
            <div className="text-center space-y-2">
              <h2 className="text-xl font-bold text-stone-900">초대 코드 입력</h2>
              <p className="text-stone-500">
                모임장에게 받은 6자리 초대 코드를 입력하세요.
              </p>
            </div>

            {/* Code Input */}
            <div className="flex justify-center">
              <InputOTP
                maxLength={6}
                value={inviteCode}
                onChange={setInviteCode}
              >
                <InputOTPGroup className="gap-2">
                  <InputOTPSlot index={0} className="w-12 h-14 text-xl border-stone-200 rounded-lg uppercase" />
                  <InputOTPSlot index={1} className="w-12 h-14 text-xl border-stone-200 rounded-lg uppercase" />
                  <InputOTPSlot index={2} className="w-12 h-14 text-xl border-stone-200 rounded-lg uppercase" />
                  <InputOTPSlot index={3} className="w-12 h-14 text-xl border-stone-200 rounded-lg uppercase" />
                  <InputOTPSlot index={4} className="w-12 h-14 text-xl border-stone-200 rounded-lg uppercase" />
                  <InputOTPSlot index={5} className="w-12 h-14 text-xl border-stone-200 rounded-lg uppercase" />
                </InputOTPGroup>
              </InputOTP>
            </div>

            {/* Or use full code */}
            <div className="space-y-2">
              <Label className="text-center block text-stone-500 text-sm">
                또는 전체 초대 링크 입력
              </Label>
              <Input
                placeholder="https://... 또는 초대코드"
                className="h-12 bg-stone-50 border-stone-200 rounded-xl text-center"
                value={inviteCode.length === 6 ? '' : inviteCode}
                onChange={(e) => {
                  // URL에서 코드 추출 시도
                  const value = e.target.value;
                  const codeMatch = value.match(/[A-Za-z0-9]{6}$/);
                  if (codeMatch) {
                    setInviteCode(codeMatch[0].toUpperCase());
                  }
                }}
              />
            </div>

            {/* Search Button */}
            <Button
              onClick={handleSearch}
              disabled={inviteCode.length < 6 || isLoading}
              className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl disabled:opacity-50"
            >
              {isLoading ? '검색 중...' : '모임 찾기'}
            </Button>
          </div>
        ) : (
          <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-300">
            {/* Found Group */}
            <div className="text-center space-y-2">
              <h2 className="text-xl font-bold text-stone-900">모임을 찾았습니다!</h2>
              <p className="text-stone-500">아래 모임에 가입하시겠습니까?</p>
            </div>

            <div className="bg-stone-50 rounded-2xl p-6 text-center">
              <div className="w-24 h-24 rounded-2xl overflow-hidden mx-auto mb-4">
                <img
                  src={foundGroup.image}
                  alt={foundGroup.name}
                  className="w-full h-full object-cover"
                />
              </div>
              <h3 className="text-xl font-bold text-stone-900 mb-2">
                {foundGroup.name}
              </h3>
              <div className="flex items-center justify-center gap-1 text-stone-500">
                <Users className="w-4 h-4" />
                <span>{foundGroup.memberCount}명의 멤버</span>
              </div>
            </div>

            <div className="space-y-3">
              <Button
                onClick={handleJoin}
                disabled={isLoading}
                className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl"
              >
                {isLoading ? '가입 중...' : '가입하기'}
              </Button>
              <Button
                variant="outline"
                onClick={handleReset}
                className="w-full h-12 rounded-xl"
              >
                다른 코드 입력
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}



