import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { ArrowDownLeft, ArrowUpRight, Calendar, Filter, Plus, Edit2, AlertCircle, Link2 } from 'lucide-react';
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
import { 
  getLedger, 
  createManualTransaction, 
  updateTransaction, 
  getUnmatchedTransactions,
  manualMatch,
  manualMatchMultiple,
  type TransactionLogResponse, 
  type ManualTransactionRequest, 
  type TransactionUpdateRequest,
  type UnmatchedTransactionsResponse,
  type BankTransactionHistory,
} from '../../../../api/ledger';
import { useUserPermissions } from '../../../data/userRoles';
import { getMyInfo } from '../../../../api/user';

export function LedgerView() {
  const { groupId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [transactions, setTransactions] = useState<TransactionLogResponse[]>([]);
  const [unmatchedData, setUnmatchedData] = useState<UnmatchedTransactionsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [showMatchDialog, setShowMatchDialog] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState<TransactionLogResponse | null>(null);
  const [selectedUnmatched, setSelectedUnmatched] = useState<BankTransactionHistory[]>([]);
  const [selectedRequest, setSelectedRequest] = useState<string>('');
  
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
        
        // 미매칭 거래도 함께 조회
        if (permissions.canWithdraw) {
          const unmatchedData = await getUnmatchedTransactions(Number(groupId));
          console.log('📊 초기 로드 - 미매칭 거래:', unmatchedData.unmatchedTransactions.length);
          console.log('📊 초기 로드 - 입금요청:', unmatchedData.availableRequests.length);
          console.log('📊 입금요청 상세:', unmatchedData.availableRequests);
          setUnmatchedData(unmatchedData);
        }
      } catch (error) {
        console.error('거래 내역 조회 실패:', error);
        toast.error('거래 내역을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    }
    fetchTransactions();
  }, [groupId, startDate, endDate, permissions.canWithdraw]);

  // 사용자 정보 가져오기
  useEffect(() => {
    async function fetchUserInfo() {
      try {
        const userInfo = await getMyInfo();
        setCurrentUserId(userInfo.userId);
      } catch (error) {
        console.error('사용자 정보 조회 실패:', error);
      }
    }
    fetchUserInfo();
  }, []);

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

  // 체크박스 토글 (선택/해제만)
  const toggleSelectTransaction = (unmatched: BankTransactionHistory) => {
    const isAlreadySelected = selectedUnmatched.some(tx => tx.historyId === unmatched.historyId);
    
    if (isAlreadySelected) {
      setSelectedUnmatched(prev => prev.filter(tx => tx.historyId !== unmatched.historyId));
    } else {
      setSelectedUnmatched(prev => [...prev, unmatched]);
    }
  };

  // 매칭 다이얼로그 열기
  const handleOpenMatchDialog = async () => {
    if (selectedUnmatched.length === 0) {
      toast.error('매칭할 거래를 선택해주세요.');
      return;
    }

    setSelectedRequest('');
    
    // 매칭 다이얼로그 열 때 최신 데이터 다시 조회
    try {
      const freshData = await getUnmatchedTransactions(Number(groupId));
      console.log('🔍 미매칭 데이터:', freshData);
      console.log('📋 입금요청 개수:', freshData.availableRequests.length);
      setUnmatchedData(freshData);
    } catch (error) {
      console.error('미매칭 데이터 조회 실패:', error);
    }
    
    setShowMatchDialog(true);
  };

  const handleManualMatch = async () => {
    console.log('매칭 완료 버튼 클릭!');
    console.log('groupId:', groupId);
    console.log('selectedUnmatched:', selectedUnmatched);
    console.log('selectedRequest:', selectedRequest);
    console.log('currentUserId:', currentUserId);

    if (!groupId || selectedUnmatched.length === 0 || !selectedRequest || !currentUserId) {
      console.error('매칭 정보 누락:', { groupId, selectedUnmatched, selectedRequest, currentUserId });
      toast.error('매칭 정보를 선택해주세요.');
      return;
    }

    try {
      setIsSubmitting(true);
      
      // 여러 거래를 선택한 경우
      if (selectedUnmatched.length > 1) {
        const historyIds = selectedUnmatched.map(tx => tx.historyId);
        console.log('다중 거래 매칭 요청:', { groupId, requestId: selectedRequest, historyIds, currentUserId });
        await manualMatchMultiple(
          Number(groupId),
          Number(selectedRequest),
          historyIds,
          currentUserId
        );
        toast.success(`${selectedUnmatched.length}개의 거래가 매칭되었습니다.`);
      } else {
        // 단일 거래 매칭
        console.log('단일 거래 매칭 요청:', { groupId, requestId: selectedRequest, historyId: selectedUnmatched[0].historyId, currentUserId });
        await manualMatch(
          Number(groupId),
          Number(selectedRequest),
          selectedUnmatched[0].historyId,
          currentUserId
        );
        toast.success('수동 매칭이 완료되었습니다.');
      }
      
      setShowMatchDialog(false);
      setSelectedUnmatched([]);
      setSelectedRequest('');
      
      // 목록 새로고침
      const [transactionData, unmatchedDataRefresh] = await Promise.all([
        getLedger(Number(groupId), startDate || undefined, endDate || undefined),
        getUnmatchedTransactions(Number(groupId))
      ]);
      setTransactions(transactionData);
      setUnmatchedData(unmatchedDataRefresh);
    } catch (error) {
      console.error('수동 매칭 실패:', error);
      toast.error('수동 매칭에 실패했습니다.');
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
    <div className="space-y-4 pb-4" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* 날짜 필터 및 수동 기록 추가 버튼 - sticky로 고정 (탭 아래에 위치) */}
      <div className="sticky top-0 z-[60] bg-stone-50 pt-4 pb-2 -mx-4 md:-mx-6 px-4 md:px-6">
        <div className="bg-white rounded-xl p-4 border border-stone-100 shadow-sm">
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
      </div>

      {/* 거래 내역 목록 */}
      {loading ? (
        <div className="text-center py-8 text-stone-500">로딩 중...</div>
      ) : (
        <>
          {/* 미매칭 거래 섹션 */}
          {permissions.canWithdraw && unmatchedData && unmatchedData.unmatchedTransactions.length > 0 && (
            <div className="bg-amber-50 rounded-xl border border-amber-200 p-4 space-y-3">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <AlertCircle className="w-5 h-5 text-amber-600" />
                  <h3 className="font-semibold text-amber-900">미매칭 거래 ({unmatchedData.unmatchedTransactions.length}건)</h3>
                </div>
                {selectedUnmatched.length > 0 && (
                  <Button
                    onClick={handleOpenMatchDialog}
                    size="sm"
                    className="bg-orange-500 hover:bg-orange-600"
                  >
                    <Link2 className="w-4 h-4 mr-1" />
                    선택한 {selectedUnmatched.length}건 매칭
                  </Button>
                )}
              </div>
              <p className="text-sm text-amber-700">
                아래 거래내역은 자동 매칭되지 않았습니다. 수동으로 매칭하거나 확인이 필요합니다.
              </p>
              <div className="space-y-2">
                {unmatchedData.unmatchedTransactions.map((tx) => {
                  const isDeposit = tx.inoutType === 'DEPOSIT';
                  const isSelected = selectedUnmatched.some(s => s.historyId === tx.historyId);
                  return (
                    <div 
                      key={tx.historyId} 
                      className={`bg-white rounded-lg p-3 border ${isSelected ? 'border-orange-400 ring-2 ring-orange-200' : 'border-amber-200'} cursor-pointer transition-all`}
                      onClick={() => toggleSelectTransaction(tx)}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3 flex-1">
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => toggleSelectTransaction(tx)}
                            onClick={(e) => e.stopPropagation()}
                            className="w-4 h-4 text-orange-500 rounded border-stone-300 focus:ring-orange-500"
                          />
                          <div
                            className={`w-8 h-8 rounded-full flex items-center justify-center ${
                              isDeposit
                                ? 'bg-blue-50 text-blue-600'
                                : 'bg-red-50 text-red-600'
                            }`}
                          >
                            {isDeposit ? (
                              <ArrowDownLeft className="w-4 h-4" />
                            ) : (
                              <ArrowUpRight className="w-4 h-4" />
                            )}
                          </div>
                          <div className="flex-1">
                            <p className="font-medium text-stone-900">{tx.printContent}</p>
                            <div className="flex items-center gap-2 mt-0.5">
                              <Calendar className="w-3 h-3 text-stone-400" />
                              <p className="text-xs text-stone-500">
                                {new Date(tx.bankTransactionAt).toLocaleDateString('ko-KR')}
                              </p>
                            </div>
                            {tx.unmatchReason && (
                              <p className="text-xs text-amber-600 mt-1">
                                사유: {tx.unmatchReason}
                              </p>
                            )}
                          </div>
                        </div>
                        <div className="text-right">
                          <p className={`font-semibold ${isDeposit ? 'text-blue-600' : 'text-stone-900'}`}>
                            {isDeposit ? '+' : '-'}
                            {formatAmount(tx.amount)}원
                          </p>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* 기존 거래 내역 목록 */}
          {transactions.length === 0 ? (
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
                              {tx.matchedMemberName && (
                                <p className="text-xs text-green-600 mt-0.5">
                                  ✓ {tx.matchedMemberName} 매칭됨
                                </p>
                              )}
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
                              {tx.bankHistoryId && (
                                <Badge variant="outline" className="mt-1 text-xs ml-1">
                                  은행거래 #{tx.bankHistoryId}
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
        </>
      )}

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

      {/* 수동 매칭 다이얼로그 */}
      <Dialog open={showMatchDialog} onOpenChange={(open) => {
        setShowMatchDialog(open);
        if (!open) {
          setSelectedUnmatched([]);
          setSelectedRequest('');
        }
      }}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>거래 수동 매칭</DialogTitle>
            <DialogDescription>
              {selectedUnmatched.length > 1 
                ? `${selectedUnmatched.length}개의 미매칭 거래를 입금요청과 매칭합니다.` 
                : '미매칭 거래를 입금요청과 매칭합니다.'}
            </DialogDescription>
          </DialogHeader>
          {selectedUnmatched.length > 0 && (
            <div className="py-4 space-y-4">
              {/* 선택된 거래 정보 */}
              <div className="bg-stone-50 rounded-lg p-4 space-y-3">
                <Label className="text-xs text-stone-500">
                  선택된 거래 ({selectedUnmatched.length}건)
                </Label>
                {selectedUnmatched.map((tx) => (
                  <div key={tx.historyId} className="flex items-center justify-between bg-white rounded p-2 border border-stone-200">
                    <div>
                      <p className="font-medium text-stone-900 text-sm">{tx.printContent}</p>
                      <p className="text-xs text-stone-500">
                        {new Date(tx.bankTransactionAt).toLocaleDateString('ko-KR')}
                      </p>
                    </div>
                    <p className="font-bold text-blue-600">
                      +{formatAmount(tx.amount)}원
                    </p>
                  </div>
                ))}
                {selectedUnmatched.length > 1 && (
                  <div className="border-t border-stone-200 pt-2 flex items-center justify-between">
                    <span className="text-sm font-medium text-stone-700">합계</span>
                    <span className="text-lg font-bold text-blue-600">
                      +{formatAmount(selectedUnmatched.reduce((sum, tx) => sum + tx.amount, 0))}원
                    </span>
                  </div>
                )}
              </div>

              {/* 입금요청 선택 */}
              <div className="space-y-2">
                <Label htmlFor="request-select">매칭할 입금요청</Label>
                {unmatchedData && unmatchedData.availableRequests.length > 0 ? (
                  <>
                    <p className="text-xs text-green-600 mb-2">
                      ✓ {unmatchedData.availableRequests.length}개의 입금요청이 있습니다.
                    </p>
                    <select
                      id="request-select"
                      value={selectedRequest}
                      onChange={(e) => {
                        console.log('선택된 요청 ID:', e.target.value);
                        setSelectedRequest(e.target.value);
                      }}
                      className="flex h-10 w-full rounded-md border border-stone-200 bg-white px-3 py-2 text-sm ring-offset-white placeholder:text-stone-500 focus:outline-none focus:ring-2 focus:ring-stone-950 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <option value="">입금요청을 선택하세요</option>
                      {unmatchedData.availableRequests.map((req) => (
                        <option key={req.requestId} value={req.requestId.toString()}>
                          {req.memberName} - {formatAmount(req.expectedAmount)}원
                          {req.scheduleId ? ` (일정 #${req.scheduleId})` : ''}
                        </option>
                      ))}
                    </select>
                  </>
                ) : (
                  <p className="text-sm text-stone-500 py-2">
                    매칭 가능한 입금요청이 없습니다.
                  </p>
                )}
              </div>

              {/* 매칭 정보 확인 */}
              {selectedRequest && unmatchedData && (
                <div className="bg-blue-50 rounded-lg p-4">
                  <p className="text-sm text-blue-900">
                    <strong>{unmatchedData.availableRequests.find(r => r.requestId === Number(selectedRequest))?.memberName}</strong>님의 
                    입금요청과 매칭됩니다.
                  </p>
                </div>
              )}
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowMatchDialog(false)}>
              취소
            </Button>
            <Button
              onClick={handleManualMatch}
              disabled={!selectedRequest || isSubmitting}
              className="bg-orange-500 hover:bg-orange-600 disabled:bg-stone-300"
            >
              {isSubmitting ? '매칭 중...' : '매칭 완료'}
            </Button>
            {/* 디버깅용 */}
            {!selectedRequest && (
              <p className="text-xs text-red-500 mt-2">
                ⚠️ 입금요청이 선택되지 않았습니다. ({selectedRequest})
              </p>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
