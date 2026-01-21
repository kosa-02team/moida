import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, ChevronRight, Settings, Edit3, Users, Calendar, Wallet, LogOut, Shield, Bell, HelpCircle, FileText } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '../ui/avatar';
import { Badge } from '../ui/badge';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '../ui/alert-dialog';
import { getMyInfo, UserResponse, getMyClubs, MyClubResponse } from '@/api/user';
import { removeToken, AuthenticationError } from '@/api/client';

interface MenuItem {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  description?: string;
  href: string;
}

interface MenuSection {
  section: string;
  items: MenuItem[];
}

export function ProfileView() {
  const navigate = useNavigate();
  const [user, setUser] = useState<UserResponse | null>(null);
  const [clubs, setClubs] = useState<MyClubResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchData() {
      try {
        setLoading(true);
        const userInfoPromise = getMyInfo();
        const myClubsPromise = getMyClubs();
        const userInfo = await userInfoPromise;
        const myClubs = await myClubsPromise;
        setUser(userInfo);
        setClubs(myClubs);
      } catch (error) {
        console.error('프로필 조회 실패:', error);
        // 인증 에러면 조용히 로그인 페이지로 리다이렉트
        if (error instanceof AuthenticationError) {
          navigate('/login');
        } else {
          // 일반 에러는 토스트만 표시
          toast.error('프로필을 불러오는데 실패했습니다.');
        }
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, [navigate]);

  const menuItems: MenuSection[] = [
    {
      section: '모임',
      items: [
        { icon: Users, label: '내 모임', description: `${clubs.length}개 모임 활동 중`, href: '/' },
        { icon: Calendar, label: '내 일정', href: '/' },
      ],
    },
    {
      section: '설정',
      items: [
        { icon: Bell, label: '알림 설정', href: '/settings/notifications' },
        { icon: Shield, label: '개인정보 보호', href: '/settings/privacy' },
        { icon: Settings, label: '앱 설정', href: '/settings' },
      ],
    },
    {
      section: '기타',
      items: [
        { icon: HelpCircle, label: '도움말', href: '/help' },
        { icon: FileText, label: '이용약관', href: '/terms' },
      ],
    },
  ];

  const handleLogout = () => {
    removeToken();
    localStorage.removeItem('refreshToken');
    toast.success('로그아웃 되었습니다');
    navigate('/login');
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">프로필을 불러올 수 없습니다.</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
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
            <h1 className="ml-2 text-lg font-semibold text-stone-800">내 프로필</h1>
          </div>
          <Link to="/profile/edit">
            <Button variant="ghost" size="icon">
              <Edit3 className="w-5 h-5 text-stone-600" />
            </Button>
          </Link>
        </div>
      </header>

      {/* Profile Card */}
      <div className="p-4">
        <div className="bg-white rounded-2xl p-6 border border-stone-100 shadow-sm">
          <div className="flex items-center gap-4">
            <Avatar className="w-20 h-20 border-4 border-orange-100">
              <AvatarFallback className="text-2xl bg-orange-100 text-orange-600">
                {user.realName?.[0] || '?'}
              </AvatarFallback>
            </Avatar>
            <div>
              <h2 className="text-xl font-bold text-stone-900">{user.realName}</h2>
              <p className="text-sm text-stone-500">{user.loginId}</p>
              <Badge variant="secondary" className="mt-2 bg-orange-100 text-orange-700">
                가입일 {new Date(user.createdAt).toLocaleDateString('ko-KR')}
              </Badge>
            </div>
          </div>

          {/* Stats */}
          <div className="grid grid-cols-2 gap-4 mt-6 pt-6 border-t border-stone-100">
            <div className="text-center">
              <p className="text-2xl font-bold text-orange-600">{clubs.length}</p>
              <p className="text-xs text-stone-500">활동 모임</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-orange-600">{user.status === 'ACTIVE' ? '정상' : '정지'}</p>
              <p className="text-xs text-stone-500">계정 상태</p>
            </div>
          </div>
        </div>
      </div>

      {/* Menu Sections */}
      <div className="px-4 space-y-4">
        {menuItems.map(section => (
          <div key={section.section} className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
            <div className="px-4 py-3 bg-stone-50 border-b border-stone-100">
              <h3 className="text-sm font-medium text-stone-500">{section.section}</h3>
            </div>
            <div className="divide-y divide-stone-100">
              {section.items.map(item => (
                <Link
                  key={item.label}
                  to={item.href}
                  className="flex items-center justify-between p-4 hover:bg-stone-50 transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-stone-100 rounded-lg flex items-center justify-center">
                      <item.icon className="w-5 h-5 text-stone-600" />
                    </div>
                    <div>
                      <p className="font-medium text-stone-900">{item.label}</p>
                      {item.description && (
                        <p className="text-xs text-stone-500">{item.description}</p>
                      )}
                    </div>
                  </div>
                  <ChevronRight className="w-5 h-5 text-stone-300" />
                </Link>
              ))}
            </div>
          </div>
        ))}

        {/* Logout Button */}
        <AlertDialog>
          <AlertDialogTrigger asChild>
            <button className="w-full p-4 bg-white rounded-2xl border border-stone-100 flex items-center gap-3 hover:bg-stone-50 transition-colors">
              <div className="w-10 h-10 bg-red-100 rounded-lg flex items-center justify-center">
                <LogOut className="w-5 h-5 text-red-600" />
              </div>
              <span className="font-medium text-red-600">로그아웃</span>
            </button>
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>로그아웃</AlertDialogTitle>
              <AlertDialogDescription>
                정말 로그아웃 하시겠습니까?
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>취소</AlertDialogCancel>
              <AlertDialogAction
                onClick={handleLogout}
                className="bg-red-500 hover:bg-red-600"
              >
                로그아웃
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>

        {/* Version Info */}
        <p className="text-center text-xs text-stone-400 py-4">
          버전 1.0.0
        </p>
      </div>
    </div>
  );
}



