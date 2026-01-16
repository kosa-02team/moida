import { useState, useEffect } from 'react';
import { Plus, Bell, Search, X, Users, Compass, KeyRound, Crown, Wallet, Shield, Coins, ShieldAlert } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { MOCK_GROUPS, Group } from '../data/mockData';
import { getRoleLabel, getRoleColor } from '../data/userRoles';
import { Button } from './ui/button';
import { Card, CardContent } from './ui/card';
import { Badge } from './ui/badge';
import { Input } from './ui/input';
import { Avatar, AvatarFallback } from './ui/avatar';
import { Tabs, TabsList, TabsTrigger } from './ui/tabs';

// 전체 공개 모임 (탐색용 mock)
const PUBLIC_GROUPS: Partial<Group>[] = [
  {
    id: 'public1',
    name: '서울 러닝 크루',
    image: 'https://images.unsplash.com/photo-1571008887538-b36bb32f4571?w=400&h=200&fit=crop',
    tags: ['러닝', '운동'],
    memberCount: 42,
    maxMembers: 100,
    type: 'club' as const,
    isPublic: true,
    nextEvent: { title: '한강 러닝', date: '4/14', location: '여의도' },
  },
  {
    id: 'public2',
    name: '독서 모임',
    image: 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400&h=200&fit=crop',
    tags: ['독서', '토론'],
    memberCount: 18,
    maxMembers: 30,
    type: 'study' as const,
    isPublic: true,
    nextEvent: { title: '4월 독서 토론', date: '4/20', location: '강남' },
  },
];

// 역할 아이콘 컴포넌트
function RoleIcon({ role }: { role: string }) {
  if (role.includes('모임장')) return <Crown className="w-3 h-3" />;
  if (role.includes('총무')) return <Wallet className="w-3 h-3" />;
  if (role.includes('운영진')) return <Shield className="w-3 h-3" />;
  return null;
}

export function HomeView() {
  const navigate = useNavigate();
  const unreadNotifications = 2;
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<'all' | 'club' | 'meetup' | 'study'>('all');
  const [searchScope, setSearchScope] = useState<'my' | 'all'>('my');
  const [isSystemAdmin, setIsSystemAdmin] = useState(false);

  // 시스템 관리자 확인
  useEffect(() => {
    const adminStatus = localStorage.getItem('isSystemAdmin');
    setIsSystemAdmin(adminStatus === 'true');
  }, []);

  // 내 모임 필터링
  const filteredMyGroups = MOCK_GROUPS.filter(group => {
    const matchesSearch = 
      group.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      group.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()));
    const matchesType = filterType === 'all' || group.type === filterType;
    return matchesSearch && matchesType;
  });

  // 전체 모임 필터링 (검색어가 있을 때만)
  const filteredAllGroups = searchQuery 
    ? [...MOCK_GROUPS, ...PUBLIC_GROUPS].filter(group => {
        const matchesSearch = 
          group.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          group.tags?.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()));
        const matchesType = filterType === 'all' || group.type === filterType;
        return matchesSearch && matchesType;
      })
    : [];

  const displayGroups = searchScope === 'my' ? filteredMyGroups : filteredAllGroups;

  // 역할별 모임 수 계산
  const roleCounts = {
    owner: MOCK_GROUPS.filter(g => g.myRole === 'owner').length,
    treasurer: MOCK_GROUPS.filter(g => g.myRole === 'treasurer' || g.myRoles?.includes('treasurer')).length,
    manager: MOCK_GROUPS.filter(g => g.myRole === 'manager' || g.myRoles?.includes('manager')).length,
    member: MOCK_GROUPS.filter(g => g.myRole === 'member').length,
  };

  return (
    <div className="p-4 space-y-6 pb-24">
      <header className="flex justify-between items-center py-2">
        <div className="flex items-center gap-2">
          <img src="/logo.svg" alt="모임" className="w-8 h-8" />
          <h1 className="text-2xl font-bold text-stone-800">나의 모임</h1>
        </div>
        <div className="flex items-center gap-1">
          {/* 시스템 관리자 버튼 */}
          {isSystemAdmin && (
            <Button 
              variant="ghost" 
              size="icon" 
              onClick={() => navigate('/system-admin')}
              className="text-red-500 hover:bg-red-50"
            >
              <ShieldAlert className="w-6 h-6" />
            </Button>
          )}
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
            <Avatar className="w-8 h-8 cursor-pointer">
              <AvatarFallback>홍</AvatarFallback>
            </Avatar>
          </Link>
        </div>
      </header>

      {/* System Admin Banner */}
      {isSystemAdmin && (
        <div className="bg-gradient-to-r from-red-500 to-red-600 text-white rounded-xl p-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <ShieldAlert className="w-6 h-6" />
            <div>
              <p className="font-bold">시스템 관리자 모드</p>
              <p className="text-xs text-red-100">모든 모임과 회원을 관리할 수 있습니다</p>
            </div>
          </div>
          <Button 
            variant="secondary" 
            size="sm" 
            onClick={() => navigate('/system-admin')}
            className="bg-white text-red-600 hover:bg-red-50"
          >
            관리 페이지
          </Button>
        </div>
      )}

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

        {/* Search Scope Toggle - 검색어가 있을 때만 표시 */}
        {searchQuery && (
          <Tabs value={searchScope} onValueChange={(v) => setSearchScope(v as 'my' | 'all')} className="w-full">
            <TabsList className="w-full grid grid-cols-2 h-10 bg-stone-100 rounded-lg p-1">
              <TabsTrigger 
                value="my" 
                className="rounded-md data-[state=active]:bg-white data-[state=active]:shadow-sm"
              >
                내 모임
              </TabsTrigger>
              <TabsTrigger 
                value="all"
                className="rounded-md data-[state=active]:bg-white data-[state=active]:shadow-sm"
              >
                전체 모임
              </TabsTrigger>
            </TabsList>
          </Tabs>
        )}

        {/* Filter Chips */}
        <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-1">
          {[
            { value: 'all', label: '전체' },
            { value: 'club', label: '동아리' },
            { value: 'study', label: '스터디' },
            { value: 'meetup', label: '정모' },
          ].map((filter) => (
            <button
              key={filter.value}
              onClick={() => setFilterType(filter.value as typeof filterType)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
                filterType === filter.value
                  ? 'bg-orange-500 text-white'
                  : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
              }`}
            >
              {filter.label}
            </button>
          ))}
        </div>
      </div>

      {/* Search Scope Info */}
      {searchQuery && (
        <div className="text-sm text-stone-500">
          {searchScope === 'my' 
            ? `내 모임에서 "${searchQuery}" 검색 결과 ${displayGroups.length}개`
            : `전체 모임에서 "${searchQuery}" 검색 결과 ${displayGroups.length}개`
          }
        </div>
      )}

      {/* Group List */}
      {displayGroups.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {displayGroups.map((group) => {
            const isMyGroup = MOCK_GROUPS.find(g => g.id === group.id);
            const roleLabel = isMyGroup ? getRoleLabel(group.id || '') : null;
            const roleColor = isMyGroup ? getRoleColor(group.id || '') : '';
            const fullGroup: Group | undefined = isMyGroup || undefined;
            
            return (
              <Link 
                to={searchScope === 'all' && !isMyGroup
                  ? `/explore/${group.id}` 
                  : `/group/${group.id}`
                } 
                key={group.id} 
                className="block"
              >
                <Card className="overflow-hidden hover:shadow-md transition-shadow border-stone-100 bg-white">
                  <div className="relative h-32 bg-stone-200">
                    <img
                      src={group.image}
                      alt={group.name}
                      className="w-full h-full object-cover"
                      loading="lazy"
                      onError={(e) => {
                        const target = e.target as HTMLImageElement;
                        target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2Y1ZjVmNSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5Yzk3OTciIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5JbWFnZSBub3QgZm91bmQ8L3RleHQ+PC9zdmc+';
                      }}
                    />
                    <div className="absolute top-2 right-2 flex gap-1">
                      {searchScope === 'all' && !isMyGroup && (
                        <Badge variant="secondary" className="bg-blue-500 text-white text-xs">
                          공개
                        </Badge>
                      )}
                      <Badge variant="secondary" className="bg-white/90 backdrop-blur-sm text-stone-800 hover:bg-white">
                        {group.type === 'club' ? '동아리' : group.type === 'study' ? '스터디' : '정모'}
                      </Badge>
                    </div>
                    {/* 역할 배지 - 내 모임만 표시 */}
                    {roleLabel && (
                      <div className="absolute bottom-2 left-2">
                        <Badge className={`${roleColor} text-xs flex items-center gap-1 shadow-sm`}>
                          <RoleIcon role={roleLabel} />
                          {roleLabel}
                        </Badge>
                      </div>
                    )}
                    {/* 통장 유형 배지 - 내 모임만 표시 */}
                    {fullGroup?.account && (
                      <div className="absolute bottom-2 right-2">
                        <Badge className={`text-xs shadow-sm ${
                          fullGroup.account.managementType === 'fair' 
                            ? 'bg-green-500/90 text-white' 
                            : 'bg-blue-500/90 text-white'
                        }`}>
                          <Coins className="w-3 h-3 mr-1" />
                          {fullGroup.account.managementType === 'fair' ? '공정정산' : '운영비'}
                        </Badge>
                      </div>
                    )}
                  </div>
                  <CardContent className="p-4">
                    <div className="flex justify-between items-start mb-2">
                      <h3 className="font-bold text-lg text-stone-900">{group.name}</h3>
                      <span className="text-xs text-stone-500 bg-stone-100 px-2 py-1 rounded-full">
                        {group.memberCount}/{group.maxMembers}명
                      </span>
                    </div>
                    <div className="flex flex-wrap gap-1 mb-3">
                      {group.tags?.map((tag) => (
                        <span key={tag} className="text-xs text-orange-600 bg-orange-50 px-2 py-0.5 rounded">
                          #{tag}
                        </span>
                      ))}
                    </div>
                    {/* 통장 잔액 - 내 모임만 표시 */}
                    {fullGroup?.account && (
                      <div className="flex items-center justify-between text-sm py-2 px-3 bg-stone-50 rounded-lg mb-3">
                        <span className="text-stone-500">통장 잔액</span>
                        <span className="font-bold text-stone-800">
                          {fullGroup.account.totalBalance.toLocaleString()}원
                        </span>
                      </div>
                    )}
                    {/* 1인당 지분 - 공정정산형만 표시 */}
                    {fullGroup?.account?.managementType === 'fair' && fullGroup.account.perPersonShare && (
                      <div className="flex items-center justify-between text-xs py-1 px-3 bg-green-50 rounded-lg mb-3">
                        <span className="text-green-600">1인당 지분</span>
                        <span className="font-bold text-green-700">
                          {fullGroup.account.perPersonShare.toLocaleString()}원
                        </span>
                      </div>
                    )}
                    {group.nextEvent && (
                      <div className="flex items-center text-sm text-stone-600 mt-3 pt-3 border-t border-stone-100">
                        <span className="w-2 h-2 rounded-full bg-green-500 mr-2"></span>
                        <span className="truncate">{group.nextEvent.title} ({group.nextEvent.date})</span>
                      </div>
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
              ? searchScope === 'my' 
                ? '전체 모임에서 검색해보세요.' 
                : '다른 검색어로 시도해보세요.'
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
          {searchQuery && searchScope === 'my' && (
            <Button 
              variant="outline" 
              onClick={() => setSearchScope('all')}
              className="border-orange-500 text-orange-600 hover:bg-orange-50"
            >
              전체 모임에서 검색
            </Button>
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
