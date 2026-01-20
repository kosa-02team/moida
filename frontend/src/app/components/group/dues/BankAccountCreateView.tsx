import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Building2, User, CreditCard } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../ui/select';
import { Card, CardContent } from '../../ui/card';
import { createBankAccount, type AccountCreateRequest } from '../../../../api/bank';
import { getMyInfo } from '../../../../api/user';
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

export function BankAccountCreateView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [userId, setUserId] = useState<number | null>(null);
  const [bankCode, setBankCode] = useState('');
  const [accountNumber, setAccountNumber] = useState('');
  const [ownerName, setOwnerName] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    async function fetchMyInfo() {
      try {
        const userInfo = await getMyInfo();
        setUserId(userInfo.userId);
        setOwnerName(userInfo.realName);
      } catch (error) {
        console.error('사용자 정보 조회 실패:', error);
      }
    }
    fetchMyInfo();
  }, []);

  const handleSubmit = async () => {
    if (!groupId || !userId) {
      toast.error('필수 정보가 없습니다.');
      return;
    }
    if (!bankCode) {
      toast.error('은행을 선택해주세요.');
      return;
    }
    if (!ownerName.trim()) {
      toast.error('소유자명을 입력해주세요.');
      return;
    }

    try {
      setIsSubmitting(true);
      const request: AccountCreateRequest = {
        userId,
        bankCode,
        accountNumber: accountNumber.trim() || null,
        ownerName: ownerName.trim(),
      };

      await createBankAccount(Number(groupId), request);
      toast.success('가상계좌가 생성되었습니다.');
      navigate(-1);
    } catch (error) {
      console.error('가상계좌 생성 실패:', error);
      toast.error('가상계좌 생성에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // 권한 체크
  if (!permissions.canWithdraw) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-stone-500">가상계좌 생성은 총무 이상만 가능합니다.</p>
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
          <h1 className="ml-2 text-lg font-semibold text-stone-800">가상계좌 생성</h1>
        </div>
      </header>

      <div className="p-5 space-y-4">
        <Card className="border-stone-100">
          <CardContent className="p-5 space-y-4">
            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <Building2 className="w-4 h-4" />
                은행 선택 *
              </Label>
              <Select value={bankCode} onValueChange={setBankCode}>
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
                계좌번호 (선택)
              </Label>
              <Input
                placeholder="기존 계좌번호가 있으면 입력하세요"
                value={accountNumber}
                onChange={(e) => setAccountNumber(e.target.value)}
              />
              <p className="text-xs text-stone-500">
                비워두면 가상계좌가 자동으로 발급됩니다.
              </p>
            </div>

            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <User className="w-4 h-4" />
                소유자명 *
              </Label>
              <Input
                placeholder="계좌 소유자명"
                value={ownerName}
                onChange={(e) => setOwnerName(e.target.value)}
                maxLength={50}
              />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Bottom Button */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-stone-100 p-4">
        <Button
          onClick={handleSubmit}
          disabled={!bankCode || !ownerName.trim() || isSubmitting}
          className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl"
        >
          {isSubmitting ? '생성 중...' : '가상계좌 생성'}
        </Button>
      </div>
    </div>
  );
}
