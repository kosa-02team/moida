import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, TrendingUp, TrendingDown, Minus, Calendar, Users, Award, ChevronDown } from 'lucide-react';
import { Button } from '../../ui/button';
import { Card, CardContent } from '../../ui/card';
import { Avatar, AvatarFallback, AvatarImage } from '../../ui/avatar';
import { Badge } from '../../ui/badge';
import { Progress } from '../../ui/progress';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../ui/select';

interface MemberStats {
  id: string;
  name: string;
  avatar?: string;
  totalEvents: number;
  attendedEvents: number;
  attendanceRate: number;
  recentAttendance: boolean[]; // 최근 5개 일정
  trend: 'up' | 'down' | 'stable';
  rank: number;
}

export function ParticipationStatsView() {
  const navigate = useNavigate();
  const [period, setPeriod] = useState('3months');
  const [sortBy, setSortBy] = useState<'rate' | 'count'>('rate');

  // Mock data
  const totalEvents = 12;
  const avgAttendance = 78;
  
  const memberStats: MemberStats[] = [
    {
      id: '1',
      name: '홍길동',
      avatar: '',
      totalEvents: 12,
      attendedEvents: 12,
      attendanceRate: 100,
      recentAttendance: [true, true, true, true, true],
      trend: 'stable',
      rank: 1,
    },
    {
      id: '2',
      name: '김철수',
      avatar: '',
      totalEvents: 12,
      attendedEvents: 11,
      attendanceRate: 92,
      recentAttendance: [true, true, true, false, true],
      trend: 'up',
      rank: 2,
    },
    {
      id: '3',
      name: '이영희',
      avatar: '',
      totalEvents: 12,
      attendedEvents: 10,
      attendanceRate: 83,
      recentAttendance: [true, false, true, true, true],
      trend: 'up',
      rank: 3,
    },
    {
      id: '4',
      name: '박민수',
      avatar: '',
      totalEvents: 12,
      attendedEvents: 9,
      attendanceRate: 75,
      recentAttendance: [false, true, true, true, false],
      trend: 'down',
      rank: 4,
    },
    {
      id: '5',
      name: '정지훈',
      avatar: '',
      totalEvents: 12,
      attendedEvents: 7,
      attendanceRate: 58,
      recentAttendance: [false, false, true, true, true],
      trend: 'up',
      rank: 5,
    },
    {
      id: '6',
      name: '최유진',
      avatar: '',
      totalEvents: 12,
      attendedEvents: 5,
      attendanceRate: 42,
      recentAttendance: [false, false, false, true, true],
      trend: 'up',
      rank: 6,
    },
  ];

  const sortedStats = [...memberStats].sort((a, b) => {
    if (sortBy === 'rate') return b.attendanceRate - a.attendanceRate;
    return b.attendedEvents - a.attendedEvents;
  });

  const getTrendIcon = (trend: string) => {
    switch (trend) {
      case 'up':
        return <TrendingUp className="w-4 h-4 text-green-500" />;
      case 'down':
        return <TrendingDown className="w-4 h-4 text-red-500" />;
      default:
        return <Minus className="w-4 h-4 text-stone-400" />;
    }
  };

  const getRankBadge = (rank: number) => {
    if (rank === 1) return '🥇';
    if (rank === 2) return '🥈';
    if (rank === 3) return '🥉';
    return `${rank}위`;
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center px-4 py-3">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigate(-1)}
            className="-ml-2"
          >
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="ml-2 text-lg font-semibold text-stone-800">참여 통계</h1>
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Period Selector */}
        <div className="flex justify-end">
          <Select value={period} onValueChange={setPeriod}>
            <SelectTrigger className="w-32 h-9 bg-white">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="1month">최근 1개월</SelectItem>
              <SelectItem value="3months">최근 3개월</SelectItem>
              <SelectItem value="6months">최근 6개월</SelectItem>
              <SelectItem value="1year">최근 1년</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* Overview Cards */}
        <div className="grid grid-cols-2 gap-4">
          <Card className="border-stone-100">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                  <Calendar className="w-5 h-5 text-blue-600" />
                </div>
                <div>
                  <p className="text-sm text-stone-500">총 일정</p>
                  <p className="text-2xl font-bold text-stone-900">{totalEvents}회</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="border-stone-100">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
                  <Users className="w-5 h-5 text-green-600" />
                </div>
                <div>
                  <p className="text-sm text-stone-500">평균 참석률</p>
                  <p className="text-2xl font-bold text-stone-900">{avgAttendance}%</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Top 3 */}
        <Card className="border-stone-100 overflow-hidden">
          <div className="bg-gradient-to-r from-orange-500 to-orange-400 p-4">
            <div className="flex items-center gap-2 text-white">
              <Award className="w-5 h-5" />
              <h3 className="font-bold">참석왕 TOP 3</h3>
            </div>
          </div>
          <CardContent className="p-4">
            <div className="flex justify-center gap-4">
              {sortedStats.slice(0, 3).map((member, i) => (
                <div key={member.id} className={`text-center ${i === 0 ? 'order-2' : i === 1 ? 'order-1' : 'order-3'}`}>
                  <div className={`relative ${i === 0 ? 'scale-110' : ''}`}>
                    <Avatar className={`${i === 0 ? 'w-16 h-16' : 'w-12 h-12'} border-4 ${
                      i === 0 ? 'border-yellow-400' : i === 1 ? 'border-stone-300' : 'border-orange-300'
                    }`}>
                      <AvatarImage src={member.avatar} />
                      <AvatarFallback>{member.name[0]}</AvatarFallback>
                    </Avatar>
                    <span className="absolute -bottom-2 left-1/2 -translate-x-1/2 text-lg">
                      {getRankBadge(i + 1)}
                    </span>
                  </div>
                  <p className="mt-3 text-sm font-medium text-stone-900">{member.name}</p>
                  <p className="text-xs text-orange-600">{member.attendanceRate}%</p>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Sort Options */}
        <div className="flex gap-2">
          <Button
            variant={sortBy === 'rate' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setSortBy('rate')}
            className={sortBy === 'rate' ? 'bg-orange-500 hover:bg-orange-600' : ''}
          >
            참석률순
          </Button>
          <Button
            variant={sortBy === 'count' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setSortBy('count')}
            className={sortBy === 'count' ? 'bg-orange-500 hover:bg-orange-600' : ''}
          >
            참석 횟수순
          </Button>
        </div>

        {/* Member List */}
        <div className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
          <div className="p-4 bg-stone-50 border-b border-stone-100 text-sm text-stone-500">
            멤버별 상세 통계
          </div>
          <div className="divide-y divide-stone-100">
            {sortedStats.map((member, index) => (
              <div key={member.id} className="p-4">
                <div className="flex items-center gap-3 mb-3">
                  <span className="w-6 text-center font-bold text-stone-400">
                    {index < 3 ? getRankBadge(index + 1) : `${index + 1}`}
                  </span>
                  <Avatar className="w-10 h-10">
                    <AvatarImage src={member.avatar} />
                    <AvatarFallback>{member.name[0]}</AvatarFallback>
                  </Avatar>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <p className="font-medium text-stone-900">{member.name}</p>
                      {getTrendIcon(member.trend)}
                    </div>
                    <p className="text-xs text-stone-500">
                      {member.attendedEvents}/{member.totalEvents}회 참석
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-bold text-orange-600">{member.attendanceRate}%</p>
                  </div>
                </div>
                
                {/* Progress Bar */}
                <div className="ml-9 mb-3">
                  <Progress value={member.attendanceRate} className="h-2" />
                </div>

                {/* Recent Attendance */}
                <div className="ml-9 flex items-center gap-2">
                  <span className="text-xs text-stone-500">최근 5회:</span>
                  <div className="flex gap-1">
                    {member.recentAttendance.map((attended, i) => (
                      <div
                        key={i}
                        className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-medium ${
                          attended 
                            ? 'bg-green-100 text-green-700' 
                            : 'bg-red-100 text-red-700'
                        }`}
                      >
                        {attended ? 'O' : 'X'}
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}



