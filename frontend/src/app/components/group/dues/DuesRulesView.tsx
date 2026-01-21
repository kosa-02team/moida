import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Info, CreditCard, Calendar, Bell, AlertCircle } from 'lucide-react';
import { Button } from '../../ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../../ui/card';
import { Badge } from '../../ui/badge';
import { Separator } from '../../ui/separator';

export function DuesRulesView() {
  const navigate = useNavigate();

  // Mock data - 실제로는 API에서 가져와야 함
  const rules = {
    amount: 30000,
    paymentDay: 25,
    allowMultiplePayments: true,
    autoNotification: true,
    notificationDay: 20,
    gracePeriod: 7, // 연체 유예 기간 (일)
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100 backdrop-blur-sm bg-white/95">
        <div className="flex items-center justify-between px-4 py-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="font-bold text-lg text-stone-800">회비 규칙</h1>
          <div className="w-10" />
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Info Banner */}
        <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
          <div className="flex items-start gap-3">
            <Info className="w-5 h-5 text-blue-600 mt-0.5 flex-shrink-0" />
            <div className="flex-1">
              <p className="text-sm text-blue-900 font-medium mb-1">회비 규칙 안내</p>
              <p className="text-xs text-blue-700">
                아래 규칙은 현재 모임에 적용된 회비 정책입니다. 규칙 변경은 관리자만 할 수 있습니다.
              </p>
            </div>
          </div>
        </div>

        {/* Basic Rules */}
        <Card className="border-stone-200">
          <CardHeader>
            <CardTitle className="text-lg flex items-center gap-2">
              <CreditCard className="w-5 h-5 text-orange-500" />
              기본 규칙
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between py-2">
              <div>
                <p className="font-medium text-stone-900">회비 금액</p>
                <p className="text-xs text-stone-500 mt-0.5">월 회비 금액</p>
              </div>
              <Badge className="bg-orange-100 text-orange-700 text-base px-3 py-1">
                {rules.amount.toLocaleString()}원
              </Badge>
            </div>

            <Separator />

            <div className="flex items-center justify-between py-2">
              <div>
                <p className="font-medium text-stone-900">납부일</p>
                <p className="text-xs text-stone-500 mt-0.5">매월 회비 납부 마감일</p>
              </div>
              <div className="flex items-center gap-2">
                <Calendar className="w-4 h-4 text-stone-400" />
                <span className="font-semibold text-stone-900">매월 {rules.paymentDay}일</span>
              </div>
            </div>

            <Separator />

            <div className="flex items-center justify-between py-2">
              <div>
                <p className="font-medium text-stone-900">중복 납부 허용</p>
                <p className="text-xs text-stone-500 mt-0.5">여러 달 회비를 한 번에 납부 가능</p>
              </div>
              <Badge variant={rules.allowMultiplePayments ? "default" : "secondary"} 
                     className={rules.allowMultiplePayments ? "bg-green-100 text-green-700" : ""}>
                {rules.allowMultiplePayments ? '허용' : '불가'}
              </Badge>
            </div>
          </CardContent>
        </Card>

        {/* Notification Rules */}
        <Card className="border-stone-200">
          <CardHeader>
            <CardTitle className="text-lg flex items-center gap-2">
              <Bell className="w-5 h-5 text-blue-500" />
              알림 규칙
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between py-2">
              <div>
                <p className="font-medium text-stone-900">자동 미납 알림</p>
                <p className="text-xs text-stone-500 mt-0.5">미납 시 자동으로 알림 발송</p>
              </div>
              <Badge variant={rules.autoNotification ? "default" : "secondary"}
                     className={rules.autoNotification ? "bg-blue-100 text-blue-700" : ""}>
                {rules.autoNotification ? '활성화' : '비활성화'}
              </Badge>
            </div>

            {rules.autoNotification && (
              <>
                <Separator />
                <div className="flex items-center justify-between py-2">
                  <div>
                    <p className="font-medium text-stone-900">알림 발송일</p>
                    <p className="text-xs text-stone-500 mt-0.5">납부일 이전 알림 발송일</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Bell className="w-4 h-4 text-stone-400" />
                    <span className="font-semibold text-stone-900">매월 {rules.notificationDay}일</span>
                  </div>
                </div>
              </>
            )}
          </CardContent>
        </Card>

        {/* Overdue Rules */}
        <Card className="border-stone-200">
          <CardHeader>
            <CardTitle className="text-lg flex items-center gap-2">
              <AlertCircle className="w-5 h-5 text-red-500" />
              연체 규칙
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between py-2">
              <div>
                <p className="font-medium text-stone-900">연체 유예 기간</p>
                <p className="text-xs text-stone-500 mt-0.5">납부일 이후 연체 처리까지 기간</p>
              </div>
              <Badge className="bg-yellow-100 text-yellow-700">
                {rules.gracePeriod}일
              </Badge>
            </div>

            <div className="bg-stone-50 rounded-lg p-4 mt-4">
              <p className="text-xs text-stone-600 leading-relaxed">
                <strong>예시:</strong> 납부일이 매월 25일이고 유예 기간이 7일인 경우, 
                6월 25일까지 납부하지 않으면 7월 2일부터 연체 처리됩니다.
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Payment Methods */}
        <Card className="border-stone-200">
          <CardHeader>
            <CardTitle className="text-lg">납부 방법</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-start gap-3 p-3 bg-stone-50 rounded-lg">
              <div className="p-2 bg-white rounded-lg">
                <CreditCard className="w-5 h-5 text-stone-600" />
              </div>
              <div className="flex-1">
                <p className="font-medium text-stone-900 mb-1">계좌 이체</p>
                <p className="text-xs text-stone-500">국민은행 123-456-789012</p>
                <p className="text-xs text-stone-500">예금주: 주말 등산 클럽</p>
              </div>
            </div>

            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
              <p className="text-xs text-blue-800">
                💡 이체 시 입금자명을 꼭 본인 이름으로 남겨주세요. 확인이 어려울 경우 연락 주시기 바랍니다.
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Footer Note */}
        <div className="text-center py-4">
          <p className="text-xs text-stone-500">
            규칙 변경을 원하시면 관리자에게 문의하세요.
          </p>
        </div>
      </div>
    </div>
  );
}

