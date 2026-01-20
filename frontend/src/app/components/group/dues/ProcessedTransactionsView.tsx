import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Calendar, CheckCircle, XCircle, Clock } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Badge } from '../../ui/badge';
import { Card, CardContent } from '../../ui/card';
import { getProcessedTransactions, type ProcessedTransactionResponse } from '../../../../api/bank';
import { useUserPermissions } from '../../../data/userRoles';

export function ProcessedTransactionsView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [transactions, setTransactions] = useState<ProcessedTransactionResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  // 날짜 기본값 설정
  useEffect(() => {
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
    setFromDate(firstDay.toISOString().split('T')[0]);
    setToDate(today.toISOString().split('T')[0]);
  }, []);

  const fetchTransactions = async () => {
    if (!groupId || !fromDate || !toDate) return;
    try {
      setLoading(true);
      const data = await getProcessedTransactions(Number(groupId), fromDate, toDate);
      setTransactions(data);
    } catch (error) {
      console.error('처리된 거래내역 조회 실패:', error);
      toast.error('거래내역을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (fromDate && toDate) {
      fetchTransactions();
    }
  }, [fromDate, toDate]);

  const getMatchTypeBadge = (matchType: string) => {
    switch (matchType) {
      case 'AUTO_MATCHED':
        return <Badge className="bg-green-100 text-green-700">자동 매칭</Badge>;
      case 'CONFIRMED':
        return <Badge className="bg-blue-100 text-blue-700">수동 확인</Badge>;
      case 'UNMATCHED':
        return <Badge className="bg-yellow-100 text-yellow-700">미매칭</Badge>;
      default:
        return <Badge variant="secondary">{matchType}</Badge>;
    }
  };

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
          <p className="text-stone-500">처리된 거래내역 조회는 총무 이상만 가능합니다.</p>
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
          <h1 className="ml-2 text-lg font-semibold text-stone-800">처리된 거래내역</h1>
        </div>
      </header>

      <div className="p-5 space-y-4">
        {/* 날짜 필터 */}
        <Card className="border-stone-100">
          <CardContent className="p-4 space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label className="text-xs text-stone-500">시작일</Label>
                <Input
                  type="date"
                  value={fromDate}
                  onChange={(e) => setFromDate(e.target.value)}
                  className="h-10"
                />
              </div>
              <div className="space-y-1">
                <Label className="text-xs text-stone-500">종료일</Label>
                <Input
                  type="date"
                  value={toDate}
                  onChange={(e) => setToDate(e.target.value)}
                  className="h-10"
                />
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 거래내역 목록 */}
        {loading ? (
          <div className="text-center py-8 text-stone-500">로딩 중...</div>
        ) : transactions.length === 0 ? (
          <Card>
            <CardContent className="p-8 text-center">
              <p className="text-stone-500">거래내역이 없습니다</p>
            </CardContent>
          </Card>
        ) : (
          <div className="space-y-3">
            {transactions.map((tx) => {
              const isDeposit = tx.type === 'DEPOSIT';
              return (
                <Card key={tx.historyId} className="border-stone-100">
                  <CardContent className="p-4">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-2">
                          {getMatchTypeBadge(tx.matchType)}
                          {tx.matchedMemberName && (
                            <Badge variant="secondary" className="text-xs">
                              {tx.matchedMemberName}
                            </Badge>
                          )}
                        </div>
                        <p className="font-medium text-stone-900 mb-1">
                          {tx.printContent}
                        </p>
                        <div className="flex items-center gap-2 text-xs text-stone-500">
                          <Calendar className="w-3 h-3" />
                          <span>{formatDate(tx.occurredAt)}</span>
                        </div>
                        {tx.matchedRequestType && (
                          <p className="text-xs text-stone-500 mt-1">
                            요청 유형: {tx.matchedRequestType}
                          </p>
                        )}
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
                        <p className="text-xs text-stone-500 mt-1">
                          잔액: {tx.balanceAfter.toLocaleString()}원
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
    </div>
  );
}
