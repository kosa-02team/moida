import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, ArrowDownLeft, ArrowUpRight, Calendar, Filter, Plus, Edit2 } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Badge } from '../../ui/badge';
import { Textarea } from '../../ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../ui/dialog';
import { getLedger, createManualTransaction, updateTransaction, type TransactionLogResponse, type ManualTransactionRequest, type TransactionUpdateRequest } from '../../../../api/ledger';
import { useUserPermissions } from '../../../data/userRoles';

export function LedgerView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [transactions, setTransactions] = useState<TransactionLogResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState<TransactionLogResponse | null>(null);
  
  // 수동 기록 추가 폼 상태
  const [manualDate, setManualDate] = useState('');
  const [manualContent, setManualContent] = useState('');
  const [manualAmount, setManualAmount] = useState('');
  const [manualType, setManualType] = useState<'DEPOSIT' | 'WITHDRAW'>('DEPOSIT');
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  // 수정 폼 상태
  const [editMemo, setEditMemo] = useState('');

  useEffect(() => {
    async function fetchTransactions() {
      if (!groupId) return;
      try {
        setLoading(true);
        const data = await getLedger(
          Number(groupId),
          startDate || undefined,
          endDate || undefined
        );
        setTransactions(data);
      } catch (error) {
        console.error('거래 내역 조회 실패:', error);
        toast.error('거래 내역을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    }
    fetchTransactions();
  }, [groupId, startDate, endDate]);

  // 날짜 기본값 설정 (이번 달 1일 ~ 오늘)
  useEffect(() => {
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
    setStartDate(firstDay.toISOString().split('T')[0]);
    setEndDate(today.toISOString().split('T')[0]);
    setManualDate(today.toISOString().split('T')[0]);
  }, []);

  const handleAddManualTransaction = async () => {
    if (!groupId || !manualDate || !manualContent.trim() || !manualAmount) {
      toast.error('모든 필드를 입력해주세요.');
      return;
    }
    
    const amount = parseFloat(manualAmount);
    if (isNaN(amount) || amount <= 0) {
      toast.error('올바른 금액을 입력해주세요.');
      return;
    }

    try {
      setIsSubmitting(true);
      const request: ManualTransactionRequest = {
        occurredAt: manualDate,
        content: manualContent.trim(),
        amount: amount,
        type: manualType,
      };
      
      await createManualTransaction(Number(groupId), request);
      toast.success('거래 내역이 추가되었습니다.');
      setShowAddDialog(false);
      setManualContent('');
      setManualAmount('');
      setManualType('DEPOSIT');
      // 목록 새로고침
      const data = await getLedger(
        Number(groupId),
        startDate || undefined,
        endDate || undefined
      );
      setTransactions(data);
    } catch (error) {
      console.error('수동 기록 추가 실패:', error);
      toast.error('거래 내역 추가에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleStartEdit = (tx: TransactionLogResponse) => {
    setEditingTransaction(tx);
    setEditMemo(tx.description || '');
    setShowEditDialog(true);
  };

  const handleUpdateTransaction = async () => {
    if (!groupId || !editingTransaction) return;
    
    try {
      setIsSubmitting(true);
      const request: TransactionUpdateRequest = {
        memo: editMemo.trim(),
      };
      
      await updateTransaction(Number(groupId), editingTransaction.transactionId, request);
      toast.success('거래 내역이 수정되었습니다.');
      setShowEditDialog(false);
      setEditingTransaction(null);
      setEditMemo('');
      // 목록 새로고침
      const data = await getLedger(
        Number(groupId),
        startDate || undefined,
        endDate || undefined
      );
      setTransactions(data);
    } catch (error) {
      console.error('거래 내역 수정 실패:', error);
      toast.error('거래 내역 수정에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
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

  const formatAmount = (amount: number) => {
    return amount.toLocaleString();
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-20" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center px-4 py-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="ml-2 text-lg font-semibold text-stone-800">장부 관리</h1>
        </div>
      </header>

      <div className="p-5 space-y-4">
        {/* 날짜 필터 및 수동 기록 추가 버튼 */}
        <div className="bg-white rounded-xl p-4 border border-stone-100">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Filter className="w-5 h-5 text-stone-500" />
              <Label className="font-medium text-stone-900">기간 선택</Label>
            </div>
            {permissions.canWithdraw && (
              <Button
                onClick={() => setShowAddDialog(true)}
                size="sm"
                className="bg-orange-500 hover:bg-orange-600"
              >
                <Plus className="w-4 h-4 mr-1" />
                수동 기록
              </Button>
            )}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <Label className="text-xs text-stone-500">시작일</Label>
              <Input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="h-10"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-stone-500">종료일</Label>
              <Input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="h-10"
              />
            </div>
          </div>
        </div>

        {/* 거래 내역 목록 */}
        {loading ? (
          <div className="text-center py-8 text-stone-500">로딩 중...</div>
        ) : transactions.length === 0 ? (
          <div className="bg-white rounded-xl p-8 text-center border border-stone-100">
            <p className="text-stone-500">거래 내역이 없습니다</p>
          </div>
        ) : (
          <div className="bg-white rounded-xl border border-stone-100 overflow-hidden">
            <div className="divide-y divide-stone-100">
              {transactions.map((tx) => {
                const isDeposit = tx.type === 'DEPOSIT';
                return (
                  <div key={tx.transactionId} className="p-4">
                    <div className="flex items-start justify-between">
                      <div className="flex items-start gap-3 flex-1">
                        <div
                          className={`w-10 h-10 rounded-full flex items-center justify-center ${
                            isDeposit
                              ? 'bg-blue-50 text-blue-600'
                              : 'bg-red-50 text-red-600'
                          }`}
                        >
                          {isDeposit ? (
                            <ArrowDownLeft className="w-5 h-5" />
                          ) : (
                            <ArrowUpRight className="w-5 h-5" />
                          )}
                        </div>
                        <div className="flex-1">
                          <div className="flex items-start justify-between">
                            <div className="flex-1">
                              <p className="font-medium text-stone-900">
                                {tx.description || (isDeposit ? '입금' : '출금')}
                              </p>
                              <div className="flex items-center gap-2 mt-1">
                                <Calendar className="w-3 h-3 text-stone-400" />
                                <p className="text-xs text-stone-500">
                                  {formatDate(tx.createdAt)}
                                </p>
                              </div>
                              {tx.scheduleId && (
                                <Badge variant="secondary" className="mt-1 text-xs">
                                  일정 #{tx.scheduleId}
                                </Badge>
                              )}
                            </div>
                            {permissions.canWithdraw && (
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleStartEdit(tx)}
                                className="h-8 w-8 p-0"
                              >
                                <Edit2 className="w-4 h-4 text-stone-400" />
                              </Button>
                            )}
                          </div>
                        </div>
                      </div>
                      <div className="text-right">
                        <p
                          className={`font-bold text-lg ${
                            isDeposit ? 'text-blue-600' : 'text-stone-900'
                          }`}
                        >
                          {isDeposit ? '+' : '-'}
                          {formatAmount(Math.abs(tx.amount))}원
                        </p>
                        <p className="text-xs text-stone-500 mt-1">
                          잔액: {formatAmount(tx.balanceAfter)}원
                        </p>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>

      {/* 수동 기록 추가 다이얼로그 */}
      <Dialog open={showAddDialog} onOpenChange={setShowAddDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>수동 거래 내역 추가</DialogTitle>
            <DialogDescription>
              모임 통장의 거래 내역을 수동으로 기록합니다.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4 space-y-4">
            <div className="space-y-2">
              <Label htmlFor="manual-date">거래일</Label>
              <Input
                id="manual-date"
                type="date"
                value={manualDate}
                onChange={(e) => setManualDate(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="manual-type">유형</Label>
              <Select value={manualType} onValueChange={(value: 'DEPOSIT' | 'WITHDRAW') => setManualType(value)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="DEPOSIT">입금</SelectItem>
                  <SelectItem value="WITHDRAW">출금</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="manual-content">내용</Label>
              <Textarea
                id="manual-content"
                placeholder="거래 내용을 입력하세요"
                value={manualContent}
                onChange={(e) => setManualContent(e.target.value)}
                maxLength={200}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="manual-amount">금액</Label>
              <Input
                id="manual-amount"
                type="number"
                placeholder="0"
                value={manualAmount}
                onChange={(e) => setManualAmount(e.target.value)}
                min="1"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowAddDialog(false)}>
              취소
            </Button>
            <Button
              onClick={handleAddManualTransaction}
              disabled={!manualDate || !manualContent.trim() || !manualAmount || isSubmitting}
              className="bg-orange-500 hover:bg-orange-600"
            >
              {isSubmitting ? '추가 중...' : '추가'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 거래 내역 수정 다이얼로그 */}
      <Dialog open={showEditDialog} onOpenChange={setShowEditDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>거래 내역 수정</DialogTitle>
            <DialogDescription>
              거래 내역의 메모를 수정할 수 있습니다.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4 space-y-4">
            <div className="space-y-2">
              <Label htmlFor="edit-memo">메모</Label>
              <Textarea
                id="edit-memo"
                placeholder="메모를 입력하세요"
                value={editMemo}
                onChange={(e) => setEditMemo(e.target.value)}
                maxLength={200}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowEditDialog(false)}>
              취소
            </Button>
            <Button
              onClick={handleUpdateTransaction}
              disabled={isSubmitting}
              className="bg-orange-500 hover:bg-orange-600"
            >
              {isSubmitting ? '수정 중...' : '수정'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
