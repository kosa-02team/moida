import { useState, useEffect } from 'react';
import { useParams, useOutletContext } from 'react-router-dom';
import { Wallet, ArrowDownLeft, ArrowUpRight, History, Receipt, Info, ChevronRight, Users, AlertCircle } from 'lucide-react';
import { Button } from '../../ui/button';
import { Card, CardContent } from '../../ui/card';
import { Link } from 'react-router-dom';
import { Badge } from '../../ui/badge';
import { Progress } from '../../ui/progress';
import { 
  useUserRole, 
  useUserPermissions,
  getRoleLabel,
  getRoleColor,
} from '../../../data/userRoles';
import { ClubDetailResponse } from '../../../../api/club-full';
import { getLedger, type TransactionLogResponse } from '../../../../api/ledger';

interface GroupContextType {
  club: ClubDetailResponse | null;
  loading: boolean;
}

export function DuesView() {
  const { groupId } = useParams();
  const { club, loading: clubLoading } = useOutletContext<GroupContextType>();
  
  // 모임별 역할 가져오기
  const { userRole } = useUserRole(groupId || '1');
  const permissions = useUserPermissions(groupId || '1');
  
  // 권한 체크
  const showWithdrawButton = permissions.canWithdraw;

  const [transactions, setTransactions] = useState<TransactionLogResponse[]>([]);
  const [loadingTransactions, setLoadingTransactions] = useState(true);

  useEffect(() => {
    async function fetchTransactions() {
      if (!groupId) return;
      try {
        setLoadingTransactions(true);
        // 최근 5개만 가져오기
        const data = await getLedger(Number(groupId));
        setTransactions(data.slice(0, 5));
      } catch (error) {
        console.error('거래 내역 조회 실패:', error);
      } finally {
        setLoadingTransactions(false);
      }
    }
    fetchTransactions();
  }, [groupId]);

  // 그룹이 없으면 에러 표시
  if (clubLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  if (!club) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
        <AlertCircle className="w-12 h-12 text-red-400 mb-4" />
        <h3 className="text-lg font-medium text-stone-900">모임을 찾을 수 없습니다</h3>
        <p className="text-sm text-stone-500 mt-1">존재하지 않는 모임입니다.</p>
      </div>
    );
  }

  const isFairType = club.type === 'FAIR_SETTLEMENT';

  return (
    <div className="space-y-6 pb-20" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* User Role Badge */}
      <div className="flex justify-end">
        <Badge className={`${getRoleColor(groupId || '1')} text-xs`}>
          {getRoleLabel(groupId || '1')}
        </Badge>
      </div>

          {/* Balance Card */}
      <Card className="bg-gradient-to-br from-stone-900 to-stone-800 text-white border-none shadow-lg rounded-2xl overflow-hidden relative">
        <div className="absolute top-0 right-0 w-32 h-32 bg-white/5 rounded-full -mr-10 -mt-10 blur-2xl"></div>
        <CardContent className="p-6 relative z-10">
          <div className="flex justify-between items-start mb-4">
            <div>
              <p className="text-stone-400 text-sm mb-1">총 모임 통장 잔액</p>
              <h2 className="text-3xl font-bold">
                {transactions.length > 0 
                  ? transactions[0].balanceAfter.toLocaleString() 
                  : '0'}원
              </h2>
            </div>
            <div className="p-2 bg-white/10 rounded-full">
              <Wallet className="w-6 h-6 text-orange-400" />
            </div>
          </div>

          {/* Management Type Badge */}
          <div className="mb-4">
            <Badge className={
              isFairType
                ? 'bg-green-500/20 text-green-300 border-green-500/30' 
                : 'bg-blue-500/20 text-blue-300 border-blue-500/30'
            }>
              {isFairType ? '공정정산형' : '운영비형'}
            </Badge>
          </div>

          {/* 멤버 수 정보 */}
          <div className="bg-white/10 rounded-xl p-4 mb-4">
            <div className="flex items-center gap-2 mb-3">
              <Users className="w-4 h-4 text-stone-300" />
              <span className="text-sm text-stone-300">멤버 수: {club.currentMembers || 0}명</span>
            </div>
          </div>
          
          {/* Action Buttons */}
          <div className="flex gap-4">
            <Link to="deposit" className="flex-1">
              <Button className="w-full bg-orange-500 hover:bg-orange-600 text-white border-none h-12 rounded-xl">
                <ArrowDownLeft className="w-4 h-4 mr-2" />
                채우기
              </Button>
            </Link>
            
            {showWithdrawButton ? (
              <Link to="withdraw" className="flex-1">
                <Button variant="secondary" className="w-full bg-white/10 hover:bg-white/20 text-white border-none h-12 rounded-xl">
                  <ArrowUpRight className="w-4 h-4 mr-2" />
                  보내기
                </Button>
              </Link>
            ) : (
              <Button 
                variant="secondary" 
                disabled 
                className="flex-1 bg-white/5 text-white/50 border-none h-12 rounded-xl cursor-not-allowed"
              >
                <ArrowUpRight className="w-4 h-4 mr-2" />
                보내기
              </Button>
            )}
          </div>
          
          {!showWithdrawButton && (
            <p className="text-xs text-stone-500 text-center mt-2">
              보내기는 모임장/총무만 가능합니다
            </p>
          )}
        </CardContent>
      </Card>

      {/* Quick Actions */}
      <div className="grid grid-cols-2 gap-4">
        <Link to="settlement-request">
          <Button variant="outline" className="w-full h-20 flex flex-col items-center justify-center gap-2 border-stone-200 hover:bg-stone-50 hover:border-orange-200 rounded-xl group">
            <div className="p-2 bg-stone-100 rounded-full group-hover:bg-orange-100 transition-colors">
              <Receipt className="w-5 h-5 text-stone-600 group-hover:text-orange-600" />
            </div>
            <span className="text-sm font-medium text-stone-600">정산 요청</span>
          </Button>
        </Link>
        <Link to="rules">
          <Button variant="outline" className="w-full h-20 flex flex-col items-center justify-center gap-2 border-stone-200 hover:bg-stone-50 hover:border-orange-200 rounded-xl group">
            <div className="p-2 bg-stone-100 rounded-full group-hover:bg-orange-100 transition-colors">
              <History className="w-5 h-5 text-stone-600 group-hover:text-orange-600" />
            </div>
            <span className="text-sm font-medium text-stone-600">회비 규칙</span>
          </Button>
        </Link>
      </div>

      {/* 장부 관리 Link (총무/모임장만) */}
      {showWithdrawButton && (
        <>
          <Link to="ledger">
            <Card className="border-indigo-200 bg-indigo-50 hover:bg-indigo-100 transition-colors cursor-pointer">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-indigo-100 rounded-full flex items-center justify-center">
                      <History className="w-5 h-5 text-indigo-600" />
                    </div>
                    <div>
                      <p className="font-medium text-indigo-900">장부 관리</p>
                      <p className="text-xs text-indigo-700">모임통장 사용 내역 조회</p>
                    </div>
                  </div>
                  <ChevronRight className="w-5 h-5 text-indigo-600" />
                </div>
              </CardContent>
            </Card>
          </Link>
          <Link to="payment-requests">
            <Card className="border-purple-200 bg-purple-50 hover:bg-purple-100 transition-colors cursor-pointer">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-purple-100 rounded-full flex items-center justify-center">
                      <Receipt className="w-5 h-5 text-purple-600" />
                    </div>
                    <div>
                      <p className="font-medium text-purple-900">입금 요청 관리</p>
                      <p className="text-xs text-purple-700">입금 요청 목록 및 확인</p>
                    </div>
                  </div>
                  <ChevronRight className="w-5 h-5 text-purple-600" />
                </div>
              </CardContent>
            </Card>
          </Link>
          <Link to="bank/create">
            <Card className="border-blue-200 bg-blue-50 hover:bg-blue-100 transition-colors cursor-pointer">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                      <Wallet className="w-5 h-5 text-blue-600" />
                    </div>
                    <div>
                      <p className="font-medium text-blue-900">가상계좌 생성</p>
                      <p className="text-xs text-blue-700">모임 가상계좌 발급</p>
                    </div>
                  </div>
                  <ChevronRight className="w-5 h-5 text-blue-600" />
                </div>
              </CardContent>
            </Card>
          </Link>
          <Link to="bank/sync">
            <Card className="border-green-200 bg-green-50 hover:bg-green-100 transition-colors cursor-pointer">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
                      <ArrowDownLeft className="w-5 h-5 text-green-600" />
                    </div>
                    <div>
                      <p className="font-medium text-green-900">거래내역 동기화</p>
                      <p className="text-xs text-green-700">은행 거래내역 가져오기</p>
                    </div>
                  </div>
                  <ChevronRight className="w-5 h-5 text-green-600" />
                </div>
              </CardContent>
            </Card>
          </Link>
          <Link to="bank/transactions/processed">
            <Card className="border-cyan-200 bg-cyan-50 hover:bg-cyan-100 transition-colors cursor-pointer">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-cyan-100 rounded-full flex items-center justify-center">
                      <History className="w-5 h-5 text-cyan-600" />
                    </div>
                    <div>
                      <p className="font-medium text-cyan-900">처리된 거래내역</p>
                      <p className="text-xs text-cyan-700">매칭 정보 포함 거래내역</p>
                    </div>
                  </div>
                  <ChevronRight className="w-5 h-5 text-cyan-600" />
                </div>
              </CardContent>
            </Card>
          </Link>
          <Link to="bank/transactions/unmatched">
            <Card className="border-yellow-200 bg-yellow-50 hover:bg-yellow-100 transition-colors cursor-pointer">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-yellow-100 rounded-full flex items-center justify-center">
                      <AlertCircle className="w-5 h-5 text-yellow-600" />
                    </div>
                    <div>
                      <p className="font-medium text-yellow-900">미매칭 거래내역</p>
                      <p className="text-xs text-yellow-700">매칭되지 않은 거래 확인</p>
                    </div>
                  </div>
                  <ChevronRight className="w-5 h-5 text-yellow-600" />
                </div>
              </CardContent>
            </Card>
          </Link>
          <Link to="bank/refund">
            <Card className="border-red-200 bg-red-50 hover:bg-red-100 transition-colors cursor-pointer">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
                      <ArrowUpRight className="w-5 h-5 text-red-600" />
                    </div>
                    <div>
                      <p className="font-medium text-red-900">환급하기</p>
                      <p className="text-xs text-red-700">회원에게 환급 처리</p>
                    </div>
                  </div>
                  <ChevronRight className="w-5 h-5 text-red-600" />
                </div>
              </CardContent>
            </Card>
          </Link>
        </>
      )}

      {/* Info Box - 통장 유형 안내 */}
      <Card className={isFairType ? 'border-green-200 bg-green-50' : 'border-blue-200 bg-blue-50'}>
        <CardContent className="p-4">
          <div className="flex gap-3">
            <Info className={`w-5 h-5 shrink-0 ${isFairType ? 'text-green-600' : 'text-blue-600'}`} />
            <div className={`text-sm ${isFairType ? 'text-green-800' : 'text-blue-800'}`}>
              <p className="font-medium">{isFairType ? '공정정산형' : '운영비형'} 안내</p>
              <ul className="mt-2 space-y-1">
                {isFairType ? (
                  <>
                    <li className="text-xs text-green-700">• 모든 멤버 동일 지분</li>
                    <li className="text-xs text-green-700">• 신규 가입 시 기존 멤버 지분만큼 납부</li>
                    <li className="text-xs text-green-700">• 탈퇴 시 지분만큼 환불</li>
                  </>
                ) : (
                  <>
                    <li className="text-xs text-blue-700">• 탈퇴 시 환불 없음</li>
                    <li className="text-xs text-blue-700">• 남은 돈 계속 축적</li>
                    <li className="text-xs text-blue-700">• 운영비로 자유롭게 사용</li>
                  </>
                )}
              </ul>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* History */}
      <div className="space-y-4">
        <h3 className="font-bold text-lg text-stone-800 px-1">최근 내역</h3>
        {loadingTransactions ? (
          <div className="text-center py-8 text-stone-500">로딩 중...</div>
        ) : transactions.length === 0 ? (
          <div className="bg-white rounded-2xl border border-stone-100 shadow-sm p-8 text-center">
            <p className="text-stone-500">거래 내역이 없습니다</p>
          </div>
        ) : (
          <div className="bg-white rounded-2xl border border-stone-100 shadow-sm overflow-hidden">
            {transactions.map((tx, i) => {
              const isDeposit = tx.type === 'DEPOSIT';
              return (
                <div key={tx.transactionId} className={`p-4 flex justify-between items-center ${i !== transactions.length - 1 ? 'border-b border-stone-100' : ''}`}>
                  <div className="flex items-center gap-3">
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center ${isDeposit ? 'bg-blue-50 text-blue-600' : 'bg-red-50 text-red-600'}`}>
                      {isDeposit ? <ArrowDownLeft className="w-5 h-5" /> : <ArrowUpRight className="w-5 h-5" />}
                    </div>
                    <div>
                      <p className="font-medium text-stone-900">{tx.description || (isDeposit ? '입금' : '출금')}</p>
                      <p className="text-xs text-stone-400">
                        {new Date(tx.createdAt).toLocaleDateString('ko-KR')}
                      </p>
                    </div>
                  </div>
                  <span className={`font-bold ${isDeposit ? 'text-blue-600' : 'text-stone-900'}`}>
                    {isDeposit ? '+' : '-'}{Math.abs(tx.amount).toLocaleString()}원
                  </span>
                </div>
              );
            })}
            <Link to="ledger" className="block p-3 text-center border-t border-stone-50">
              <span className="text-sm text-stone-500 hover:text-stone-800 font-medium">전체 내역 보기</span>
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
