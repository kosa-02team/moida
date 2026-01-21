import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, RefreshCw, Calendar } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Card, CardContent } from '../../ui/card';
import { syncBankTransactions } from '../../../../api/bank';
import { useUserPermissions } from '../../../data/userRoles';

export function BankSyncView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncResult, setSyncResult] = useState<{ count: number; transactions: any[] } | null>(null);

  // 날짜 기본값 설정 (이번 달 1일 ~ 오늘)
  useEffect(() => {
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
    setFromDate(firstDay.toISOString().split('T')[0]);
    setToDate(today.toISOString().split('T')[0]);
  }, []);

  const handleSync = async () => {
    if (!groupId) return;

    try {
      setIsSyncing(true);
      const transactions = await syncBankTransactions(
        Number(groupId),
        fromDate || undefined,
        toDate || undefined
      );
      setSyncResult({
        count: transactions.length,
        transactions: transactions.slice(0, 5), // 최근 5개만 표시
      });
      toast.success(`${transactions.length}개의 거래내역이 동기화되었습니다.`);
    } catch (error) {
      console.error('거래내역 동기화 실패:', error);
      toast.error('거래내역 동기화에 실패했습니다.');
    } finally {
      setIsSyncing(false);
    }
  };

  // 권한 체크
  if (!permissions.canWithdraw) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-stone-500">거래내역 동기화는 총무 이상만 가능합니다.</p>
          <Button onClick={() => navigate(-1)} className="mt-4">
            돌아가기
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 pb-32" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center px-4 py-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="ml-2 text-lg font-semibold text-stone-800">거래내역 동기화</h1>
        </div>
      </header>

      <div className="p-5 space-y-4">
        <Card className="border-stone-100">
          <CardContent className="p-5 space-y-4">
            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <Calendar className="w-4 h-4" />
                시작일
              </Label>
              <Input
                type="date"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
              />
              <p className="text-xs text-stone-500">
                비워두면 마지막 거래 이후부터 자동으로 동기화됩니다.
              </p>
            </div>

            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <Calendar className="w-4 h-4" />
                종료일
              </Label>
              <Input
                type="date"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
              />
              <p className="text-xs text-stone-500">
                비워두면 오늘까지 자동으로 동기화됩니다.
              </p>
            </div>
          </CardContent>
        </Card>

        {syncResult && (
          <Card className="border-green-200 bg-green-50">
            <CardContent className="p-5">
              <div className="flex items-center gap-2 mb-3">
                <RefreshCw className="w-5 h-5 text-green-600" />
                <h3 className="font-medium text-green-900">동기화 완료</h3>
              </div>
              <p className="text-sm text-green-700 mb-2">
                총 {syncResult.count}개의 거래내역이 동기화되었습니다.
              </p>
              {syncResult.transactions.length > 0 && (
                <div className="space-y-2 mt-3">
                  <p className="text-xs font-medium text-green-800">최근 거래내역:</p>
                  {syncResult.transactions.map((tx, idx) => (
                    <div key={idx} className="text-xs text-green-700 bg-white/50 p-2 rounded">
                      {tx.description || (tx.type === 'DEPOSIT' ? '입금' : '출금')} - {Math.abs(tx.amount).toLocaleString()}원
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        )}
      </div>

      {/* Bottom Button */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-stone-100 p-4">
        <Button
          onClick={handleSync}
          disabled={isSyncing}
          className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl"
        >
          <RefreshCw className={`w-5 h-5 mr-2 ${isSyncing ? 'animate-spin' : ''}`} />
          {isSyncing ? '동기화 중...' : '거래내역 동기화'}
        </Button>
      </div>
    </div>
  );
}
