import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Search, X, Users, ChevronRight, SlidersHorizontal, Eye, ArrowLeft, User } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Card, CardContent } from '../ui/card';
import { Badge } from '../ui/badge';
import { Avatar, AvatarFallback } from '../ui/avatar';
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
import { getToken } from '@/api/client';
import { getMyInfo, type UserResponse } from '@/api/user';
import { getMembers } from '@/api/member';

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

// 1. 공개/비공개 타입 정의
type VisibilityType = 'PUBLIC' | 'PRIVATE';

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
    //2. 공개 여부 상태 추가
    const [selectedVisibility, setSelectedVisibility] = useState<string[]>([]);
    const [publicGroups, setPublicGroups] = useState<PublicGroup[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(false);
    const [totalElements, setTotalElements] = useState(0);
    const [userInfo, setUserInfo] = useState<UserResponse | null>(null);
    const isLoggedIn = !!getToken();

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

            // 필터링: ACTIVE 상태이고, visibility 필터 적용
            let filteredContent = response.content.filter(club => club.status === 'ACTIVE');

            // visibility 필터 적용 (선택된 경우)
            if (selectedVisibility.length > 0) {
                filteredContent = filteredContent.filter(club =>
                    selectedVisibility.includes(club.visibility as VisibilityType)
                );
            }

            const groups: PublicGroup[] = filteredContent.map(club => {
                // 로컬 스토리지에서 이미지 가져오기
                const imageKey = `club_image_${club.clubId}`;
                const storedImage = club.coverImageUrl || localStorage.getItem(imageKey) || '';
                if (storedImage) {
                    console.log(`[목록] 클럽 ${club.clubId} 이미지 발견:`, {
                        imageKey,
                        hasImage: !!storedImage,
                        imageLength: storedImage.length,
                        imageStart: storedImage.substring(0, 50)
                    });
                }
                return {
                    id: club.clubId.toString(),
                    name: club.clubName,
                    image: storedImage || '',
                    description: '',
                    memberCount: club.currentMembers || 0,
                    maxMembers: club.maxMembers || 100,
                    category: (club.category || 'ETC') as CategoryType,
                    isPostPublic: club.visibility === 'PUBLIC',
                };
            });

            if (reset) {
                setPublicGroups(groups);
            } else {
                setPublicGroups(prev => [...prev, ...groups]);
            }

            setHasMore(!response.last);
            // 필터링된 결과의 개수를 사용 (visibility 필터가 적용된 경우를 고려)
            setTotalElements(filteredContent.length);
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
    }, [searchQuery, selectedCategories, selectedVisibility]);

    // 검색어 입력 디바운스
    useEffect(() => {
        const timer = setTimeout(() => {
            if (searchQuery !== undefined) {
                fetchClubs(0, true);
            }
        }, 500);

        return () => clearTimeout(timer);
    }, [searchQuery]);

    // 로그인 시 유저 정보 로드
    useEffect(() => {
        async function fetchUserInfo() {
            if (isLoggedIn && !userInfo) {
                try {
                    const info = await getMyInfo();
                    setUserInfo(info);
                } catch (error) {
                    console.error('유저 정보 로드 실패:', error);
                }
            }
        }
        fetchUserInfo();
    }, [isLoggedIn, userInfo]);

    const handleLoadMore = () => {
        if (!loading && hasMore) {
            fetchClubs(page + 1, false);
            setPage(prev => prev + 1);
        }
    };

    const handleGroupClick = async (e: React.MouseEvent, groupId: string) => {
        if (!isLoggedIn || !userInfo) {
            // 로그인하지 않은 경우 기본 동작 (상세 페이지로)
            return;
        }

        try {
            // 해당 모임의 멤버인지 확인
            const members = await getMembers(Number(groupId), 'ACTIVE');
            const isMember = members.some(member => member.userId === userInfo.userId);

            if (isMember) {
                // 멤버인 경우 모임 페이지로 이동
                e.preventDefault();
                navigate(`/group/${groupId}`);
            }
            // 멤버가 아닌 경우 기본 동작 (상세 페이지로) - Link의 기본 동작 사용
        } catch (error) {
            console.error('멤버 확인 실패:', error);
            // 에러 발생 시 기본 동작 (상세 페이지로)
        }
    };

    const toggleCategory = (category: CategoryType) => {
        setSelectedCategories(prev =>
            prev.includes(category) ? prev.filter(c => c !== category) : [...prev, category]
        );
    };

    const clearFilters = () => {
        setSelectedCategories([]);
        setSelectedVisibility([]);
    };

    const activeFilterCount = selectedCategories.length + selectedVisibility.length;

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
                        {isLoggedIn ? (
                            <div className="flex items-center gap-2">
                                <Link to="/">
                                    <Button variant="ghost" size="sm" className="text-stone-600">
                                        내 모임
                                    </Button>
                                </Link>
                                <Link to="/profile">
                                    <Avatar className="w-8 h-8 cursor-pointer border border-stone-200">
                                        <AvatarFallback className="bg-orange-100 text-orange-600 text-xs">
                                            {userInfo?.realName ? userInfo.realName[0] : <User className="w-4 h-4" />}
                                        </AvatarFallback>
                                    </Avatar>
                                </Link>
                            </div>
                        ) : (
                            <Link to="/login">
                                <Button variant="outline" size="sm" className="rounded-full">
                                    로그인
                                </Button>
                            </Link>
                        )}
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
                        {totalElements > 0
                            ? `${totalElements}개의 ${selectedVisibility.length === 1
                                ? selectedVisibility[0] === 'PUBLIC' ? '공개' : '비공개'
                                : ''}모임`
                            : '모임을 찾는 중...'}
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
                            <Link
                                to={`/explore/${group.id}`}
                                key={group.id}
                                onClick={(e) => handleGroupClick(e, group.id)}
                            >
                                <Card className="overflow-hidden hover:shadow-md transition-shadow border-stone-100">
                                    <div className="flex">
                                        <div className="w-28 h-28 bg-stone-200 flex-shrink-0 relative overflow-hidden">
                                            {group.image ? (
                                                <img
                                                    src={group.image}
                                                    alt={group.name}
                                                    className="w-full h-full object-cover"
                                                    style={{ display: 'block' }}
                                                    onError={(e) => {
                                                        console.error('이미지 로드 실패 (목록):', group.id);
                                                        e.currentTarget.style.display = 'none';
                                                    }}
                                                    onLoad={() => {
                                                        console.log('이미지 로드 성공 (목록):', group.id);
                                                    }}
                                                />
                                            ) : (
                                                <div className="w-full h-full bg-gradient-to-br from-orange-100 to-orange-200 flex items-center justify-center">
                                                    <span className="text-2xl font-bold text-orange-400">
                                                        {group.name[0]}
                                                    </span>
                                                </div>
                                            )}
                                            {/* 공개/비공개 모임 배지 */}
                                            {group.isPostPublic ? (
                                                <div className="absolute top-2 left-2 flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-green-500 text-white">
                                                    <Eye className="w-3 h-3" />
                                                    공개
                                                </div>
                                            ) : (
                                                <div className="absolute top-2 left-2 flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-gray-500 text-white">
                                                    <Eye className="w-3 h-3" />
                                                    비공개
                                                </div>
                                            )}
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