import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Search, X, MapPin, Users, ChevronRight, SlidersHorizontal, Eye, ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Card, CardContent } from '../ui/card';
import { Badge } from '../ui/badge';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '../ui/sheet';
import { Label } from '../ui/label';
import { Checkbox } from '../ui/checkbox';
import { getAllClubs, getClubsByCategory, searchClubs, type PageResponse, type ClubDetailResponse } from '@/api/club-full';

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

interface PublicGroup {
  id: string;
  name: string;
  image: string;
  description: string;
  memberCount: number;
  maxMembers: number;
  category: CategoryType;
  isPostPublic: boolean;
}

export function ExploreView() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategories, setSelectedCategories] = useState<CategoryType[]>([]);
  const [publicGroups, setPublicGroups] = useState<PublicGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [totalElements, setTotalElements] = useState(0);

  const fetchClubs = async (pageNum: number = 0, reset: boolean = false) => {
    try {
      if (reset) {
        setLoading(true);
        setPage(0);
      }

      let response: PageResponse<ClubDetailResponse>;

      // 검색어가 있으면 검색 API 사용
      if (searchQuery.trim()) {
        response = await searchClubs(
          selectedCategories.length === 1 ? selectedCategories[0] : undefined,
          searchQuery.trim(),
          pageNum,
          20
        );
      }
      // 카테고리가 하나만 선택된 경우 카테고리 API 사용
      else if (selectedCategories.length === 1) {
        response = await getClubsByCategory(selectedCategories[0], pageNum, 20);
      }
      // 그 외에는 전체 조회
      else {
        response = await getAllClubs(pageNum, 20);
      }

      // PUBLIC이고 ACTIVE인 모임만 필터링
      const groups: PublicGroup[] = response.content
        .filter(club => club.status === 'ACTIVE' && club.visibility === 'PUBLIC')
        .map(club => ({
          id: club.clubId.toString(),
          name: club.clubName,
          image: '',
          description: '',
          memberCount: club.currentMembers || 0,
          maxMembers: club.maxMembers || 100,
          category: (club.category || 'ETC') as CategoryType,
          isPostPublic: true,
        }));

      if (reset) {
        setPublicGroups(groups);
      } else {
        setPublicGroups(prev => [...prev, ...groups]);
      }

      setHasMore(!response.last);
      setTotalElements(response.totalElements);
    } catch (error) {
      console.error('모임 목록 불러오기 실패:', error);
      toast.error('모임 목록을 불러오는데 실패했습니다.');
      if (reset) {
        setPublicGroups([]);
      }
    } finally {
      setLoading(false);
    }
  };

  // 초기 로드 및 필터/검색 변경 시
  useEffect(() => {
    fetchClubs(0, true);
  }, [searchQuery, selectedCategories]);

  // 검색어 입력 디바운스
  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchQuery !== undefined) {
        fetchClubs(0, true);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [searchQuery]);

  const handleLoadMore = () => {
    if (!loading && hasMore) {
      fetchClubs(page + 1, false);
      setPage(prev => prev + 1);
    }
  };

  const toggleCategory = (category: CategoryType) => {
    setSelectedCategories(prev =>
      prev.includes(category) ? prev.filter(c => c !== category) : [...prev, category]
    );
  };

  const clearFilters = () => {
    setSelectedCategories([]);
  };

  const activeFilterCount = selectedCategories.length;

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="p-4 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Button
                variant="ghost"
                size="icon"
                onClick={() => navigate(-1)}
                className="-ml-2"
              >
                <ArrowLeft className="w-6 h-6 text-stone-800" />
              </Button>
              <h1 className="text-xl font-bold text-stone-900">모임 둘러보기</h1>
            </div>
            <Link to="/login">
              <Button variant="outline" size="sm" className="rounded-full">
                로그인
              </Button>
            </Link>
          </div>

          {/* Search */}
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
              <Input
                placeholder="모임 이름, 태그로 검색"
                className="pl-10 pr-10 h-11 bg-stone-50 border-stone-200 rounded-xl"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-stone-400"
                >
                  <X className="w-5 h-5" />
                </button>
              )}
            </div>

            {/* Filter Button */}
            <Sheet>
              <SheetTrigger asChild>
                <Button variant="outline" size="icon" className="h-11 w-11 rounded-xl relative">
                  <SlidersHorizontal className="w-5 h-5" />
                  {activeFilterCount > 0 && (
                    <span className="absolute -top-1 -right-1 w-5 h-5 bg-orange-500 text-white text-xs rounded-full flex items-center justify-center">
                      {activeFilterCount}
                    </span>
                  )}
                </Button>
              </SheetTrigger>
              <SheetContent>
                <SheetHeader>
                  <SheetTitle>필터</SheetTitle>
                  <SheetDescription>카테고리별로 모임을 찾아보세요</SheetDescription>
                </SheetHeader>
                <div className="py-6 space-y-6">
                  {/* Category Filter */}
                  <div className="space-y-3">
                    <Label className="text-base font-medium">카테고리</Label>
                    <div className="space-y-2">
                      {(Object.keys(CATEGORY_LABELS) as CategoryType[]).map(category => (
                        <div key={category} className="flex items-center gap-2">
                          <Checkbox
                            id={category}
                            checked={selectedCategories.includes(category)}
                            onCheckedChange={() => toggleCategory(category)}
                            className="data-[state=checked]:bg-orange-500"
                          />
                          <Label htmlFor={category} className="cursor-pointer">
                            {CATEGORY_LABELS[category]}
                          </Label>
                        </div>
                      ))}
                    </div>
                  </div>

                  {activeFilterCount > 0 && (
                    <Button
                      variant="outline"
                      onClick={clearFilters}
                      className="w-full"
                    >
                      필터 초기화
                    </Button>
                  )}
                </div>
              </SheetContent>
            </Sheet>
          </div>
        </div>
      </header>

      {/* Results Summary */}
      <div className="p-4 pb-2">
        <div className="flex items-center justify-between">
          <p className="text-sm text-stone-500">
            {totalElements > 0 ? `${totalElements}개의 공개 모임` : '공개 모임을 찾는 중...'}
          </p>
        </div>
      </div>

      {/* Group List */}
      <div className="px-4 pb-4">
        {loading && publicGroups.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <div className="text-stone-500">로딩 중...</div>
          </div>
        ) : publicGroups.length > 0 ? (
          <div className="space-y-4">
            {publicGroups.map(group => (
              <Link to={`/explore/${group.id}`} key={group.id}>
                <Card className="overflow-hidden hover:shadow-md transition-shadow border-stone-100">
                  <div className="flex">
                    <div className="w-28 h-28 bg-stone-200 flex-shrink-0 relative">
                      <img
                        src={group.image}
                        alt={group.name}
                        className="w-full h-full object-cover"
                      />
                      {/* 공개 모임 배지 */}
                      <div className="absolute top-2 left-2 flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-green-500 text-white">
                        <Eye className="w-3 h-3" />
                        공개
                      </div>
                    </div>
                    <CardContent className="flex-1 p-3">
                      <div className="flex items-start justify-between">
                        <div>
                          <h3 className="font-bold text-stone-900">{group.name}</h3>
                          <div className="flex items-center gap-2 mt-1">
                            <Badge variant="secondary" className="text-xs bg-stone-100">
                              {CATEGORY_LABELS[group.category] || '기타'}
                            </Badge>
                          </div>
                        </div>
                        <ChevronRight className="w-5 h-5 text-stone-300" />
                      </div>
                      {group.description && (
                        <p className="text-xs text-stone-500 mt-2 line-clamp-1">
                          {group.description}
                        </p>
                      )}
                      <div className="flex items-center justify-between mt-2">
                        <div className="flex items-center gap-1 text-xs text-stone-500">
                          <Users className="w-3 h-3" />
                          {group.memberCount}/{group.maxMembers}명
                        </div>
                      </div>
                    </CardContent>
                  </div>
                </Card>
              </Link>
            ))}
            {hasMore && (
              <div className="pt-4">
                <Button
                  variant="outline"
                  onClick={handleLoadMore}
                  disabled={loading}
                  className="w-full"
                >
                  {loading ? '로딩 중...' : '더보기'}
                </Button>
              </div>
            )}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <div className="w-16 h-16 bg-stone-100 rounded-full flex items-center justify-center mb-4">
              <Search className="w-8 h-8 text-stone-400" />
            </div>
            <h3 className="text-lg font-semibold text-stone-700 mb-2">모임을 찾을 수 없습니다</h3>
            <p className="text-sm text-stone-500">다른 검색어나 필터로 시도해보세요.</p>
          </div>
        )}
      </div>

      {/* Info Banner */}
      <div className="px-4">
        <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
          <div className="flex items-start gap-3">
            <div className="flex-shrink-0">
              <Eye className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <p className="text-sm font-medium text-blue-800">게시글 공개 모임</p>
              <p className="text-xs text-blue-700 mt-1">
                모든 모임은 게시글이 공개되어 있어 가입 전에도 미리 볼 수 있습니다.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
