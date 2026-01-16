import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Plus, Receipt, Trash2, Upload, Calculator, Users, Check } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Textarea } from '../../ui/textarea';
import { Card, CardContent } from '../../ui/card';
import { Checkbox } from '../../ui/checkbox';
import { Avatar, AvatarFallback, AvatarImage } from '../../ui/avatar';
import { Badge } from '../../ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '../../ui/dialog';

interface ExpenseItem {
  id: string;
  title: string;
  amount: number;
  receipt?: string;
}

interface Member {
  id: string;
  name: string;
  avatar?: string;
  selected: boolean;
}

export function SettlementRequestView() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [items, setItems] = useState<ExpenseItem[]>([
    { id: '1', title: '', amount: 0 }
  ]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showMemberSelector, setShowMemberSelector] = useState(false);
  const [splitMethod, setSplitMethod] = useState<'equal' | 'custom'>('equal');

  // 멤버 목록 (실제로는 API에서)
  const [members, setMembers] = useState<Member[]>([
    { id: '1', name: '홍길동', avatar: '', selected: true },
    { id: '2', name: '김철수', avatar: '', selected: true },
    { id: '3', name: '이영희', avatar: '', selected: true },
    { id: '4', name: '박민수', avatar: '', selected: false },
    { id: '5', name: '정지훈', avatar: '', selected: false },
    { id: '6', name: '최유진', avatar: '', selected: true },
  ]);

  const totalAmount = items.reduce((sum, item) => sum + (item.amount || 0), 0);
  const selectedMembers = members.filter(m => m.selected);
  const perPersonAmount = selectedMembers.length > 0 
    ? Math.ceil(totalAmount / selectedMembers.length)
    : 0;

  const addItem = () => {
    setItems([...items, { id: Date.now().toString(), title: '', amount: 0 }]);
  };

  const removeItem = (id: string) => {
    if (items.length > 1) {
      setItems(items.filter(item => item.id !== id));
    }
  };

  const updateItem = (id: string, field: 'title' | 'amount', value: string | number) => {
    setItems(items.map(item => 
      item.id === id 
        ? { ...item, [field]: field === 'amount' ? Number(value) || 0 : value }
        : item
    ));
  };

  const toggleMember = (id: string) => {
    setMembers(members.map(m => 
      m.id === id ? { ...m, selected: !m.selected } : m
    ));
  };

  const selectAllMembers = () => {
    setMembers(members.map(m => ({ ...m, selected: true })));
  };

  const deselectAllMembers = () => {
    setMembers(members.map(m => ({ ...m, selected: false })));
  };

  const handleSubmit = () => {
    if (!title.trim()) {
      toast.error('정산 제목을 입력해주세요');
      return;
    }
    if (totalAmount <= 0) {
      toast.error('정산 금액을 입력해주세요');
      return;
    }
    if (selectedMembers.length === 0) {
      toast.error('정산 대상자를 선택해주세요');
      return;
    }

    setIsSubmitting(true);
    setTimeout(() => {
      setIsSubmitting(false);
      toast.success('정산 요청이 등록되었습니다');
      navigate(-1);
    }, 1000);
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-32">
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
          <h1 className="ml-2 text-lg font-semibold text-stone-800">정산 요청</h1>
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Title */}
        <div className="bg-white rounded-2xl p-5 border border-stone-100 space-y-3">
          <Label>정산 제목 *</Label>
          <Input
            placeholder="예: 4월 모임 식비 정산"
            className="h-12 bg-stone-50 border-stone-200 rounded-xl"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
        </div>

        {/* Expense Items */}
        <div className="bg-white rounded-2xl p-5 border border-stone-100 space-y-4">
          <div className="flex items-center justify-between">
            <Label>지출 항목</Label>
            <Button
              variant="ghost"
              size="sm"
              onClick={addItem}
              className="text-orange-600 hover:text-orange-700"
            >
              <Plus className="w-4 h-4 mr-1" />
              항목 추가
            </Button>
          </div>

          <div className="space-y-3">
            {items.map((item, index) => (
              <div key={item.id} className="flex gap-2 items-start">
                <div className="flex-1 space-y-2">
                  <Input
                    placeholder="항목명"
                    className="h-10 bg-stone-50 border-stone-200 rounded-lg"
                    value={item.title}
                    onChange={(e) => updateItem(item.id, 'title', e.target.value)}
                  />
                  <div className="relative">
                    <Input
                      type="number"
                      placeholder="금액"
                      className="h-10 bg-stone-50 border-stone-200 rounded-lg pr-8"
                      value={item.amount || ''}
                      onChange={(e) => updateItem(item.id, 'amount', e.target.value)}
                    />
                    <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-stone-500">원</span>
                  </div>
                </div>
                <div className="flex gap-1 pt-1">
                  <Button variant="ghost" size="icon" className="h-8 w-8">
                    <Upload className="w-4 h-4 text-stone-500" />
                  </Button>
                  {items.length > 1 && (
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => removeItem(item.id)}
                      className="h-8 w-8 text-red-500 hover:text-red-600"
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>

          {/* Total */}
          <div className="flex justify-between items-center pt-4 border-t border-stone-100">
            <span className="font-medium text-stone-700">총 금액</span>
            <span className="text-xl font-bold text-orange-600">
              {totalAmount.toLocaleString()}원
            </span>
          </div>
        </div>

        {/* Target Members */}
        <div className="bg-white rounded-2xl p-5 border border-stone-100 space-y-4">
          <div className="flex items-center justify-between">
            <Label className="flex items-center gap-2">
              <Users className="w-4 h-4" />
              정산 대상자 *
            </Label>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowMemberSelector(true)}
              className="text-orange-600 border-orange-200 hover:bg-orange-50"
            >
              {selectedMembers.length}명 선택됨
            </Button>
          </div>

          {/* Selected Members Preview */}
          {selectedMembers.length > 0 && (
            <div className="space-y-3">
              <div className="flex flex-wrap gap-2">
                {selectedMembers.slice(0, 5).map(member => (
                  <Badge key={member.id} variant="secondary" className="bg-orange-50 text-orange-700 gap-1">
                    {member.name}
                  </Badge>
                ))}
                {selectedMembers.length > 5 && (
                  <Badge variant="secondary" className="bg-stone-100 text-stone-600">
                    +{selectedMembers.length - 5}명
                  </Badge>
                )}
              </div>

              {/* Per Person Amount */}
              <Card className="bg-blue-50 border-blue-200">
                <CardContent className="p-4">
                  <div className="flex items-center gap-3">
                    <Calculator className="w-5 h-5 text-blue-600" />
                    <div>
                      <p className="text-sm text-blue-700">1인당 정산 금액</p>
                      <p className="text-lg font-bold text-blue-900">
                        {perPersonAmount.toLocaleString()}원
                      </p>
                      <p className="text-xs text-blue-600">
                        {totalAmount.toLocaleString()}원 ÷ {selectedMembers.length}명
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          )}
        </div>

        {/* Description */}
        <div className="bg-white rounded-2xl p-5 border border-stone-100 space-y-3">
          <Label>상세 설명</Label>
          <Textarea
            placeholder="정산에 대한 상세 내용을 작성해주세요"
            className="min-h-24 resize-none bg-stone-50 border-stone-200 rounded-xl"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>
      </div>

      {/* Bottom Button */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-stone-100 p-4 safe-area-pb">
        <div className="max-w-md mx-auto space-y-2">
          <div className="flex justify-between text-sm text-stone-600 px-1">
            <span>총 {selectedMembers.length}명에게 정산 요청</span>
            <span className="font-medium">1인당 {perPersonAmount.toLocaleString()}원</span>
          </div>
          <Button
            onClick={handleSubmit}
            disabled={isSubmitting || !title || totalAmount <= 0 || selectedMembers.length === 0}
            className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl disabled:opacity-50"
          >
            <Receipt className="w-5 h-5 mr-2" />
            {isSubmitting ? '요청 중...' : '정산 요청하기'}
          </Button>
        </div>
      </div>

      {/* Member Selector Dialog */}
      <Dialog open={showMemberSelector} onOpenChange={setShowMemberSelector}>
        <DialogContent className="max-w-md max-h-[80vh] overflow-hidden flex flex-col">
          <DialogHeader>
            <DialogTitle>정산 대상자 선택</DialogTitle>
            <DialogDescription>
              정산에 참여할 멤버를 선택하세요
            </DialogDescription>
          </DialogHeader>

          <div className="flex justify-between py-2 border-b border-stone-100">
            <Button variant="ghost" size="sm" onClick={selectAllMembers}>
              전체 선택
            </Button>
            <Button variant="ghost" size="sm" onClick={deselectAllMembers}>
              전체 해제
            </Button>
          </div>

          <div className="flex-1 overflow-y-auto py-2 space-y-1">
            {members.map(member => (
              <label
                key={member.id}
                className={`flex items-center gap-3 p-3 rounded-xl cursor-pointer transition-colors ${
                  member.selected ? 'bg-orange-50' : 'hover:bg-stone-50'
                }`}
              >
                <Checkbox
                  checked={member.selected}
                  onCheckedChange={() => toggleMember(member.id)}
                  className="data-[state=checked]:bg-orange-500 data-[state=checked]:border-orange-500"
                />
                <Avatar className="w-10 h-10">
                  <AvatarImage src={member.avatar} />
                  <AvatarFallback>{member.name[0]}</AvatarFallback>
                </Avatar>
                <span className="flex-1 font-medium text-stone-900">{member.name}</span>
                {member.selected && (
                  <Check className="w-5 h-5 text-orange-500" />
                )}
              </label>
            ))}
          </div>

          <div className="pt-4 border-t border-stone-100">
            <Button
              onClick={() => setShowMemberSelector(false)}
              className="w-full bg-orange-500 hover:bg-orange-600"
            >
              {selectedMembers.length}명 선택 완료
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
