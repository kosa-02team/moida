import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Plus, CheckCircle, Clock, XCircle, Calendar } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Badge } from '../../ui/badge';
import { Card, CardContent } from '../../ui/card';
import { getPaymentRequests, confirmPaymentRequest, type PaymentRequest } from '../../../../api/payment-request';
import { useUserPermissions } from '../../../data/userRoles';

export function PaymentRequestListView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [paymentRequests, setPaymentRequests] = useState<PaymentRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [confirmingId, setConfirmingId] = useState<number | null>(null);

  useEffect(() => {
    async function fetchPaymentRequests() {
      if (!groupId) return;
      try {
        setLoading(true);
        const requests = await getPaymentRequests(Number(groupId));
        setPaymentRequests(requests);
      } catch (error) {
        console.error('입금 요청 목록 조회 실패:', error);
        toast.error('입금 요청 목록을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    }
    fetchPaymentRequests();
  }, [groupId]);

  const handleConfirm = async (requestId: number) => {
    if (!groupId) return;
    try {
      setConfirmingId(requestId);
      await confirmPaymentRequest(Number(groupId), requestId);
      toast.success('입금이 확인되었습니다.');
      // 목록 새로고침
      const requests = await getPaymentRequests(Number(groupId));
      setPaymentRequests(requests);
    } catch (error) {
      console.error('입금 확인 실패:', error);
      toast.error('입금 확인에 실패했습니다.');
    } finally {
      setConfirmingId(null);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <Badge className="bg-yellow-100 text-yellow-700">대기중</Badge>;
      case 'MATCHED':
        return <Badge className="bg-green-100 text-green-700">매칭완료</Badge>;
      case 'EXPIRED':
        return <Badge className="bg-red-100 text-red-700">만료</Badge>;
      default:
        return <Badge variant="secondary">{status}</Badge>;
    }
  };

  const getRequestTypeLabel = (type: string) => {
    switch (type) {
      case 'MEMBERSHIP_FEE':
        return '회비';
      case 'SETTLEMENT':
        return '정산';
      case 'DEPOSIT':
        return '입금';
      default:
        return type;
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  // 권한 체크
  if (!permissions.canWithdraw) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-stone-500">입금 요청 관리는 총무 이상만 가능합니다.</p>
          <Button onClick={() => navigate(-1)} className="mt-4">
            돌아가기
          </Button>
        </div>
      </div>
    );
  }

  const pendingRequests = paymentRequests.filter(r => r.status === 'PENDING');
  const matchedRequests = paymentRequests.filter(r => r.status === 'MATCHED');
  const expiredRequests = paymentRequests.filter(r => r.status === 'EXPIRED');

  return (
    <div className="min-h-screen bg-stone-50 pb-20" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center">
            <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
              <ArrowLeft className="w-6 h-6 text-stone-800" />
            </Button>
            <h1 className="ml-2 text-lg font-semibold text-stone-800">입금 요청 관리</h1>
          </div>
          <Button
            onClick={() => navigate('../payment-requests/create')}
            size="sm"
            className="bg-orange-500 hover:bg-orange-600"
          >
            <Plus className="w-4 h-4 mr-1" />
            요청 생성
          </Button>
        </div>
      </header>

      <div className="p-5 space-y-4">
        {/* 통계 */}
        <div className="grid grid-cols-3 gap-3">
          <Card>
            <CardContent className="p-4 text-center">
              <div className="text-2xl font-bold text-yellow-600">{pendingRequests.length}</div>
              <div className="text-xs text-stone-500 mt-1">대기중</div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4 text-center">
              <div className="text-2xl font-bold text-green-600">{matchedRequests.length}</div>
              <div className="text-xs text-stone-500 mt-1">매칭완료</div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4 text-center">
              <div className="text-2xl font-bold text-red-600">{expiredRequests.length}</div>
              <div className="text-xs text-stone-500 mt-1">만료</div>
            </CardContent>
          </Card>
        </div>

        {/* 입금 요청 목록 */}
        {loading ? (
          <div className="text-center py-8 text-stone-500">로딩 중...</div>
        ) : paymentRequests.length === 0 ? (
          <Card>
            <CardContent className="p-8 text-center">
              <p className="text-stone-500">입금 요청이 없습니다</p>
            </CardContent>
          </Card>
        ) : (
          <div className="space-y-3">
            {paymentRequests.map((request) => (
              <Card key={request.requestId} className="border-stone-100">
                <CardContent className="p-4">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <span className="font-medium text-stone-900">{request.memberName}</span>
                        {getStatusBadge(request.status)}
                        <Badge variant="secondary" className="text-xs">
                          {getRequestTypeLabel(request.requestType)}
                        </Badge>
                      </div>
                      <div className="flex items-center gap-4 text-sm text-stone-600 mb-2">
                        <div className="flex items-center gap-1">
                          <Calendar className="w-3 h-3" />
                          <span>예상일: {formatDate(request.expectedDate)}</span>
                        </div>
                      </div>
                      <div className="text-lg font-bold text-stone-900">
                        {request.expectedAmount.toLocaleString()}원
                      </div>
                      {request.matchedAt && (
                        <div className="text-xs text-stone-500 mt-1">
                          매칭일: {formatDate(request.matchedAt)}
                        </div>
                      )}
                      {request.expiresAt && (
                        <div className="text-xs text-stone-500 mt-1">
                          만료일: {formatDate(request.expiresAt)}
                        </div>
                      )}
                    </div>
                    {request.status === 'MATCHED' && request.matchType === 'AUTO_MATCHED' && (
                      <Button
                        onClick={() => handleConfirm(request.requestId)}
                        disabled={confirmingId === request.requestId}
                        size="sm"
                        className="bg-green-500 hover:bg-green-600"
                      >
                        {confirmingId === request.requestId ? '확인 중...' : '확인'}
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
