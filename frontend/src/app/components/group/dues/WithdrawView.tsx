import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Wallet, Send, AlertTriangle, User } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Textarea } from '../../ui/textarea';
import { Card, CardContent } from '../../ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../ui/select';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '../../ui/alert-dialog';

export function WithdrawView() {
  const navigate = useNavigate();
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('');
  const [recipient, setRecipient] = useState('');
  const [accountInfo, setAccountInfo] = useState('');
  const [note, setNote] = useState('');
  const [showConfirm, setShowConfirm] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 현재 잔액 (실제로는 API에서)
  const currentBalance = 250000;

  // 지출 카테고리
  const categories = [
    { value: 'event', label: '모임 행사비' },
    { value: 'supplies', label: '용품 구매' },
    { value: 'food', label: '식비/음료' },
    { value: 'venue', label: '장소 대여' },
    { value: 'refund', label: '회비 환불' },
    { value: 'other', label: '기타' },
  ];

  const handleSubmit = () => {
    const amountNum = parseInt(amount.replace(/,/g, ''));
    if (!amountNum || amountNum <= 0) {
      toast.error('금액을 입력해주세요');
      return;
    }
    if (amountNum > currentBalance) {
      toast.error('잔액이 부족합니다');
      return;
    }
    if (!category) {
      toast.error('지출 유형을 선택해주세요');
      return;
    }
    if (!recipient) {
      toast.error('받는 사람을 입력해주세요');
      return;
    }

    setShowConfirm(true);
  };

  const handleConfirm = () => {
    setIsSubmitting(true);
    // 실제로는 API 호출
    setTimeout(() => {
      setIsSubmitting(false);
      setShowConfirm(false);
      toast.success('출금이 완료되었습니다');
      navigate(-1);
    }, 1000);
  };

  const formatAmount = (value: string) => {
    const num = value.replace(/[^0-9]/g, '');
    return num ? parseInt(num).toLocaleString() : '';
  };

  const amountNum = parseInt(amount.replace(/,/g, '')) || 0;

  return (
    <div className="min-h-screen bg-stone-50 pb-24">
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
          <h1 className="ml-2 text-lg font-semibold text-stone-800">회비 보내기</h1>
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Current Balance */}
        <Card className="border-green-200 bg-green-50">
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 bg-green-500 rounded-full flex items-center justify-center">
                <Wallet className="w-6 h-6 text-white" />
              </div>
              <div>
                <p className="text-sm text-green-700">현재 잔액</p>
                <p className="text-2xl font-bold text-green-900">
                  {currentBalance.toLocaleString()}원
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Warning */}
        <div className="flex items-start gap-3 p-4 bg-amber-50 border border-amber-200 rounded-xl">
          <AlertTriangle className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-amber-800">총무/모임장 전용 기능</p>
            <p className="text-xs text-amber-700 mt-1">
              모든 출금 내역은 회원에게 공개되며, 관리자에게 알림이 전송됩니다.
            </p>
          </div>
        </div>

        {/* Amount Input */}
        <div className="bg-white rounded-2xl p-5 border border-stone-100 space-y-4">
          <h3 className="font-bold text-stone-900">출금 금액</h3>
          
          <div className="relative">
            <Input
              type="text"
              inputMode="numeric"
              placeholder="0"
              className={`h-14 text-2xl font-bold text-right pr-12 bg-stone-50 border-stone-200 rounded-xl ${
                amountNum > currentBalance ? 'border-red-500 bg-red-50' : ''
              }`}
              value={amount}
              onChange={(e) => setAmount(formatAmount(e.target.value))}
            />
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-lg font-medium text-stone-500">
              원
            </span>
          </div>
          {amountNum > currentBalance && (
            <p className="text-sm text-red-500">잔액이 부족합니다</p>
          )}
          {amountNum > 0 && amountNum <= currentBalance && (
            <p className="text-sm text-stone-500">
              출금 후 잔액: {(currentBalance - amountNum).toLocaleString()}원
            </p>
          )}
        </div>

        {/* Category */}
        <div className="bg-white rounded-2xl p-5 border border-stone-100 space-y-3">
          <Label>지출 유형 *</Label>
          <Select value={category} onValueChange={setCategory}>
            <SelectTrigger className="h-12 bg-stone-50 border-stone-200 rounded-xl">
              <SelectValue placeholder="지출 유형을 선택하세요" />
            </SelectTrigger>
            <SelectContent>
              {categories.map((cat) => (
                <SelectItem key={cat.value} value={cat.value}>
                  {cat.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Recipient */}
        <div className="bg-white rounded-2xl p-5 border border-stone-100 space-y-3">
          <Label>받는 사람 *</Label>
          <div className="relative">
            <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
            <Input
              placeholder="이름 또는 상호명"
              className="h-12 pl-10 bg-stone-50 border-stone-200 rounded-xl"
              value={recipient}
              onChange={(e) => setRecipient(e.target.value)}
            />
          </div>
        </div>

        {/* Account Info (optional) */}
        <div className="bg-white rounded-2xl p-5 border border-stone-100 space-y-3">
          <Label>계좌 정보 (선택)</Label>
          <Input
            placeholder="은행명 + 계좌번호"
            className="h-12 bg-stone-50 border-stone-200 rounded-xl"
            value={accountInfo}
            onChange={(e) => setAccountInfo(e.target.value)}
          />
        </div>

        {/* Note */}
        <div className="bg-white rounded-2xl p-5 border border-stone-100 space-y-3">
          <Label>상세 내용</Label>
          <Textarea
            placeholder="출금 사유를 상세히 작성해주세요"
            className="min-h-24 resize-none bg-stone-50 border-stone-200 rounded-xl"
            value={note}
            onChange={(e) => setNote(e.target.value)}
          />
        </div>
      </div>

      {/* Bottom Button */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-stone-100 p-4 safe-area-pb">
        <div className="max-w-md mx-auto">
          <Button
            onClick={handleSubmit}
            disabled={!amount || amountNum > currentBalance || !category || !recipient}
            className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl disabled:opacity-50"
          >
            <Send className="w-5 h-5 mr-2" />
            {amount || '0'}원 보내기
          </Button>
        </div>
      </div>

      {/* Confirmation Dialog */}
      <AlertDialog open={showConfirm} onOpenChange={setShowConfirm}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>출금 확인</AlertDialogTitle>
            <AlertDialogDescription asChild>
              <div className="space-y-3">
                <p>아래 내용으로 출금하시겠습니까?</p>
                <div className="bg-stone-50 rounded-lg p-4 space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-stone-500">금액</span>
                    <span className="font-bold text-stone-900">{amount}원</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-stone-500">유형</span>
                    <span className="text-stone-900">
                      {categories.find(c => c.value === category)?.label}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-stone-500">받는 사람</span>
                    <span className="text-stone-900">{recipient}</span>
                  </div>
                </div>
              </div>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleConfirm}
              disabled={isSubmitting}
              className="bg-orange-500 hover:bg-orange-600"
            >
              {isSubmitting ? '처리 중...' : '확인'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}



