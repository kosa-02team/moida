import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, Mail, KeyRound, Check } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { InputOTP, InputOTPGroup, InputOTPSlot } from '../ui/input-otp';

export function ForgotPasswordView() {
  const navigate = useNavigate();
  const [step, setStep] = useState<'email' | 'verify' | 'reset' | 'complete'>('email');
  const [email, setEmail] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSendCode = async () => {
    if (!email) {
      toast.error('이메일을 입력해주세요');
      return;
    }

    setIsLoading(true);
    // 실제로는 API 호출
    setTimeout(() => {
      setIsLoading(false);
      toast.success('인증번호가 발송되었습니다');
      setStep('verify');
    }, 1000);
  };

  const handleVerifyCode = async () => {
    if (verificationCode.length !== 6) {
      toast.error('인증번호 6자리를 입력해주세요');
      return;
    }

    setIsLoading(true);
    // 실제로는 API 호출
    setTimeout(() => {
      setIsLoading(false);
      toast.success('인증되었습니다');
      setStep('reset');
    }, 1000);
  };

  const handleResetPassword = async () => {
    if (!newPassword || !confirmPassword) {
      toast.error('비밀번호를 입력해주세요');
      return;
    }
    if (newPassword !== confirmPassword) {
      toast.error('비밀번호가 일치하지 않습니다');
      return;
    }

    setIsLoading(true);
    // 실제로는 API 호출
    setTimeout(() => {
      setIsLoading(false);
      setStep('complete');
    }, 1000);
  };

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center px-4 py-3">
          <Button 
            variant="ghost" 
            size="icon" 
            onClick={() => {
              if (step === 'email') navigate(-1);
              else if (step === 'verify') setStep('email');
              else if (step === 'reset') setStep('verify');
            }} 
            className="-ml-2"
          >
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="ml-2 text-lg font-semibold text-stone-800">비밀번호 찾기</h1>
        </div>
      </header>

      <div className="p-6">
        {/* Email Step */}
        {step === 'email' && (
          <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
            <div className="space-y-2">
              <h2 className="text-2xl font-bold text-stone-900">이메일 입력</h2>
              <p className="text-stone-500">
                가입 시 사용한 이메일을 입력해주세요.<br />
                인증번호를 발송해드립니다.
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="email">이메일</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
                <Input
                  id="email"
                  type="email"
                  placeholder="example@email.com"
                  className="pl-10 h-12 bg-stone-50 border-stone-200 rounded-xl"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>
            </div>

            <Button
              onClick={handleSendCode}
              disabled={!email || isLoading}
              className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl disabled:opacity-50"
            >
              {isLoading ? '발송 중...' : '인증번호 받기'}
            </Button>
          </div>
        )}

        {/* Verify Step */}
        {step === 'verify' && (
          <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
            <div className="space-y-2">
              <h2 className="text-2xl font-bold text-stone-900">인증번호 입력</h2>
              <p className="text-stone-500">
                <span className="text-orange-600">{email}</span>로<br />
                발송된 인증번호 6자리를 입력해주세요.
              </p>
            </div>

            <div className="flex justify-center py-4">
              <InputOTP
                maxLength={6}
                value={verificationCode}
                onChange={setVerificationCode}
              >
                <InputOTPGroup className="gap-2">
                  <InputOTPSlot index={0} className="w-12 h-14 text-xl border-stone-200 rounded-lg" />
                  <InputOTPSlot index={1} className="w-12 h-14 text-xl border-stone-200 rounded-lg" />
                  <InputOTPSlot index={2} className="w-12 h-14 text-xl border-stone-200 rounded-lg" />
                  <InputOTPSlot index={3} className="w-12 h-14 text-xl border-stone-200 rounded-lg" />
                  <InputOTPSlot index={4} className="w-12 h-14 text-xl border-stone-200 rounded-lg" />
                  <InputOTPSlot index={5} className="w-12 h-14 text-xl border-stone-200 rounded-lg" />
                </InputOTPGroup>
              </InputOTP>
            </div>

            <div className="text-center">
              <button
                onClick={handleSendCode}
                className="text-sm text-stone-500 hover:text-orange-600"
              >
                인증번호 재발송
              </button>
            </div>

            <Button
              onClick={handleVerifyCode}
              disabled={verificationCode.length !== 6 || isLoading}
              className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl disabled:opacity-50"
            >
              {isLoading ? '확인 중...' : '확인'}
            </Button>
          </div>
        )}

        {/* Reset Step */}
        {step === 'reset' && (
          <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300">
            <div className="space-y-2">
              <h2 className="text-2xl font-bold text-stone-900">새 비밀번호 설정</h2>
              <p className="text-stone-500">
                새로운 비밀번호를 입력해주세요.
              </p>
            </div>

            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="newPassword">새 비밀번호</Label>
                <div className="relative">
                  <KeyRound className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
                  <Input
                    id="newPassword"
                    type="password"
                    placeholder="새 비밀번호를 입력하세요"
                    className="pl-10 h-12 bg-stone-50 border-stone-200 rounded-xl"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="confirmPassword">비밀번호 확인</Label>
                <div className="relative">
                  <KeyRound className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
                  <Input
                    id="confirmPassword"
                    type="password"
                    placeholder="비밀번호를 다시 입력하세요"
                    className={`pl-10 h-12 bg-stone-50 border-stone-200 rounded-xl ${
                      confirmPassword && newPassword !== confirmPassword ? 'border-red-500' : ''
                    }`}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                  />
                </div>
                {confirmPassword && newPassword !== confirmPassword && (
                  <p className="text-xs text-red-500">비밀번호가 일치하지 않습니다</p>
                )}
              </div>
            </div>

            <Button
              onClick={handleResetPassword}
              disabled={!newPassword || !confirmPassword || newPassword !== confirmPassword || isLoading}
              className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl disabled:opacity-50"
            >
              {isLoading ? '변경 중...' : '비밀번호 변경'}
            </Button>
          </div>
        )}

        {/* Complete Step */}
        {step === 'complete' && (
          <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-300 text-center py-12">
            <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto">
              <Check className="w-10 h-10 text-green-600" />
            </div>
            <div className="space-y-2">
              <h2 className="text-2xl font-bold text-stone-900">비밀번호 변경 완료</h2>
              <p className="text-stone-500">
                새로운 비밀번호로 로그인해주세요.
              </p>
            </div>

            <Button
              onClick={() => navigate('/login')}
              className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl"
            >
              로그인하러 가기
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}



