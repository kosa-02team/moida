import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Plus, Trash2, Calendar } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../ui/select';
import { Card, CardContent } from '../../ui/card';
import { getMembers, type MemberListResponse } from '../../../../api/member';
import { createPaymentRequests, type PaymentRequestCreateRequest, type PaymentRequestItem } from '../../../../api/payment-request';
import { useUserPermissions } from '../../../data/userRoles';

interface RequestFormItem {
  id: string;
  memberId: number | null;
  memberName: string;
  requestType: 'MEMBERSHIP_FEE' | 'SETTLEMENT' | 'DEPOSIT';
  expectedAmount: string;
  expectedDate: string;
  matchDaysRange: string;
  expiresInDays: string;
}

export function PaymentRequestCreateView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [members, setMembers] = useState<MemberListResponse[]>([]);
  const [loadingMembers, setLoadingMembers] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [items, setItems] = useState<RequestFormItem[]>([
    {
      id: '1',
      memberId: null,
      memberName: '',
      requestType: 'DEPOSIT',
      expectedAmount: '',
      expectedDate: new Date().toISOString().split('T')[0],
      matchDaysRange: '10',
      expiresInDays: '30',
    },
  ]);

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

  const addItem = () => {
    setItems([
      ...items,
      {
        id: Date.now().toString(),
        memberId: null,
        memberName: '',
        requestType: 'DEPOSIT',
        expectedAmount: '',
        expectedDate: new Date().toISOString().split('T')[0],
        matchDaysRange: '10',
        expiresInDays: '30',
      },
    ]);
  };

  const removeItem = (id: string) => {
    if (items.length > 1) {
      setItems(items.filter(item => item.id !== id));
    }
  };

  const updateItem = (id: string, field: keyof RequestFormItem, value: string | number) => {
    setItems(items.map(item => {
      if (item.id !== id) return item;
      
      if (field === 'memberId') {
        const member = members.find(m => m.memberId === value);
        return {
          ...item,
          memberId: value as number,
          memberName: member ? (member.clubNickname || member.realName) : '',
        };
      }
      
      return { ...item, [field]: value };
    }));
  };

  const handleSubmit = async () => {
    if (!groupId) return;

    // 유효성 검사
    for (const item of items) {
      if (!item.memberId) {
        toast.error('모든 항목의 멤버를 선택해주세요.');
        return;
      }
      if (!item.expectedAmount || parseFloat(item.expectedAmount) <= 0) {
        toast.error('모든 항목의 금액을 입력해주세요.');
        return;
      }
      if (!item.expectedDate) {
        toast.error('모든 항목의 예상 날짜를 입력해주세요.');
        return;
      }
    }

    try {
      setIsSubmitting(true);
      
      const requestItems: PaymentRequestItem[] = items.map(item => ({
        memberId: item.memberId!,
        memberName: item.memberName,
        requestType: item.requestType,
        expectedAmount: parseFloat(item.expectedAmount),
        expectedDate: item.expectedDate,
        matchDaysRange: item.matchDaysRange ? parseInt(item.matchDaysRange) : undefined,
        expiresInDays: item.expiresInDays ? parseInt(item.expiresInDays) : undefined,
        scheduleId: null,
        billingPeriod: null,
      }));

      const request: PaymentRequestCreateRequest = {
        requests: requestItems,
      };

      await createPaymentRequests(Number(groupId), request);
      toast.success('입금 요청이 생성되었습니다.');
      navigate(-1);
    } catch (error) {
      console.error('입금 요청 생성 실패:', error);
      toast.error('입금 요청 생성에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // 권한 체크
  if (!permissions.canWithdraw) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-stone-500">입금 요청 생성은 총무 이상만 가능합니다.</p>
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
          <h1 className="ml-2 text-lg font-semibold text-stone-800">입금 요청 생성</h1>
        </div>
      </header>

      <div className="p-5 space-y-4">
        {/* 요청 항목들 */}
        {items.map((item, index) => (
          <Card key={item.id} className="border-stone-100">
            <CardContent className="p-4 space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="font-medium text-stone-900">요청 #{index + 1}</h3>
                {items.length > 1 && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => removeItem(item.id)}
                    className="text-red-500 hover:text-red-600"
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                )}
              </div>

              <div className="space-y-3">
                <div className="space-y-1">
                  <Label>멤버 *</Label>
                  <Select
                    value={item.memberId?.toString() || ''}
                    onValueChange={(value) => updateItem(item.id, 'memberId', parseInt(value))}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="멤버 선택" />
                    </SelectTrigger>
                    <SelectContent>
                      {members.map(member => (
                        <SelectItem key={member.memberId} value={member.memberId.toString()}>
                          {member.clubNickname || member.realName}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-1">
                  <Label>요청 유형 *</Label>
                  <Select
                    value={item.requestType}
                    onValueChange={(value: 'MEMBERSHIP_FEE' | 'SETTLEMENT' | 'DEPOSIT') =>
                      updateItem(item.id, 'requestType', value)
                    }
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="MEMBERSHIP_FEE">회비</SelectItem>
                      <SelectItem value="SETTLEMENT">정산</SelectItem>
                      <SelectItem value="DEPOSIT">입금</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-1">
                  <Label>예상 금액 *</Label>
                  <Input
                    type="number"
                    placeholder="0"
                    value={item.expectedAmount}
                    onChange={(e) => updateItem(item.id, 'expectedAmount', e.target.value)}
                    min="1"
                  />
                </div>

                <div className="space-y-1">
                  <Label>예상 날짜 *</Label>
                  <div className="relative">
                    <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400" />
                    <Input
                      type="date"
                      value={item.expectedDate}
                      onChange={(e) => updateItem(item.id, 'expectedDate', e.target.value)}
                      className="pl-10"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1">
                    <Label>매칭 범위 (일)</Label>
                    <Input
                      type="number"
                      placeholder="10"
                      value={item.matchDaysRange}
                      onChange={(e) => updateItem(item.id, 'matchDaysRange', e.target.value)}
                      min="1"
                    />
                  </div>
                  <div className="space-y-1">
                    <Label>만료일 (일)</Label>
                    <Input
                      type="number"
                      placeholder="30"
                      value={item.expiresInDays}
                      onChange={(e) => updateItem(item.id, 'expiresInDays', e.target.value)}
                      min="1"
                    />
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}

        <Button
          onClick={addItem}
          variant="outline"
          className="w-full border-dashed"
        >
          <Plus className="w-4 h-4 mr-2" />
          항목 추가
        </Button>
      </div>

      {/* Bottom Button */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-stone-100 p-4">
        <Button
          onClick={handleSubmit}
          disabled={isSubmitting || loadingMembers}
          className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl"
        >
          {isSubmitting ? '생성 중...' : `입금 요청 생성 (${items.length}개)`}
        </Button>
      </div>
    </div>
  );
}
