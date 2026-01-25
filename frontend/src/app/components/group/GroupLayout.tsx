import { Outlet, NavLink, useParams, Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, Menu } from 'lucide-react';
import { Button } from '../ui/button';
import { useState, useEffect } from 'react';
import { getClub, ClubDetailResponse } from '@/api/club-full';
import { getMembers } from '@/api/member';
import { getMyInfo } from '@/api/user';
import { Badge } from '../ui/badge';

export function GroupLayout() {
  const { groupId } = useParams();
  const navigate = useNavigate();
  const [club, setClub] = useState<ClubDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [userRole, setUserRole] = useState<string>('회원');
  const [roleColor, setRoleColor] = useState<string>('bg-stone-500 text-white');

  useEffect(() => {
    const fetchClub = async () => {
      if (!groupId) return;

      try {
        setLoading(true);
        const response = await getClub(Number(groupId));
        setClub(response);

        // 현재 사용자의 역할 가져오기
        try {
          const myInfo = await getMyInfo();
          const members = await getMembers(Number(groupId), 'ACTIVE');
          const currentMember = members.find(m => m.userId === myInfo.userId);
          if (currentMember) {
            const roles = currentMember.roles || [];
            // 역할 우선순위: OWNER > ACCOUNTANT > STAFF > MEMBER
            if (roles.includes('OWNER')) {
              setUserRole('모임장');
              setRoleColor('bg-orange-500 text-white');
            } else if (roles.includes('ACCOUNTANT')) {
              setUserRole('총무');
              setRoleColor('bg-green-500 text-white');
            } else if (roles.includes('STAFF')) {
              setUserRole('운영진');
              setRoleColor('bg-blue-500 text-white');
            } else {
              setUserRole('회원');
              setRoleColor('bg-stone-500 text-white');
            }
          }
        } catch (error) {
          console.error('사용자 역할 조회 실패:', error);
        }
      } catch (error) {
        console.error('모임 정보 조회 실패:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchClub();
  }, [groupId, location.pathname]);

  const tabs = [
    { label: '홈', path: '' },
    { label: '일정', path: 'schedule' },
    { label: '게시글', path: 'posts' }, // 스토리 -> 게시글
    { label: '통계', path: 'stats' },
    { label: '관리', path: 'admin' },
  ];

  // 역할 배지가 있을 때와 없을 때 탭의 top 위치 계산
  const headerHeight = 57; // 헤더 높이 (px)
  const roleBadgeHeight = userRole && userRole !== '회원' ? 40 : 0; // 역할 배지 높이
  const tabsTop = headerHeight + roleBadgeHeight;

  return (
    <div className="min-h-screen bg-stone-50 text-stone-900 font-sans" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      <div className="max-w-md md:max-w-2xl lg:max-w-4xl mx-auto min-h-screen bg-white shadow-xl relative flex flex-col">
        {/* Top Navigation Bar */}
        <header className="flex items-center justify-between px-4 py-3 bg-white sticky top-0 z-[100] shadow-sm">
          <Link to="/">
            <Button variant="ghost" size="icon" className="-ml-2" aria-label="뒤로가기">
              <ArrowLeft className="w-6 h-6 text-stone-800" />
            </Button>
          </Link>
          <h1 className="font-bold text-lg text-stone-800 truncate px-2">
            {loading ? '로딩 중...' : club?.clubName || '모임'}
          </h1>
          <Button
            variant="ghost"
            size="icon"
            className="-mr-2"
            aria-label="프로필"
            onClick={() => navigate('/profile')}
          >
            <Menu className="w-6 h-6 text-stone-800" />
          </Button>
        </header>

        {/* 역할 배지 */}
        {userRole && userRole !== '회원' && (
          <div className="flex justify-end px-4 py-2 bg-white sticky top-[57px] z-[90] shadow-sm">
            <Badge className={`${roleColor} text-xs`}>
              {userRole}
            </Badge>
          </div>
        )}

        {/* Scrollable Tabs */}
        <div
          className="flex overflow-x-auto scrollbar-hide bg-white sticky z-[80] shadow-sm md:justify-center"
          style={{ top: `${tabsTop}px` }}
        >
          {tabs.map((tab) => (
            <NavLink
              key={tab.path}
              to={tab.path}
              end={tab.path === ''}
              className={({ isActive }) =>
                `flex-none px-5 py-3 text-sm font-medium whitespace-nowrap transition-colors border-b-2 ${isActive
                  ? 'border-orange-500 text-orange-600'
                  : 'border-transparent text-stone-500 hover:text-stone-700'
                }`
              }
            >
              {tab.label}
            </NavLink>
          ))}
        </div>

        {/* Main Content */}
        <main className="flex-1 overflow-y-auto bg-stone-50 p-4 md:p-6" style={{ position: 'relative', zIndex: 1 }}>
          <Outlet context={{ club, loading }} />
        </main>
      </div>
    </div>
  );
}
