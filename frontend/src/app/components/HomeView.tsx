import { useState, useEffect } from 'react';
import { Plus, Bell, Search, X, Users, Compass, KeyRound, Crown, Wallet, Shield } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Button } from './ui/button';
import { Card, CardContent } from './ui/card';
import { Badge } from './ui/badge';
import { Input } from './ui/input';
import { Avatar, AvatarFallback } from './ui/avatar';
import { Tabs, TabsList, TabsTrigger } from './ui/tabs';
import { getMyClubs, MyClubResponse, getMyInfo, UserResponse } from '@/api/user';
import { getUnreadCount } from '@/api/notification';
import { get, getToken, AuthenticationError } from '@/api/client';

// 백엔드 Category enum과 일치
type CategoryType = 'all' | 'STUDY' | 'SPORTS' | 'SOCIAL' | 'HOBBY' | 'FINANCE' | 'ETC';

const CATEGORY_LABELS: Record<CategoryType, string> = {
  all: '전체',
  STUDY: '스터디',
  SPORTS: '운동',
  SOCIAL: '친목',
  HOBBY: '취미',
  FINANCE: '재테크',
  ETC: '기타',
};

// 역할 아이콘 컴포넌트
function RoleIcon({ role }: { role: string }) {
  if (role.includes('모임장')) return <Crown className="w-3 h-3" />;
  if (role.includes('총무')) return <Wallet className="w-3 h-3" />;
  if (role.includes('운영진')) return <Shield className="w-3 h-3" />;
  return null;
}

export function HomeView() {
  const navigate = useNavigate();
  const [unreadNotifications, setUnreadNotifications] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterCategory, setFilterCategory] = useState<CategoryType>('all');
  const [myClubs, setMyClubs] = useState<MyClubResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [userInfo, setUserInfo] = useState<UserResponse | null>(null);

  useEffect(() => {
    async function fetchData() {
      // 로그인 체크
      const token = getToken();
      if (!token) {
        navigate('/login');
        return;
      }

      try {
        setLoading(true);

        // 내 정보 조회
        try {
          const user = await getMyInfo();
          setUserInfo(user);
        } catch (error) {
          console.error('사용자 정보 조회 실패:', error);
        }

        // 내 모임 목록 조회
        const myClubsData = await getMyClubs();
        setMyClubs(myClubsData);

        // 읽지 않은 알림 개수 조회 (로그인된 경우만)
        try {
          const unreadCount = await getUnreadCount();
          setUnreadNotifications(unreadCount);
        } catch (error: any) {
          // 500 에러 등 서버 에러는 조용히 처리 (백엔드 문제)
          // console.error는 제거하여 과도한 에러 로그 방지
          setUnreadNotifications(0);
        }

      } catch (error) {
        console.error('데이터 불러오기 실패:', error);
        // 인증 에러일 때만 로그인 페이지로 리다이렉트 (조용히)
        if (error instanceof AuthenticationError) {
          navigate('/login');
        } else {
          // 일반 에러는 토스트만 표시하고 페이지는 유지
          toast.error('데이터를 불러오는 중 오류가 발생했습니다');
          setLoading(false);
        }
      } finally {
        setLoading(false);
      }
    }

    fetchData();
  }, [searchQuery, navigate]);

  // 내 모임 필터링 (카테고리 포함)
  const filteredMyGroups = myClubs.filter(club => {
    const matchesSearch =
      club.name.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = filterCategory === 'all' || club.category === filterCategory;
    
    return matchesSearch && matchesCategory;
  });

  // 항상 내 모임만 표시
  const displayGroups = filteredMyGroups;

  // 역할별 모임 수 계산
  const roleCounts = {
    owner: myClubs.filter(c => c.roles.includes('OWNER')).length,
    treasurer: myClubs.filter(c => c.roles.includes('ACCOUNTANT')).length,
    manager: myClubs.filter(c => c.roles.includes('STAFF')).length,
    member: myClubs.filter(c => c.roles.includes('MEMBER') || c.roles.length === 0).length,
  };

  return (
    <div className="p-4 space-y-6 pb-24">
      <header className="flex justify-between items-center py-2">
        <div className="flex items-center gap-2">
          <img src="/logo.svg" alt="모임" className="w-8 h-8" />
          <h1 className="text-2xl font-bold text-stone-800">나의 모임</h1>
        </div>
        <div className="flex items-center gap-1">
          <Link to="/notifications">
            <Button variant="ghost" size="icon" className="text-stone-500 relative">
              <span className="sr-only">알림</span>
              <Bell className="w-6 h-6" />
              {unreadNotifications > 0 && (
                <span className="absolute -top-1 -right-1 w-5 h-5 bg-orange-500 text-white text-xs font-bold rounded-full flex items-center justify-center">
                  {unreadNotifications}
                </span>
              )}
            </Button>
          </Link>
          <Link to="/profile">
            <Avatar className="w-8 h-8 cursor-pointer bg-orange-100">
              <AvatarFallback className="bg-orange-100 text-orange-600 text-sm font-medium">
                {userInfo?.realName ? userInfo.realName[0] : '👤'}
              </AvatarFallback>
            </Avatar>
          </Link>
        </div>
      </header>


      {/* Quick Actions */}
      <div className="flex gap-2">
        <Link to="/explore" className="flex-1">
          <Button variant="outline" className="w-full h-12 rounded-xl border-orange-200 hover:bg-orange-50 hover:border-orange-300">
            <Compass className="w-5 h-5 mr-2 text-orange-500" />
            <span className="text-stone-700">모임 둘러보기</span>
          </Button>
        </Link>
        <Link to="/invite-code" className="flex-1">
          <Button variant="outline" className="w-full h-12 rounded-xl border-stone-200 hover:bg-stone-50">
            <KeyRound className="w-5 h-5 mr-2 text-stone-500" />
            <span className="text-stone-700">초대코드 입력</span>
          </Button>
        </Link>
      </div>

      {/* Role Summary */}
      <div className="grid grid-cols-4 gap-2">
        <div className="flex flex-col items-center p-2 bg-orange-50 rounded-xl">
          <Crown className="w-5 h-5 text-orange-500 mb-1" />
          <span className="text-lg font-bold text-orange-600">{roleCounts.owner}</span>
          <span className="text-xs text-stone-500">모임장</span>
        </div>
        <div className="flex flex-col items-center p-2 bg-green-50 rounded-xl">
          <Wallet className="w-5 h-5 text-green-500 mb-1" />
          <span className="text-lg font-bold text-green-600">{roleCounts.treasurer}</span>
          <span className="text-xs text-stone-500">총무</span>
        </div>
        <div className="flex flex-col items-center p-2 bg-blue-50 rounded-xl">
          <Shield className="w-5 h-5 text-blue-500 mb-1" />
          <span className="text-lg font-bold text-blue-600">{roleCounts.manager}</span>
          <span className="text-xs text-stone-500">운영진</span>
        </div>
        <div className="flex flex-col items-center p-2 bg-stone-100 rounded-xl">
          <Users className="w-5 h-5 text-stone-500 mb-1" />
          <span className="text-lg font-bold text-stone-600">{roleCounts.member}</span>
          <span className="text-xs text-stone-500">회원</span>
        </div>
      </div>

      {/* Search */}
      <div className="space-y-3">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
          <Input
            placeholder="모임 이름 또는 태그로 검색"
            className="pl-10 pr-10 h-11 bg-white border-stone-200 rounded-xl"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-stone-400 hover:text-stone-600"
            >
              <X className="w-5 h-5" />
            </button>
          )}
        </div>


        {/* Category Filter Chips */}
        <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-1">
          {(Object.keys(CATEGORY_LABELS) as CategoryType[]).map((category) => (
            <button
              key={category}
              onClick={() => setFilterCategory(category)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${filterCategory === category
                ? 'bg-orange-500 text-white'
                : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
                }`}
            >
              {CATEGORY_LABELS[category]}
            </button>
          ))}
        </div>
      </div>

      {/* Search Scope Info */}
      {searchQuery && (
        <div className="text-sm text-stone-500">
          내 모임에서 "{searchQuery}" 검색 결과 {displayGroups.length}개
        </div>
      )}

      {/* Group List */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <div className="text-stone-500">로딩 중...</div>
        </div>
      ) : displayGroups.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {displayGroups.map((club) => {
            const primaryRole = club.roles.length > 0 ? club.roles[0] : null;
            const roleLabel = primaryRole ?
              (primaryRole === 'OWNER' ? '모임장' :
                primaryRole === 'ACCOUNTANT' ? '총무' :
                  primaryRole === 'STAFF' ? '운영진' :
                    primaryRole === 'MEMBER' ? '회원' : '') : null;
            const roleColor = primaryRole ?
              (primaryRole === 'OWNER' ? 'bg-orange-500 text-white' :
                primaryRole === 'ACCOUNTANT' ? 'bg-green-500 text-white' :
                  primaryRole === 'STAFF' ? 'bg-blue-500 text-white' :
                    'bg-stone-500 text-white') : '';

            return (
              <Link
                to={`/group/${club.clubId}`}
                key={club.clubId}
                className="block"
              >
                <Card className="overflow-hidden hover:shadow-md transition-shadow border-stone-100 bg-white">
                  <div className="relative h-32 bg-gradient-to-br from-orange-50 via-stone-50 to-orange-100">
                    {(() => {
                      const clubImage = localStorage.getItem(`club_image_${club.clubId}`);
                      return clubImage ? (
                        <img 
                          src={clubImage} 
                          alt={club.name} 
                          className="w-full h-full object-cover"
                          onError={(e) => {
                            // 이미지 로드 실패 시 기본 placeholder 표시
                            const target = e.target as HTMLImageElement;
                            target.style.display = 'none';
                            const parent = target.parentElement;
                            if (parent) {
                              const fallback = document.createElement('div');
                              fallback.className = 'w-full h-full flex items-center justify-center';
                              fallback.innerHTML = `<div class="text-4xl font-bold text-orange-200">${club.name[0]}</div>`;
                              parent.appendChild(fallback);
                            }
                          }}
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center">
                          <div className="text-4xl font-bold text-orange-200">
                            {club.name[0]}
                          </div>
                        </div>
                      );
                    })()}
                    {/* 역할 배지 - 내 모임만 표시 */}
                    {roleLabel && (
                      <div className="absolute bottom-2 left-2">
                        <Badge className={`${roleColor} text-xs flex items-center gap-1 shadow-sm`}>
                          {primaryRole === 'OWNER' && <Crown className="w-3 h-3" />}
                          {primaryRole === 'ACCOUNTANT' && <Wallet className="w-3 h-3" />}
                          {primaryRole === 'STAFF' && <Shield className="w-3 h-3" />}
                          {roleLabel}
                        </Badge>
                      </div>
                    )}
                  </div>
                  <CardContent className="p-4">
                    <div className="flex justify-between items-start mb-2">
                      <h3 className="font-bold text-lg text-stone-900">{club.name}</h3>
                    </div>
                    {club.category && (
                      <Badge variant="secondary" className="bg-stone-100 text-stone-600 text-xs font-normal mt-2">
                        {CATEGORY_LABELS[club.category as CategoryType] || club.category}
                      </Badge>
                    )}
                  </CardContent>
                </Card>
              </Link>
            );
          })}
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <div className="w-16 h-16 bg-stone-100 rounded-full flex items-center justify-center mb-4">
            <Users className="w-8 h-8 text-stone-400" />
          </div>
          <h3 className="text-lg font-semibold text-stone-700 mb-2">
            {searchQuery ? '검색 결과가 없습니다' : '모임이 없습니다'}
          </h3>
          <p className="text-sm text-stone-500 mb-4">
            {searchQuery
              ? '다른 검색어로 시도해보세요.'
              : '새로운 모임을 만들거나 둘러보세요!'}
          </p>
          {!searchQuery && (
            <div className="flex gap-3">
              <Link to="/explore">
                <Button variant="outline" className="border-orange-500 text-orange-600 hover:bg-orange-50">
                  <Compass className="w-4 h-4 mr-2" />
                  모임 둘러보기
                </Button>
              </Link>
              <Link to="/create-group">
                <Button className="bg-orange-500 hover:bg-orange-600">
                  <Plus className="w-4 h-4 mr-2" />
                  모임 만들기
                </Button>
              </Link>
            </div>
          )}
        </div>
      )}

      {/* FAB for Creating Group */}
      <div className="fixed bottom-20 right-4 md:right-[calc(50%-220px+1rem)] z-40">
        <Link to="/create-group" aria-label="새 모임 만들기">
          <Button size="lg" className="rounded-full w-14 h-14 shadow-lg bg-orange-500 hover:bg-orange-600 text-white p-0 transition-transform hover:scale-110 active:scale-95">
            <Plus className="w-8 h-8" />
          </Button>
        </Link>
      </div>
    </div>
  );
}
