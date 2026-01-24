import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, User, Building2, CreditCard, DollarSign } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../ui/select';
import { Textarea } from '../../ui/textarea';
import { Card, CardContent } from '../../ui/card';
import { refundToMember, type RefundRequest } from '../../../../api/bank';
import { getMembers, type MemberListResponse } from '../../../../api/member';
import { useUserPermissions } from '../../../data/userRoles';

const BANK_CODES: Record<string, string> = {
  '001': '한국은행',
  '002': '산업은행',
  '003': '기업은행',
  '004': 'KB국민은행',
  '011': 'NH농협은행',
  '020': '우리은행',
  '023': 'SC제일은행',
  '027': '한국씨티은행',
  '088': '신한은행',
  '090': '카카오뱅크',
  '092': '토스뱅크',
};

export function RefundView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [members, setMembers] = useState<MemberListResponse[]>([]);
  const [loadingMembers, setLoadingMembers] = useState(false);
  const [selectedMemberId, setSelectedMemberId] = useState<number | null>(null);
  const [recipientName, setRecipientName] = useState('');
  const [recipientBankCode, setRecipientBankCode] = useState('');
  const [recipientAccountNum, setRecipientAccountNum] = useState('');
  const [amount, setAmount] = useState('');
  const [memo, setMemo] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    async function fetchMembers() {
      if (!groupId) return;
      try {
        setLoadingMembers(true);
        const activeMembers = await getMembers(Number(groupId), 'ACTIVE');
        setMembers(activeMembers);
      } catch (error) {
        console.error('멤버 목록 조회 실패:', error);
        toast.error('멤버 목록을 불러오는데 실패했습니다.');
      } finally {
        setLoadingMembers(false);
      }
    }
    fetchMembers();
  }, [groupId]);

  const handleMemberSelect = (memberId: string) => {
    const member = members.find(m => m.memberId === Number(memberId));
    if (member) {
      setSelectedMemberId(member.memberId);
      setRecipientName(member.realName);
    }
  };

  const handleSubmit = async () => {
    if (!groupId || !selectedMemberId) {
      toast.error('회원을 선택해주세요.');
      return;
    }
    if (!recipientName.trim()) {
      toast.error('받는 사람 이름을 입력해주세요.');
      return;
    }
    if (!recipientBankCode) {
      toast.error('은행을 선택해주세요.');
      return;
    }
    if (!recipientAccountNum.trim()) {
      toast.error('계좌번호를 입력해주세요.');
      return;
    }
    if (!amount || parseFloat(amount) <= 0) {
      toast.error('올바른 금액을 입력해주세요.');
      return;
    }

    try {
      setIsSubmitting(true);
      const request: RefundRequest = {
        clubId: Number(groupId),
        recipientUserId: selectedMemberId,
        recipientName: recipientName.trim(),
        recipientBankCode,
        recipientAccountNum: recipientAccountNum.trim(),
        amount: parseFloat(amount),
        memo: memo.trim(),
      };

      const response = await refundToMember(Number(groupId), request);
      if (response.success) {
        toast.success(`환급이 완료되었습니다. (${response.amount.toLocaleString()}원)`);
        navigate(-1);
      } else {
        toast.error(response.message || '환급에 실패했습니다.');
      }
    } catch (error) {
      console.error('환급 실패:', error);
      toast.error('환급에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // 권한 체크
  if (!permissions.canWithdraw) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-stone-500">환급은 총무 이상만 가능합니다.</p>
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
          <h1 className="ml-2 text-lg font-semibold text-stone-800">환급하기</h1>
        </div>
      </header>

      <div className="p-5 space-y-4">
        <Card className="border-stone-100">
          <CardContent className="p-5 space-y-4">
            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <User className="w-4 h-4" />
                회원 선택 *
              </Label>
              <Select
                value={selectedMemberId?.toString() || ''}
                onValueChange={handleMemberSelect}
              >
                <SelectTrigger>
                  <SelectValue placeholder="회원을 선택하세요" />
                </SelectTrigger>
                <SelectContent>
                  {members.map(member => (
                    <SelectItem key={member.memberId} value={member.memberId.toString()}>
                      {member.clubNickname || '멤버'}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <User className="w-4 h-4" />
                받는 사람 이름 *
              </Label>
              <Input
                placeholder="받는 사람 이름"
                value={recipientName}
                onChange={(e) => setRecipientName(e.target.value)}
                maxLength={50}
              />
            </div>

            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <Building2 className="w-4 h-4" />
                은행 선택 *
              </Label>
              <Select value={recipientBankCode} onValueChange={setRecipientBankCode}>
                <SelectTrigger>
                  <SelectValue placeholder="은행을 선택하세요" />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(BANK_CODES).map(([code, name]) => (
                    <SelectItem key={code} value={code}>
                      {name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <CreditCard className="w-4 h-4" />
                계좌번호 *
              </Label>
              <Input
                placeholder="계좌번호"
                value={recipientAccountNum}
                onChange={(e) => setRecipientAccountNum(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <DollarSign className="w-4 h-4" />
                환급 금액 *
              </Label>
              <Input
                type="number"
                placeholder="0"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                min="1"
              />
            </div>

            <div className="space-y-2">
              <Label>메모 (선택)</Label>
              <Textarea
                placeholder="환급 사유를 입력하세요"
                value={memo}
                onChange={(e) => setMemo(e.target.value)}
                maxLength={200}
              />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Bottom Button */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-stone-100 p-4">
        <Button
          onClick={handleSubmit}
          disabled={
            !selectedMemberId ||
            !recipientName.trim() ||
            !recipientBankCode ||
            !recipientAccountNum.trim() ||
            !amount ||
            isSubmitting
          }
          className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl"
        >
          {isSubmitting ? '환급 중...' : '환급하기'}
        </Button>
      </div>
    </div>
  );
}
