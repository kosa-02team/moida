import { Calendar, ChevronRight, Users, Wallet } from 'lucide-react';
import { Button } from '../ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { Progress } from '../ui/progress';
import { Avatar, AvatarImage, AvatarFallback } from '../ui/avatar';
import { Link } from 'react-router-dom';

export function GroupMainView() {
  // Mock data specifically for this view
  const groupInfo = {
    name: '주말 등산 클럽',
    image: 'https://images.unsplash.com/photo-1551632811-561732d1e306?w=800&auto=format&fit=crop&q=60',
    description: '매주 토요일 서울 근교 산행합니다. 초보자 환영!',
    members: 15,
    maxMembers: 50,
    tags: ['등산', '운동', '친목'],
  };

  const nextEvent = {
    id: 1,
    title: '4월 정기 관악산 산행',
    date: '4월 12일 (토) 10:00',
    location: '사당역 4번 출구',
    attendees: 8,
  };

  const stories = [
    { id: 1, image: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=300&h=300&fit=crop' },
    { id: 2, image: 'https://images.unsplash.com/photo-1483728642387-6c3bdd6c93e5?w=300&h=300&fit=crop' },
    { id: 3, image: 'https://images.unsplash.com/photo-1502224562085-639556652f33?w=300&h=300&fit=crop' },
    { id: 4, image: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=300&h=300&fit=crop' },
  ];

  const dues = {
    currentBalance: 1250000,
    unpaidCount: 2,
    nextCollection: '4월 25일',
  };

  return (
    <div className="space-y-6 pb-20">
      {/* Group Profile */}
      <section className="bg-white rounded-2xl p-5 shadow-sm border border-stone-100">
        <div className="flex items-start gap-4">
          <Avatar className="w-20 h-20 rounded-2xl border-2 border-white shadow-md">
            <AvatarImage src={groupInfo.image} className="object-cover" />
            <AvatarFallback>MT</AvatarFallback>
          </Avatar>
          <div className="flex-1 space-y-1">
            <h2 className="text-xl font-bold text-stone-900">{groupInfo.name}</h2>
            <p className="text-sm text-stone-500 line-clamp-2">{groupInfo.description}</p>
            <div className="flex flex-wrap gap-1 pt-1">
              {groupInfo.tags.map(tag => (
                <Badge key={tag} variant="secondary" className="bg-stone-100 text-stone-600 font-normal">#{tag}</Badge>
              ))}
            </div>
          </div>
        </div>
        
        <div className="mt-5 space-y-2">
          <div className="flex justify-between text-sm">
            <span className="flex items-center text-stone-600">
              <Users className="w-4 h-4 mr-1" />
              멤버 {groupInfo.members}명
            </span>
            <span className="text-stone-400">최대 {groupInfo.maxMembers}명</span>
          </div>
          <Progress value={(groupInfo.members / groupInfo.maxMembers) * 100} className="h-2 bg-stone-100" indicatorClassName="bg-orange-500" />
        </div>
      </section>

      {/* Next Event */}
      <section>
        <div className="flex items-center justify-between mb-2 px-1">
          <h3 className="font-bold text-lg text-stone-800">다음 일정</h3>
          <Link to="schedule" className="text-xs text-stone-500 hover:text-orange-500 flex items-center">
            전체보기 <ChevronRight className="w-3 h-3 ml-0.5" />
          </Link>
        </div>
        <Card className="border-l-4 border-l-orange-500 shadow-sm hover:shadow-md transition-shadow">
          <CardContent className="p-4 flex gap-4">
            <div className="flex flex-col items-center justify-center bg-orange-50 w-16 h-16 rounded-xl text-orange-600 shrink-0">
              <span className="text-xs font-bold">4월</span>
              <span className="text-2xl font-bold">12</span>
            </div>
            <div className="flex-1 min-w-0 py-0.5">
              <h4 className="font-bold text-stone-900 text-lg truncate">{nextEvent.title}</h4>
              <div className="flex items-center text-sm text-stone-500 mt-1">
                <span className="truncate">{nextEvent.location}</span>
                <span className="mx-2">•</span>
                <span>{nextEvent.date.split(' ')[2]}</span>
              </div>
              <div className="flex items-center mt-2 text-xs text-orange-600 font-medium">
                {nextEvent.attendees}명 참여 예정
              </div>
            </div>
          </CardContent>
        </Card>
      </section>

      {/* Recent Stories */}
      <section>
        <div className="flex items-center justify-between mb-2 px-1">
          <h3 className="font-bold text-lg text-stone-800">최근 스토리</h3>
          <Link to="stories" className="text-xs text-stone-500 hover:text-orange-500 flex items-center">
            더보기 <ChevronRight className="w-3 h-3 ml-0.5" />
          </Link>
        </div>
        <div className="flex gap-3 overflow-x-auto pb-2 scrollbar-hide -mx-4 px-4">
          {stories.map(story => (
            <div key={story.id} className="relative w-28 h-36 flex-none rounded-xl overflow-hidden shadow-sm">
              <img src={story.image} alt="" className="w-full h-full object-cover" />
              <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent" />
            </div>
          ))}
        </div>
      </section>

      {/* Dues Status */}
      <section>
        <div className="flex items-center justify-between mb-2 px-1">
          <h3 className="font-bold text-lg text-stone-800">회비 현황</h3>
          <Link to="dues" className="text-xs text-stone-500 hover:text-orange-500 flex items-center">
            상세보기 <ChevronRight className="w-3 h-3 ml-0.5" />
          </Link>
        </div>
        <Card className="bg-stone-800 text-white shadow-md border-none">
          <CardContent className="p-5">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2 bg-white/10 rounded-full">
                <Wallet className="w-6 h-6 text-orange-400" />
              </div>
              <div>
                <p className="text-stone-300 text-xs">현재 모임 통장 잔액</p>
                <p className="text-2xl font-bold">{dues.currentBalance.toLocaleString()}원</p>
              </div>
            </div>
            <div className="bg-white/10 rounded-lg p-3 flex justify-between items-center">
              <span className="text-sm text-stone-200">미납 인원</span>
              <div className="flex items-center gap-2">
                <span className="font-bold text-orange-300">{dues.unpaidCount}명</span>
                <Button size="sm" variant="secondary" className="h-7 text-xs bg-white text-stone-900 hover:bg-stone-200">
                  알림 보내기
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
