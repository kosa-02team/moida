import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, Eye, EyeOff, Mail, Lock, User, Check } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Checkbox } from '../ui/checkbox';
import { signup } from '@/api/auth';

export function SignUpView() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [agreePrivacy, setAgreePrivacy] = useState(false);
  const [agreeMarketing, setAgreeMarketing] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const passwordRequirements = [
    { label: '8자 이상', met: password.length >= 8 },
    { label: '영문 포함', met: /[a-zA-Z]/.test(password) },
    { label: '숫자 포함', met: /\d/.test(password) },
    { label: '특수문자 포함', met: /[!@#$%^&*]/.test(password) },
  ];

  // 이름 검증: 2-10글자 한글 이름
  const isValidName = /^[가-힣]{2,10}$/.test(name);
  // 이메일 형식 검증
  const isValidEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  const canProceed = step === 1 
    ? isValidName && isValidEmail && password && confirmPassword && password === confirmPassword && passwordRequirements.every(r => r.met)
    : agreeTerms && agreePrivacy;

  const handleNext = () => {
    if (step === 1 && password !== confirmPassword) {
      toast.error('비밀번호가 일치하지 않습니다');
      return;
    }
    setStep(2);
  };

  const handleSignUp = async () => {
    if (!canProceed) return;

    setIsLoading(true);
    try {
      await signup({
        loginId: email.trim(),
        password: password.trim(),
        realName: name.trim(),
      });
      toast.success('회원가입이 완료되었습니다!');
      navigate('/login');
    } catch (error: any) {
      console.error('회원가입 실패:', error);
      if (error?.message?.includes('이미 존재')) {
        toast.error('이미 가입된 이메일입니다.');
      } else if (error?.message?.includes('이름')) {
        toast.error('이름은 2-10글자 한글만 가능합니다.');
      } else {
        toast.error(error?.message || '회원가입에 실패했습니다.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleAgreeAll = (checked: boolean) => {
    setAgreeTerms(checked);
    setAgreePrivacy(checked);
    setAgreeMarketing(checked);
  };

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center px-4 py-3">
          <Button 
            variant="ghost" 
            size="icon" 
            onClick={() => step === 1 ? navigate(-1) : setStep(1)} 
            className="-ml-2"
          >
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="ml-2 text-lg font-semibold text-stone-800">회원가입</h1>
        </div>
        {/* Progress Bar */}
        <div className="h-1 bg-stone-100">
          <div 
            className="h-full bg-orange-500 transition-all duration-300"
            style={{ width: step === 1 ? '50%' : '100%' }}
          />
        </div>
      </header>

      <div className="p-6">
        {step === 1 && (
          <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
            <div className="space-y-2">
              <h2 className="text-2xl font-bold text-stone-900">계정 정보 입력</h2>
              <p className="text-stone-500">모임 관리를 시작하기 위해 정보를 입력해주세요.</p>
            </div>

            {/* Name */}
            <div className="space-y-2">
              <Label htmlFor="name">이름</Label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
                <Input
                  id="name"
                  placeholder="이름을 입력하세요 (한글 2-10자)"
                  className={`pl-10 h-12 bg-stone-50 border-stone-200 rounded-xl ${
                    name && !isValidName ? 'border-red-500' : ''
                  }`}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>
              {name && !isValidName && (
                <p className="text-xs text-red-500">이름은 2-10글자 한글만 입력 가능합니다</p>
              )}
            </div>

            {/* Email */}
            <div className="space-y-2">
              <Label htmlFor="email">이메일 (로그인 ID로 사용됩니다)</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
                <Input
                  id="email"
                  type="email"
                  placeholder="example@email.com"
                  className={`pl-10 h-12 bg-stone-50 border-stone-200 rounded-xl ${
                    email && !isValidEmail ? 'border-red-500' : ''
                  }`}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>
              {email && !isValidEmail && (
                <p className="text-xs text-red-500">올바른 이메일 형식을 입력해주세요</p>
              )}
            </div>

            {/* Password */}
            <div className="space-y-2">
              <Label htmlFor="password">비밀번호</Label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
                <Input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="비밀번호를 입력하세요"
                  className="pl-10 pr-10 h-12 bg-stone-50 border-stone-200 rounded-xl"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-stone-400 hover:text-stone-600"
                >
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
              {/* Password Requirements */}
              <div className="flex flex-wrap gap-2 mt-2">
                {passwordRequirements.map((req) => (
                  <span
                    key={req.label}
                    className={`text-xs px-2 py-1 rounded-full ${
                      req.met 
                        ? 'bg-green-100 text-green-700' 
                        : 'bg-stone-100 text-stone-500'
                    }`}
                  >
                    {req.met && <Check className="w-3 h-3 inline mr-1" />}
                    {req.label}
                  </span>
                ))}
              </div>
            </div>

            {/* Confirm Password */}
            <div className="space-y-2">
              <Label htmlFor="confirmPassword">비밀번호 확인</Label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
                <Input
                  id="confirmPassword"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="비밀번호를 다시 입력하세요"
                  className={`pl-10 h-12 bg-stone-50 border-stone-200 rounded-xl ${
                    confirmPassword && password !== confirmPassword ? 'border-red-500' : ''
                  }`}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                />
              </div>
              {confirmPassword && password !== confirmPassword && (
                <p className="text-xs text-red-500">비밀번호가 일치하지 않습니다</p>
              )}
            </div>

            <Button
              onClick={handleNext}
              disabled={!canProceed}
              className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl disabled:opacity-50"
            >
              다음
            </Button>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
            <div className="space-y-2">
              <h2 className="text-2xl font-bold text-stone-900">약관 동의</h2>
              <p className="text-stone-500">서비스 이용을 위해 약관에 동의해주세요.</p>
            </div>

            <div className="space-y-4">
              {/* Agree All */}
              <div className="flex items-center gap-3 p-4 bg-stone-50 rounded-xl">
                <Checkbox
                  id="agreeAll"
                  checked={agreeTerms && agreePrivacy && agreeMarketing}
                  onCheckedChange={handleAgreeAll}
                  className="w-6 h-6 data-[state=checked]:bg-orange-500 data-[state=checked]:border-orange-500"
                />
                <Label htmlFor="agreeAll" className="text-base font-semibold cursor-pointer">
                  전체 동의
                </Label>
              </div>

              <div className="space-y-3 pl-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Checkbox
                      id="agreeTerms"
                      checked={agreeTerms}
                      onCheckedChange={(checked) => setAgreeTerms(checked as boolean)}
                      className="data-[state=checked]:bg-orange-500 data-[state=checked]:border-orange-500"
                    />
                    <Label htmlFor="agreeTerms" className="text-sm cursor-pointer">
                      <span className="text-orange-500">[필수]</span> 이용약관 동의
                    </Label>
                  </div>
                  <button className="text-xs text-stone-500 underline">보기</button>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Checkbox
                      id="agreePrivacy"
                      checked={agreePrivacy}
                      onCheckedChange={(checked) => setAgreePrivacy(checked as boolean)}
                      className="data-[state=checked]:bg-orange-500 data-[state=checked]:border-orange-500"
                    />
                    <Label htmlFor="agreePrivacy" className="text-sm cursor-pointer">
                      <span className="text-orange-500">[필수]</span> 개인정보 처리방침 동의
                    </Label>
                  </div>
                  <button className="text-xs text-stone-500 underline">보기</button>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Checkbox
                      id="agreeMarketing"
                      checked={agreeMarketing}
                      onCheckedChange={(checked) => setAgreeMarketing(checked as boolean)}
                      className="data-[state=checked]:bg-orange-500 data-[state=checked]:border-orange-500"
                    />
                    <Label htmlFor="agreeMarketing" className="text-sm cursor-pointer">
                      <span className="text-stone-400">[선택]</span> 마케팅 정보 수신 동의
                    </Label>
                  </div>
                  <button className="text-xs text-stone-500 underline">보기</button>
                </div>
              </div>
            </div>

            <Button
              onClick={handleSignUp}
              disabled={!canProceed || isLoading}
              className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl disabled:opacity-50"
            >
              {isLoading ? '가입 중...' : '가입 완료'}
            </Button>
          </div>
        )}

        {/* Login Link */}
        <p className="mt-8 text-center text-stone-600">
          이미 계정이 있으신가요?{' '}
          <Link to="/login" className="text-orange-600 hover:text-orange-700 font-medium">
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}



