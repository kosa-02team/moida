import { Link } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../../ui/card';
import { Users, ChevronRight } from 'lucide-react';

export function StatsView() {

  return (
    <div className="space-y-6 pb-20" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
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
          </CardContent>
        </Card>
      </Link>
    </div>
  );
}
