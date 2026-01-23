import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Plus, Clock, Users, Check, BarChart2, Filter } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Badge } from '../../ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../ui/card';
import { getVotes, type VoteListResponse } from '../../../../api/vote';
import { useUserPermissions } from '../../../data/userRoles';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../ui/select';

export function VoteListView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [votes, setVotes] = useState<VoteListResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState<'ALL' | 'GENERAL' | 'ATTENDANCE'>('ALL');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'OPEN' | 'CLOSED'>('ALL');

  useEffect(() => {
    async function fetchVotes() {
      if (!groupId) return;
      try {
        setLoading(true);
        const votesData = await getVotes(Number(groupId));
        setVotes(votesData);
      } catch (error) {
        console.error('투표 목록 불러오기 실패:', error);
        toast.error('투표 목록을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    }
    fetchVotes();
  }, [groupId]);

  // 필터링된 투표 목록
  const filteredVotes = votes.filter((vote) => {
    if (typeFilter !== 'ALL' && vote.voteType !== typeFilter) return false;
    if (statusFilter !== 'ALL' && vote.status !== statusFilter) return false;
    return true;
  });

  // 정렬: 최신순 (생성일 기준 내림차순)
  const sortedVotes = [...filteredVotes].sort((a, b) => {
    const dateA = new Date(a.createdAt).getTime();
    const dateB = new Date(b.createdAt).getTime();
    return dateB - dateA;
  });

  const getVoteTypeLabel = (type: string) => {
    return type === 'GENERAL' ? '일반 투표' : '참석 투표';
  };

  const getStatusLabel = (status: string) => {
    return status === 'OPEN' ? '진행 중' : '종료';
  };

  const getStatusColor = (status: string) => {
    return status === 'OPEN' ? 'bg-green-500' : 'bg-gray-500';
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const formatDateTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 pb-24" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigate(-1)}
              className="-ml-2"
            >
              <ArrowLeft className="w-6 h-6 text-stone-800" />
            </Button>
            <h1 className="ml-2 text-lg font-semibold text-stone-800">투표 목록</h1>
          </div>
          {permissions.canManageGroup && (
            <Button
              variant="default"
              size="sm"
              onClick={() => navigate(`/group/${groupId}/stories/create`)}
              className="bg-orange-500 hover:bg-orange-600"
            >
              <Plus className="w-4 h-4 mr-1" />
              투표 생성
            </Button>
          )}
        </div>
      </header>

      {/* 필터 */}
      <div className="px-4 py-3 bg-white border-b border-stone-100">
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-stone-500" />
          <Select value={typeFilter} onValueChange={(value) => setTypeFilter(value as typeof typeFilter)}>
            <SelectTrigger className="w-[140px]">
              <SelectValue placeholder="타입" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">전체 타입</SelectItem>
              <SelectItem value="GENERAL">일반 투표</SelectItem>
              <SelectItem value="ATTENDANCE">참석 투표</SelectItem>
            </SelectContent>
          </Select>
          <Select value={statusFilter} onValueChange={(value) => setStatusFilter(value as typeof statusFilter)}>
            <SelectTrigger className="w-[140px]">
              <SelectValue placeholder="상태" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">전체 상태</SelectItem>
              <SelectItem value="OPEN">진행 중</SelectItem>
              <SelectItem value="CLOSED">종료</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* 투표 목록 */}
      <div className="px-4 py-4 space-y-3">
        {sortedVotes.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-stone-500">투표가 없습니다</p>
            {permissions.canManageGroup && (
              <Button
                variant="outline"
                className="mt-4"
                onClick={() => navigate(`/group/${groupId}/stories/create`)}
              >
                <Plus className="w-4 h-4 mr-1" />
                첫 투표 만들기
              </Button>
            )}
          </div>
        ) : (
          sortedVotes.map((vote) => {
            const deadline = vote.deadline ? new Date(vote.deadline) : null;
            const isDeadlinePassed = deadline ? deadline.getTime() < new Date().getTime() : false;
            const daysLeft = deadline
              ? Math.ceil((deadline.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24))
              : null;

            return (
              <Card
                key={vote.voteId}
                className="cursor-pointer hover:shadow-md transition-shadow"
                onClick={() => {
                  if (vote.voteType === 'ATTENDANCE' && vote.scheduleId) {
                    navigate(`/group/${groupId}/schedule/${vote.scheduleId}`);
                  } else {
                    navigate(`/group/${groupId}/vote/${vote.voteId}`);
                  }
                }}
              >
                <CardHeader className="pb-3">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <CardTitle className="text-base font-semibold text-stone-900 mb-1">
                        {vote.title}
                      </CardTitle>
                    </div>
                    <Badge className={`${getStatusColor(vote.status)} text-white ml-2 shrink-0`}>
                      {getStatusLabel(vote.status)}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center justify-between text-sm text-stone-500">
                    <div className="flex items-center gap-4">
                      <div className="flex items-center gap-1">
                        <BarChart2 className="w-4 h-4" />
                        <span>{getVoteTypeLabel(vote.voteType)}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <Users className="w-4 h-4" />
                        <span>투표 참여</span>
                      </div>
                      {deadline && (
                        <div className="flex items-center gap-1">
                          <Clock className="w-4 h-4" />
                          <span>
                            {isDeadlinePassed
                              ? '마감됨'
                              : daysLeft !== null && daysLeft > 0
                              ? `${daysLeft}일 남음`
                              : '오늘 마감'}
                          </span>
                        </div>
                      )}
                    </div>
                    <div className="text-xs text-stone-400">
                      {formatDate(vote.createdAt)}
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })
        )}
      </div>
    </div>
  );
}
