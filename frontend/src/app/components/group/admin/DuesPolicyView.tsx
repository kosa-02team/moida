import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Switch } from '../../ui/switch';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../ui/select';
import { Card, CardContent, CardHeader, CardTitle } from '../../ui/card';

export function DuesPolicyView() {
  const navigate = useNavigate();
  
  // Mock data
  const [amount, setAmount] = useState('30000');
  const [paymentDay, setPaymentDay] = useState('25');
  const [allowMultiplePayments, setAllowMultiplePayments] = useState(true);
  const [autoNotification, setAutoNotification] = useState(true);
  const [notificationDay, setNotificationDay] = useState('20');
  const [autoCharge, setAutoCharge] = useState(false);

  const handleSubmit = () => {
    if (!amount || parseInt(amount) <= 0) {
      toast.error('올바른 회비 금액을 입력해주세요');
      return;
    }
    // 실제로는 API 호출
    console.log({ 
      amount, 
      paymentDay, 
      allowMultiplePayments, 
      autoNotification,
      notificationDay,
      autoCharge
    });
    toast.success('회비 정책이 저장되었습니다');
    setTimeout(() => navigate(-1), 500);
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100 backdrop-blur-sm bg-white/95">
        <div className="flex items-center justify-between px-4 py-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="font-bold text-lg text-stone-800">회비 정책 관리</h1>
          <div className="w-10" />
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Basic Policy */}
        <Card className="border-stone-200">
          <CardHeader>
            <CardTitle className="text-lg">기본 정책</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            {/* Amount */}
            <div className="space-y-2">
              <Label htmlFor="amount" className="text-base font-medium">회비 금액</Label>
              <div className="flex items-center gap-2">
                <Input 
                  id="amount"
                  type="number"
                  placeholder="30000"
                  className="h-12 text-lg bg-white border-stone-200 focus-visible:ring-orange-500 rounded-xl"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                />
                <span className="text-stone-600 font-medium">원</span>
              </div>
              <p className="text-xs text-stone-500">월 회비 금액을 입력하세요</p>
            </div>

            {/* Payment Day */}
            <div className="space-y-2">
              <Label htmlFor="paymentDay" className="text-base font-medium">납부일</Label>
              <Select value={paymentDay} onValueChange={setPaymentDay}>
                <SelectTrigger className="h-12 bg-white border-stone-200 focus:ring-orange-500 rounded-xl">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Array.from({ length: 28 }, (_, i) => i + 1).map(day => (
                    <SelectItem key={day} value={day.toString()}>
                      매월 {day}일
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <p className="text-xs text-stone-500">회비 납부 마감일을 선택하세요</p>
            </div>

            {/* Allow Multiple Payments */}
            <div className="flex items-center justify-between py-2">
              <div className="space-y-0.5">
                <Label className="text-base text-stone-900">중복 납부 허용</Label>
                <p className="text-xs text-stone-500">여러 달 회비를 한 번에 납부할 수 있습니다</p>
              </div>
              <Switch 
                checked={allowMultiplePayments} 
                onCheckedChange={setAllowMultiplePayments}
                className="data-[state=checked]:bg-orange-500" 
              />
            </div>
          </CardContent>
        </Card>

        {/* Notification Settings */}
        <Card className="border-stone-200">
          <CardHeader>
            <CardTitle className="text-lg">알림 설정</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            {/* Auto Notification */}
            <div className="flex items-center justify-between py-2">
              <div className="space-y-0.5">
                <Label className="text-base text-stone-900">자동 미납 알림</Label>
                <p className="text-xs text-stone-500">미납된 회비에 대해 자동으로 알림을 보냅니다</p>
              </div>
              <Switch 
                checked={autoNotification} 
                onCheckedChange={setAutoNotification}
                className="data-[state=checked]:bg-orange-500" 
              />
            </div>

            {/* Notification Day */}
            {autoNotification && (
              <div className="space-y-2">
                <Label htmlFor="notificationDay" className="text-base font-medium">알림 발송일</Label>
                <Select value={notificationDay} onValueChange={setNotificationDay}>
                  <SelectTrigger className="h-12 bg-white border-stone-200 focus:ring-orange-500 rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Array.from({ length: 28 }, (_, i) => i + 1).map(day => (
                      <SelectItem key={day} value={day.toString()}>
                        매월 {day}일
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-xs text-stone-500">납부일 이전에 알림을 보낼 날짜입니다</p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Auto Charge (Future Feature) */}
        <Card className="border-stone-200">
          <CardHeader>
            <CardTitle className="text-lg">자동 결제</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="flex items-center justify-between py-2">
              <div className="space-y-0.5">
                <Label className="text-base text-stone-900">자동 결제 활성화</Label>
                <p className="text-xs text-stone-500">납부일에 자동으로 회비를 결제합니다 (준비 중)</p>
              </div>
              <Switch 
                checked={autoCharge} 
                onCheckedChange={setAutoCharge}
                disabled
                className="data-[state=checked]:bg-orange-500" 
              />
            </div>
            {autoCharge && (
              <p className="text-xs text-orange-600 bg-orange-50 p-3 rounded-lg">
                자동 결제 기능은 준비 중입니다.
              </p>
            )}
          </CardContent>
        </Card>

        {/* Submit Button */}
        <div className="pt-4">
          <Button 
            onClick={handleSubmit}
            className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl"
          >
            저장하기
          </Button>
        </div>
      </div>
    </div>
  );
}

