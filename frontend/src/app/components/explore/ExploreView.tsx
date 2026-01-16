import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Search, X, MapPin, Users, ChevronRight, SlidersHorizontal, Eye, Lock } from 'lucide-react';
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

interface PublicGroup {
  id: string;
  name: string;
  image: string;
  description: string;
  tags: string[];
  memberCount: number;
  maxMembers: number;
  type: 'club' | 'meetup' | 'study';
  location?: string;
  isPostPublic: boolean; // 게시글 공개 여부
  nextEvent?: {
    title: string;
    date: string;
  };
}

export function ExploreView() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTypes, setSelectedTypes] = useState<string[]>([]);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [showPublicOnly, setShowPublicOnly] = useState(false);

  // Mock data - 게시글 공개/비공개 모임 혼합
  const publicGroups: PublicGroup[] = [
    {
      id: '1',
      name: '주말 등산 클럽',
      image: 'https://images.unsplash.com/photo-1551632811-561732d1e306?w=500&auto=format&fit=crop&q=60',
      description: '매주 토요일 서울 근교 산행합니다. 초보자 환영!',
      tags: ['등산', '운동', '친목'],
      memberCount: 15,
      maxMembers: 50,
      type: 'club',
      location: '서울',
      isPostPublic: true, // 게시글 공개
      nextEvent: { title: '관악산 정기 산행', date: '4/12' },
    },
    {
      id: '2',
      name: '프라이빗 독서 모임',
      image: 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=500&auto=format&fit=crop&q=60',
      description: '비공개로 진행되는 프리미엄 독서 모임입니다.',
      tags: ['독서', '토론', '인문학'],
      memberCount: 8,
      maxMembers: 15,
      type: 'study',
      location: '강남',
      isPostPublic: false, // 게시글 비공개
      nextEvent: { title: '4월 독서 토론', date: '4/18' },
    },
    {
      id: '3',
      name: '개발자 네트워킹',
      image: 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=500&auto=format&fit=crop&q=60',
      description: 'IT 업계 종사자들의 네트워킹 모임입니다.',
      tags: ['개발', 'IT', '네트워킹'],
      memberCount: 25,
      maxMembers: 30,
      type: 'meetup',
      location: '강남',
      isPostPublic: true, // 게시글 공개
      nextEvent: { title: '4월 정기 밋업', date: '4/20' },
    },
    {
      id: '4',
      name: '요가 & 명상 클럽',
      image: 'https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=500&auto=format&fit=crop&q=60',
      description: '바쁜 일상 속 마음의 평화를 찾아요.',
      tags: ['요가', '명상', '건강'],
      memberCount: 12,
      maxMembers: 20,
      type: 'club',
      location: '여의도',
      isPostPublic: true, // 게시글 공개
      nextEvent: { title: '일요일 모닝 요가', date: '4/14' },
    },
    {
      id: '5',
      name: '비밀 투자 스터디',
      image: 'https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=500&auto=format&fit=crop&q=60',
      description: '소수 정예로 진행하는 투자 전략 스터디입니다.',
      tags: ['주식', '투자', '재테크'],
      memberCount: 6,
      maxMembers: 10,
      type: 'study',
      location: '온라인',
      isPostPublic: false, // 게시글 비공개
    },
    {
      id: '6',
      name: '주말 러닝 크루',
      image: 'https://images.unsplash.com/photo-1571008887538-b36bb32f4571?w=500&auto=format&fit=crop&q=60',
      description: '함께 뛰면 더 즐거워요! 러닝 크루 멤버 모집',
      tags: ['러닝', '운동', '건강'],
      memberCount: 35,
      maxMembers: 50,
      type: 'club',
      location: '서울숲',
      isPostPublic: true, // 게시글 공개
      nextEvent: { title: '서울숲 10km 러닝', date: '4/13' },
    },
    {
      id: '7',
      name: '사진 동호회',
      image: 'https://images.unsplash.com/photo-1542038784456-1ea8e935640e?w=500&auto=format&fit=crop&q=60',
      description: '출사와 사진 리뷰를 함께하는 모임',
      tags: ['사진', '출사', '취미'],
      memberCount: 18,
      maxMembers: 25,
      type: 'club',
      location: '서울',
      isPostPublic: true, // 게시글 공개
    },
    {
      id: '8',
      name: '프리미엄 와인 모임',
      image: 'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?w=500&auto=format&fit=crop&q=60',
      description: '와인 애호가들의 비공개 테이스팅 모임',
      tags: ['와인', '테이스팅', '프리미엄'],
      memberCount: 10,
      maxMembers: 15,
      type: 'meetup',
      location: '청담',
      isPostPublic: false, // 게시글 비공개
      nextEvent: { title: '프랑스 와인 테이스팅', date: '4/25' },
    },
    {
      id: '9',
      name: '영어 회화 스터디',
      image: 'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=500&auto=format&fit=crop&q=60',
      description: '영어로 자유롭게 대화하는 스터디입니다.',
      tags: ['영어', '회화', '스터디'],
      memberCount: 12,
      maxMembers: 16,
      type: 'study',
      location: '홍대',
      isPostPublic: true, // 게시글 공개
      nextEvent: { title: '토요일 프리토킹', date: '4/13' },
    },
    {
      id: '10',
      name: 'VIP 골프 모임',
      image: 'https://images.unsplash.com/photo-1535131749006-b7f58c99034b?w=500&auto=format&fit=crop&q=60',
      description: '골프를 사랑하는 분들의 프라이빗 모임',
      tags: ['골프', '운동', 'VIP'],
      memberCount: 8,
      maxMembers: 12,
      type: 'club',
      location: '용인',
      isPostPublic: false, // 게시글 비공개
    },
  ];

  const allTags = Array.from(new Set(publicGroups.flatMap(g => g.tags)));

  const filteredGroups = publicGroups.filter(group => {
    const matchesSearch = 
      group.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      group.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      group.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()));
    
    const matchesType = selectedTypes.length === 0 || selectedTypes.includes(group.type);
    const matchesTags = selectedTags.length === 0 || group.tags.some(tag => selectedTags.includes(tag));
    const matchesPublic = !showPublicOnly || group.isPostPublic;
    
    return matchesSearch && matchesType && matchesTags && matchesPublic;
  });

  const toggleType = (type: string) => {
    setSelectedTypes(prev =>
      prev.includes(type) ? prev.filter(t => t !== type) : [...prev, type]
    );
  };

  const toggleTag = (tag: string) => {
    setSelectedTags(prev =>
      prev.includes(tag) ? prev.filter(t => t !== tag) : [...prev, tag]
    );
  };

  const clearFilters = () => {
    setSelectedTypes([]);
    setSelectedTags([]);
    setShowPublicOnly(false);
  };

  const activeFilterCount = selectedTypes.length + selectedTags.length + (showPublicOnly ? 1 : 0);
  const publicCount = filteredGroups.filter(g => g.isPostPublic).length;
  const privateCount = filteredGroups.filter(g => !g.isPostPublic).length;

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="p-4 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <img src="/logo.svg" alt="모임" className="w-8 h-8" />
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
                  <SheetDescription>원하는 조건으로 모임을 찾아보세요</SheetDescription>
                </SheetHeader>
                <div className="py-6 space-y-6">
                  {/* Post Visibility Filter */}
                  <div className="space-y-3">
                    <Label className="text-base font-medium">게시글 공개 여부</Label>
                    <div className="flex items-center gap-2">
                      <Checkbox
                        id="publicOnly"
                        checked={showPublicOnly}
                        onCheckedChange={(checked) => setShowPublicOnly(checked as boolean)}
                        className="data-[state=checked]:bg-orange-500"
                      />
                      <Label htmlFor="publicOnly" className="cursor-pointer flex items-center gap-2">
                        <Eye className="w-4 h-4 text-green-600" />
                        게시글 공개 모임만 보기
                      </Label>
                    </div>
                  </div>

                  {/* Type Filter */}
                  <div className="space-y-3">
                    <Label className="text-base font-medium">모임 유형</Label>
                    <div className="space-y-2">
                      {[
                        { value: 'club', label: '동아리' },
                        { value: 'study', label: '스터디' },
                        { value: 'meetup', label: '정모' },
                      ].map(type => (
                        <div key={type.value} className="flex items-center gap-2">
                          <Checkbox
                            id={type.value}
                            checked={selectedTypes.includes(type.value)}
                            onCheckedChange={() => toggleType(type.value)}
                            className="data-[state=checked]:bg-orange-500"
                          />
                          <Label htmlFor={type.value} className="cursor-pointer">
                            {type.label}
                          </Label>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Tags Filter */}
                  <div className="space-y-3">
                    <Label className="text-base font-medium">관심사</Label>
                    <div className="flex flex-wrap gap-2">
                      {allTags.map(tag => (
                        <button
                          key={tag}
                          onClick={() => toggleTag(tag)}
                          className={`px-3 py-1.5 rounded-full text-sm transition-colors ${
                            selectedTags.includes(tag)
                              ? 'bg-orange-500 text-white'
                              : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
                          }`}
                        >
                          #{tag}
                        </button>
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
            {filteredGroups.length}개의 모임
          </p>
          <div className="flex items-center gap-2 text-xs">
            <span className="flex items-center gap-1 text-green-600 bg-green-50 px-2 py-1 rounded-full">
              <Eye className="w-3 h-3" />
              공개 {publicCount}
            </span>
            <span className="flex items-center gap-1 text-stone-500 bg-stone-100 px-2 py-1 rounded-full">
              <Lock className="w-3 h-3" />
              비공개 {privateCount}
            </span>
          </div>
        </div>
      </div>

      {/* Group List */}
      <div className="px-4 pb-4">
        {filteredGroups.length > 0 ? (
          <div className="space-y-4">
            {filteredGroups.map(group => (
              <Link to={`/explore/${group.id}`} key={group.id}>
                <Card className={`overflow-hidden hover:shadow-md transition-shadow ${
                  group.isPostPublic ? 'border-stone-100' : 'border-stone-200 bg-stone-50/50'
                }`}>
                  <div className="flex">
                    <div className="w-28 h-28 bg-stone-200 flex-shrink-0 relative">
                      <img
                        src={group.image}
                        alt={group.name}
                        className="w-full h-full object-cover"
                      />
                      {/* 공개/비공개 배지 */}
                      <div className={`absolute top-2 left-2 flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium ${
                        group.isPostPublic 
                          ? 'bg-green-500 text-white' 
                          : 'bg-stone-700 text-white'
                      }`}>
                        {group.isPostPublic ? (
                          <>
                            <Eye className="w-3 h-3" />
                            공개
                          </>
                        ) : (
                          <>
                            <Lock className="w-3 h-3" />
                            비공개
                          </>
                        )}
                      </div>
                    </div>
                    <CardContent className="flex-1 p-3">
                      <div className="flex items-start justify-between">
                        <div>
                          <h3 className="font-bold text-stone-900">{group.name}</h3>
                          <div className="flex items-center gap-2 mt-1">
                            <Badge variant="secondary" className="text-xs bg-stone-100">
                              {group.type === 'club' ? '동아리' : group.type === 'study' ? '스터디' : '정모'}
                            </Badge>
                            {group.location && (
                              <span className="text-xs text-stone-500 flex items-center gap-0.5">
                                <MapPin className="w-3 h-3" />
                                {group.location}
                              </span>
                            )}
                          </div>
                        </div>
                        <ChevronRight className="w-5 h-5 text-stone-300" />
                      </div>
                      <p className="text-xs text-stone-500 mt-2 line-clamp-1">
                        {group.description}
                      </p>
                      <div className="flex items-center justify-between mt-2">
                        <div className="flex items-center gap-1 text-xs text-stone-500">
                          <Users className="w-3 h-3" />
                          {group.memberCount}/{group.maxMembers}명
                        </div>
                        {group.nextEvent && (
                          <span className="text-xs text-orange-600 bg-orange-50 px-2 py-0.5 rounded">
                            {group.nextEvent.date} {group.nextEvent.title}
                          </span>
                        )}
                      </div>
                    </CardContent>
                  </div>
                </Card>
              </Link>
            ))}
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
              <p className="text-sm font-medium text-blue-800">게시글 공개/비공개 안내</p>
              <p className="text-xs text-blue-700 mt-1">
                <span className="text-green-600 font-medium">게시글 공개</span> 모임은 가입 전에도 게시글을 미리 볼 수 있습니다.<br/>
                <span className="text-stone-600 font-medium">게시글 비공개</span> 모임은 가입 후에만 게시글을 확인할 수 있습니다.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
