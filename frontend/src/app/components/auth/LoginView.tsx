import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Mail, Lock, ShieldAlert } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Checkbox } from '../ui/checkbox';

export function LoginView() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!email || !password) {
      toast.error('이메일과 비밀번호를 입력해주세요');
      return;
    }

    setIsLoading(true);
    
    setTimeout(() => {
      setIsLoading(false);
      
      // 시스템 관리자 계정 확인
      if (email === 'admin@moim.com' && password === 'admin1234') {
        localStorage.setItem('userRole', 'admin');
        localStorage.setItem('userEmail', email);
        localStorage.setItem('isSystemAdmin', 'true');
        toast.success('시스템 관리자로 로그인되었습니다!', {
          description: '모든 모임과 회원을 관리할 수 있습니다.'
        });
        navigate('/system-admin');
        return;
      }
      
      // 일반 로그인
      localStorage.setItem('userRole', 'member');
      localStorage.setItem('userEmail', email);
      localStorage.setItem('isSystemAdmin', 'false');
      toast.success('로그인 성공!');
      navigate('/');
    }, 1000);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-orange-50 via-white to-stone-100 flex flex-col">
      {/* Header */}
      <div className="flex-1 flex flex-col items-center justify-center p-6">
        {/* Logo */}
        <div className="mb-8 text-center">
          <img 
            src="/logo.svg" 
            alt="모임" 
            className="w-16 h-16 mx-auto mb-4 drop-shadow-lg"
          />
          <h1 className="text-2xl font-bold text-stone-900">모임</h1>
          <p className="text-stone-500 mt-1">일정, 회비, 스토리를 한 곳에서</p>
        </div>

        {/* Login Form */}
        <div className="w-full max-w-sm">
          <form onSubmit={handleLogin} className="space-y-4">
            {/* Email */}
            <div className="space-y-2">
              <Label htmlFor="email">이메일</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
                <Input
                  id="email"
                  type="email"
                  placeholder="example@email.com"
                  className="pl-10 h-12 bg-white border-stone-200 rounded-xl"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>
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
                  className="pl-10 pr-10 h-12 bg-white border-stone-200 rounded-xl"
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
            </div>

            {/* Remember Me & Forgot Password */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Checkbox
                  id="remember"
                  checked={rememberMe}
                  onCheckedChange={(checked) => setRememberMe(checked as boolean)}
                  className="data-[state=checked]:bg-orange-500 data-[state=checked]:border-orange-500"
                />
                <Label htmlFor="remember" className="text-sm text-stone-600 cursor-pointer">
                  로그인 유지
                </Label>
              </div>
              <Link to="/forgot-password" className="text-sm text-orange-600 hover:text-orange-700">
                비밀번호 찾기
              </Link>
            </div>

            {/* Login Button */}
            <Button
              type="submit"
              disabled={isLoading}
              className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl"
            >
              {isLoading ? '로그인 중...' : '로그인'}
            </Button>
          </form>

          {/* Sign Up Link */}
          <p className="mt-6 text-center text-stone-600">
            아직 계정이 없으신가요?{' '}
            <Link to="/signup" className="text-orange-600 hover:text-orange-700 font-medium">
              회원가입
            </Link>
          </p>

          {/* Guest Browse */}
          <div className="mt-4 text-center">
            <Link 
              to="/explore" 
              className="inline-flex items-center gap-2 px-4 py-2 text-sm text-stone-600 hover:text-orange-600 bg-stone-100 hover:bg-orange-50 rounded-full transition-colors"
            >
              🔍 로그인 없이 모임 둘러보기
            </Link>
          </div>

          {/* Admin Login Info */}
          <div className="mt-8 pt-6 border-t border-stone-200">
            <div className="flex items-center gap-2 mb-3 justify-center">
              <ShieldAlert className="w-4 h-4 text-red-500" />
              <span className="text-xs text-stone-500">시스템 관리자</span>
            </div>
            <p className="text-xs text-center text-stone-400">
              admin@moim.com / admin1234
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
