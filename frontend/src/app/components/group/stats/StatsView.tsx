import { Link } from 'react-router-dom';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '../../ui/card';
import { Users, TrendingUp, ChevronRight } from 'lucide-react';

export function StatsView() {
  const duesData = [
    { name: '입금 완료', value: 12 },
    { name: '미입금', value: 3 },
  ];
  
  const COLORS = ['#f97316', '#e5e7eb']; // Orange-500, Gray-200

  // 참여 통계 미리보기
  const participationPreview = {
    totalEvents: 12,
    avgAttendance: 78,
    topMembers: [
      { name: '홍길동', rate: 100 },
      { name: '김철수', rate: 92 },
      { name: '이영희', rate: 83 },
    ]
  };

  return (
    <div className="space-y-6 pb-20">
      {/* 회비 납부 통계 */}
      <Card className="border-stone-100 shadow-sm">
        <CardHeader>
          <CardTitle className="text-lg">이번 달 회비 납부 현황</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={duesData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {duesData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend verticalAlign="bottom" height={36}/>
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="text-center mt-4">
            <p className="text-sm text-stone-500">총 15명 중 <span className="font-bold text-orange-500">12명</span> 납부 완료</p>
            <p className="text-xs text-stone-400 mt-1">납부율 80%</p>
          </div>
        </CardContent>
      </Card>

      {/* 미납 멤버 */}
      <div className="space-y-3">
        <h3 className="font-bold text-stone-800 px-1">미납 멤버 (3명)</h3>
        <div className="bg-white rounded-xl border border-stone-100 p-4 space-y-3">
          {['박지성', '이영표', '손흥민'].map((name, i) => (
            <div key={i} className="flex justify-between items-center">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-stone-200 flex items-center justify-center text-xs text-stone-500">
                  {name[0]}
                </div>
                <span className="text-stone-900">{name}</span>
              </div>
              <button className="text-xs text-orange-500 font-medium px-3 py-1 bg-orange-50 rounded-full hover:bg-orange-100">
                독촉하기
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* 참여 통계 카드 - 링크 */}
      <Link to="participation">
        <Card className="border-stone-100 shadow-sm hover:border-orange-200 transition-colors cursor-pointer">
          <CardContent className="p-5">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-blue-100 rounded-lg">
                  <Users className="w-5 h-5 text-blue-600" />
                </div>
                <div>
                  <h3 className="font-bold text-stone-900">참여 통계</h3>
                  <p className="text-xs text-stone-500">모임 일정 참석 현황</p>
                </div>
              </div>
              <ChevronRight className="w-5 h-5 text-stone-300" />
            </div>

            {/* Preview */}
            <div className="grid grid-cols-2 gap-4 mb-4">
              <div className="bg-stone-50 rounded-lg p-3 text-center">
                <p className="text-2xl font-bold text-stone-900">{participationPreview.totalEvents}회</p>
                <p className="text-xs text-stone-500">총 일정</p>
              </div>
              <div className="bg-stone-50 rounded-lg p-3 text-center">
                <p className="text-2xl font-bold text-orange-600">{participationPreview.avgAttendance}%</p>
                <p className="text-xs text-stone-500">평균 참석률</p>
              </div>
            </div>

            {/* Top 3 Preview */}
            <div className="flex items-center justify-between text-sm">
              <span className="text-stone-500">참석왕 TOP 3</span>
              <div className="flex items-center gap-2">
                {participationPreview.topMembers.map((member, i) => (
                  <span key={i} className="flex items-center gap-1 bg-orange-50 text-orange-700 px-2 py-0.5 rounded text-xs">
                    {i === 0 ? '🥇' : i === 1 ? '🥈' : '🥉'}
                    {member.name}
                  </span>
                ))}
              </div>
            </div>
          </CardContent>
        </Card>
      </Link>

      {/* 월별 트렌드 (간단한 요약) */}
      <Card className="border-stone-100 shadow-sm">
        <CardHeader>
          <div className="flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-green-600" />
            <CardTitle className="text-lg">월별 트렌드</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-stone-600">4월</span>
              <div className="flex-1 mx-4 h-3 bg-stone-100 rounded-full overflow-hidden">
                <div className="h-full bg-orange-500 rounded-full" style={{ width: '80%' }}></div>
              </div>
              <span className="text-sm font-medium text-stone-900">80%</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-stone-600">3월</span>
              <div className="flex-1 mx-4 h-3 bg-stone-100 rounded-full overflow-hidden">
                <div className="h-full bg-orange-500 rounded-full" style={{ width: '75%' }}></div>
              </div>
              <span className="text-sm font-medium text-stone-900">75%</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-stone-600">2월</span>
              <div className="flex-1 mx-4 h-3 bg-stone-100 rounded-full overflow-hidden">
                <div className="h-full bg-orange-500 rounded-full" style={{ width: '85%' }}></div>
              </div>
              <span className="text-sm font-medium text-stone-900">85%</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
