import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, AlertCircle, Calendar, User } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Badge } from '../../ui/badge';
import { Card, CardContent } from '../../ui/card';
import { getUnmatchedTransactions, type UnmatchedTransactionsResponse } from '../../../../api/bank';
import { useUserPermissions } from '../../../data/userRoles';

export function UnmatchedTransactionsView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [data, setData] = useState<UnmatchedTransactionsResponse | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    async function fetchData() {
      if (!groupId) return;
      try {
        setLoading(true);
        const result = await getUnmatchedTransactions(Number(groupId));
        setData(result);
      } catch (error) {
        console.error('미매칭 거래내역 조회 실패:', error);
        toast.error('거래내역을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, [groupId]);

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // 권한 체크
  if (!permissions.canWithdraw) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-stone-500">미매칭 거래내역 조회는 총무 이상만 가능합니다.</p>
          <Button onClick={() => navigate(-1)} className="mt-4">
            돌아가기
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 pb-20" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center px-4 py-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="ml-2 text-lg font-semibold text-stone-800">미매칭 거래내역</h1>
        </div>
      </header>

      <div className="p-5 space-y-4">
        {/* 미매칭 거래내역 */}
        <div>
          <h2 className="font-medium text-stone-900 mb-3">
            미매칭 거래내역 ({data?.unmatchedTransactions.length || 0}개)
          </h2>
          {loading ? (
            <div className="text-center py-8 text-stone-500">로딩 중...</div>
          ) : !data || data.unmatchedTransactions.length === 0 ? (
            <Card>
              <CardContent className="p-8 text-center">
                <AlertCircle className="w-12 h-12 text-stone-300 mx-auto mb-3" />
                <p className="text-stone-500">미매칭 거래내역이 없습니다</p>
              </CardContent>
            </Card>
          ) : (
            <div className="space-y-3">
              {data.unmatchedTransactions.map((tx) => {
                const isDeposit = tx.amount > 0;
                return (
                  <Card key={tx.historyId} className="border-yellow-200 bg-yellow-50">
                    <CardContent className="p-4">
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-2">
                            <Badge className="bg-yellow-100 text-yellow-700">미매칭</Badge>
                          </div>
                          <p className="font-medium text-stone-900 mb-1">
                            {tx.printContent}
                          </p>
                          <div className="flex items-center gap-2 text-xs text-stone-500">
                            <Calendar className="w-3 h-3" />
                            <span>{formatDate(tx.bankTransactionAt)}</span>
                          </div>
                        </div>
                        <div className="text-right">
                          <p
                            className={`font-bold text-lg ${
                              isDeposit ? 'text-blue-600' : 'text-stone-900'
                            }`}
                          >
                            {isDeposit ? '+' : '-'}
                            {Math.abs(tx.amount).toLocaleString()}원
                          </p>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                );
              })}
            </div>
          )}
        </div>

        {/* 매칭 가능한 입금요청 */}
        {data && data.availableRequests.length > 0 && (
          <div>
            <h2 className="font-medium text-stone-900 mb-3">
              매칭 가능한 입금요청 ({data.availableRequests.length}개)
            </h2>
            <div className="space-y-3">
              {data.availableRequests.map((request) => (
                <Card key={request.requestId} className="border-blue-200 bg-blue-50">
                  <CardContent className="p-4">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-2">
                          <Badge className="bg-blue-100 text-blue-700">대기중</Badge>
                          <span className="text-sm font-medium text-stone-900">
                            {request.memberName}
                          </span>
                        </div>
                        <p className="text-xs text-stone-600 mb-1">
                          예상일: {new Date(request.expectedDate).toLocaleDateString('ko-KR')}
                        </p>
                        <p className="text-xs text-stone-500">
                          유형: {request.requestType}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="font-bold text-lg text-stone-900">
                          {request.expectedAmount.toLocaleString()}원
                        </p>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
