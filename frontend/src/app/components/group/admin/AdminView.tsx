import { useState, useEffect } from 'react';
import { useParams, useOutletContext } from 'react-router-dom';
import { ChevronRight, Shield, Users, LogOut, AlertTriangle, Crown, Globe, Lock, BookOpen, Info, Wallet, Copy, Check } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Switch } from '../../ui/switch';
import { Label } from '../../ui/label';
import { Input } from '../../ui/input';
import { Badge } from '../../ui/badge';
import { getBankAccount, createBankAccount, type BankAccounts, type AccountCreateRequest } from '../../../../api/bank';
import { getMembers, leaveClub } from '../../../../api/member';
import { getMyInfo } from '../../../../api/user';
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
import { Button } from '../../ui/button';
import { Card, CardContent } from '../../ui/card';
import { ClubDetailResponse, activateClub, closeClub } from '../../../../api/club-full';

interface GroupContextType {
  club: ClubDetailResponse | null;
  loading: boolean;
}

export function AdminView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const { club } = useOutletContext<GroupContextType>();

  // 실제 API에서 역할 정보 가져오기
  const [userRole, setUserRole] = useState<'owner' | 'treasurer' | 'manager' | 'member'>('member');
  const [allRoles, setAllRoles] = useState<Array<'owner' | 'treasurer' | 'manager' | 'member'>>([]);
  const [loadingRole, setLoadingRole] = useState(true);

  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [deleteConfirmText, setDeleteConfirmText] = useState('');
  const [showLeaveDialog, setShowLeaveDialog] = useState(false);
  const [pendingCount, setPendingCount] = useState(0);
  const [isActivating, setIsActivating] = useState(false);
  const [showAccountDialog, setShowAccountDialog] = useState(false);
  const [bankAccount, setBankAccount] = useState<BankAccounts | null>(null);
  const [loadingAccount, setLoadingAccount] = useState(false);
  const [copied, setCopied] = useState(false);
  const [showCreateAccountDialog, setShowCreateAccountDialog] = useState(false);
  const [creatingAccount, setCreatingAccount] = useState(false);
  const [ownerName, setOwnerName] = useState('');
  const [bankCode] = useState('STUB');

  // 실제 API에서 사용자 역할 조회
  useEffect(() => {
    async function fetchUserRole() {
      if (!groupId) return;
      try {
        setLoadingRole(true);
        const myInfo = await getMyInfo();
        const members = await getMembers(Number(groupId), 'ACTIVE');
        const currentMember = members.find(m => m.userId === myInfo.userId);

        if (currentMember) {
          const roles = currentMember.roles || [];
          const roleArray: Array<'owner' | 'treasurer' | 'manager' | 'member'> = [];

          if (roles.includes('OWNER')) {
            roleArray.push('owner');
            setUserRole('owner');
          }
          if (roles.includes('ACCOUNTANT')) {
            roleArray.push('treasurer');
            if (!roleArray.includes('owner')) setUserRole('treasurer');
          }
          if (roles.includes('STAFF')) {
            roleArray.push('manager');
            if (!roleArray.includes('owner') && !roleArray.includes('treasurer')) setUserRole('manager');
          }
          if (roleArray.length === 0) {
            roleArray.push('member');
            setUserRole('member');
          }

          setAllRoles(roleArray);
        } else {
          setUserRole('member');
          setAllRoles(['member']);
        }
      } catch (error) {
        console.error('사용자 역할 조회 실패:', error);
        setUserRole('member');
        setAllRoles(['member']);
      } finally {
        setLoadingRole(false);
      }
    }
    fetchUserRole();
  }, [groupId]);

  // 권한 계산 (실제 역할 기반)
  const permissions = {
    canManageGroup: allRoles.includes('owner') || allRoles.includes('manager') || allRoles.includes('treasurer'),
    canManageDues: allRoles.includes('owner') || allRoles.includes('treasurer'),
    canWithdraw: allRoles.includes('owner') || allRoles.includes('treasurer'),
    canManageShares: allRoles.includes('owner') || allRoles.includes('treasurer'),
    canManageMembers: allRoles.includes('owner') || allRoles.includes('manager') || allRoles.includes('treasurer'),
    canDeletePosts: allRoles.includes('owner') || allRoles.includes('manager'),
    canDeleteComments: allRoles.includes('owner') || allRoles.includes('manager'),
    canFinalizeSchedule: allRoles.includes('owner') || allRoles.includes('treasurer') || allRoles.includes('manager'),
    canChangeManagementType: allRoles.includes('owner') || allRoles.includes('treasurer'),
    canAssignRoles: allRoles.includes('owner'),
  };

  useEffect(() => {
    async function fetchPendingCount() {
      if (!groupId) return;
      try {
        const pendingMembers = await getMembers(Number(groupId), 'PENDING');
        setPendingCount(pendingMembers.length);
      } catch (error) {
        console.error('대기 멤버 수 조회 실패:', error);
      }
    }
    fetchPendingCount();
  }, [groupId]);

  // 계좌 정보 미리 조회 (조용히 처리, 에러가 있어도 사용자에게 표시하지 않음)
  useEffect(() => {
    async function fetchAccount() {
      if (!groupId) return;

      // 여러 번 재시도
      let retryCount = 0;
      const maxRetries = 2;
      const retryDelay = 1000;

      const attemptFetch = async (): Promise<void> => {
        try {
          const account = await getBankAccount(Number(groupId));
          setBankAccount(account);
          console.log('계좌 정보 조회 성공:', account);
        } catch (error: any) {
          // 에러 메시지에서 404, 400 또는 "not found", "찾을 수 없습니다" 확인
          const errorMessage = error?.message || String(error) || '';
          const status = error?.status || error?.response?.status;
          const isNotFound = status === 404 ||
            status === 400 ||
            errorMessage.toLowerCase().includes('404') ||
            errorMessage.toLowerCase().includes('400') ||
            errorMessage.toLowerCase().includes('not found') ||
            errorMessage.toLowerCase().includes('존재하지') ||
            errorMessage.includes('찾을 수 없습니다') ||
            errorMessage.includes('계좌를 찾을 수 없습니다');

          if (isNotFound) {
            // 404/400이면 계좌가 없는 것이므로 재시도하지 않음
            console.log('계좌 정보 없음 (정상):', { status, message: errorMessage });
            setBankAccount(null);
          } else {
            // 다른 에러면 재시도
            retryCount++;
            console.warn(`계좌 정보 조회 실패 (시도 ${retryCount}/${maxRetries}):`, {
              error,
              status,
              message: errorMessage
            });

            if (retryCount < maxRetries) {
              setTimeout(attemptFetch, retryDelay);
            } else {
              console.warn('계좌 정보 조회 최종 실패:', { status, message: errorMessage });
              setBankAccount(null);
            }
          }
        }
      };

      attemptFetch();
    }
    fetchAccount();
  }, [groupId]);

  // 로딩 중일 때는 권한 체크를 하지 않음
  if (loadingRole) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  const groupName = club?.clubName || '모임';
  const currentVisibility = club?.visibility === 'PUBLIC' ? 'public' :
    club?.visibility === 'PRIVATE' ? 'private' : 'searchable';

  // 모임이 닫혔는지 확인 (status가 INACTIVE이거나 closedAt이 null이 아닌 경우)
  const isClosed = club?.status === 'INACTIVE' || club?.closedAt !== null;
  const isOwner = userRole === 'owner';

  // 은행 코드를 은행 이름으로 변환
  const getBankName = (bankCode: string): string => {
    const bankMap: Record<string, string> = {
      'KB': 'KB국민은행',
      'NH': 'NH농협은행',
      'SHINHAN': '신한은행',
      'WOORI': '우리은행',
      'HANA': '하나은행',
      'KAKAO': '카카오뱅크',
      'TOSS': '토스뱅크',
      'STUB': '오픈은행',
    };
    return bankMap[bankCode] || bankCode;
  };

  // 계좌 정보 조회
  const handleShowAccount = async () => {
    if (!groupId) return;

    // 이미 조회된 계좌 정보가 있으면 바로 표시
    if (bankAccount) {
      setShowAccountDialog(true);
      return;
    }

    // 계좌 정보를 여러 번 재시도하여 조회
    let retryCount = 0;
    const maxRetries = 3;
    const retryDelay = 500;

    const attemptFetch = async (): Promise<void> => {
      try {
        setLoadingAccount(true);
        // 계좌 정보를 다시 조회 (최신 정보 확인)
        const account = await getBankAccount(Number(groupId));
        setBankAccount(account);
        setShowAccountDialog(true);
        setLoadingAccount(false);
      } catch (error: any) {
        // 에러 메시지에서 404, 400 또는 "not found", "찾을 수 없습니다" 확인
        const errorMessage = error?.message || String(error) || '';
        const status = error?.status || error?.response?.status;
        const isNotFound = status === 404 ||
          status === 400 ||
          errorMessage.toLowerCase().includes('404') ||
          errorMessage.toLowerCase().includes('400') ||
          errorMessage.toLowerCase().includes('not found') ||
          errorMessage.toLowerCase().includes('존재하지') ||
          errorMessage.includes('찾을 수 없습니다') ||
          errorMessage.includes('계좌를 찾을 수 없습니다');

        if (isNotFound && retryCount === 0) {
          // 첫 번째 시도에서 404/400이면 계좌가 없는 것이므로 생성 다이얼로그 표시
          setLoadingAccount(false);
          setShowCreateAccountDialog(true);
        } else if (!isNotFound && retryCount < maxRetries - 1) {
          // 다른 에러면 재시도
          retryCount++;
          setTimeout(attemptFetch, retryDelay * retryCount);
        } else {
          // 최종 실패
          setLoadingAccount(false);
          toast.error('계좌 정보를 불러올 수 없습니다.');
        }
      }
    };

    attemptFetch();
  };

  // 계좌 생성
  const handleCreateAccount = async () => {
    if (!groupId) return;
    try {
      setCreatingAccount(true);
      const myInfo = await getMyInfo();

      const request: AccountCreateRequest = {
        userId: myInfo.userId,
        bankCode: bankCode || 'STUB',
        accountNumber: null, // 자동 생성
        ownerName: ownerName || myInfo.realName,
      };

      const account = await createBankAccount(Number(groupId), request);

      // 계좌 정보를 상태에 저장
      setBankAccount(account);
      setShowCreateAccountDialog(false);
      setShowAccountDialog(true);
      toast.success('계좌가 생성되었습니다');

      // 계좌 생성 후 즉시 다시 조회하여 확인 (1회만)
      setTimeout(async () => {
        try {
          const verifyAccount = await getBankAccount(Number(groupId));
          setBankAccount(verifyAccount);
        } catch (verifyError) {
          // 재시도 실패 시 생성된 계좌 정보는 유지
          setBankAccount(account);
        }
      }, 500);
    } catch (error: any) {
      console.error('계좌 생성 실패:', error);
      toast.error(error.message || '계좌 생성에 실패했습니다.');
    } finally {
      setCreatingAccount(false);
    }
  };


  // 계좌번호 복사
  const handleCopyAccount = () => {
    if (!bankAccount || !bankAccount.accountNumber) return;
    navigator.clipboard.writeText(bankAccount.accountNumber.replace(/-/g, ''));
    setCopied(true);
    toast.success('계좌번호가 복사되었습니다');
    setTimeout(() => setCopied(false), 2000);
  };

  const handleActivateClub = async () => {
    if (!groupId) {
      toast.error('모임 ID가 없습니다');
      return;
    }
    try {
      setIsActivating(true);
      await activateClub(Number(groupId));
      toast.success('모임이 활성화되었습니다');
      // 모임 정보를 다시 불러오기 위해 페이지 새로고침 또는 상태 업데이트
      window.location.reload();
    } catch (error) {
      console.error('모임 활성화 실패:', error);
      toast.error('모임 활성화에 실패했습니다.');
    } finally {
      setIsActivating(false);
    }
  };

  const handleDeleteGroup = async () => {
    if (deleteConfirmText !== groupName) {
      toast.error('모임 이름이 일치하지 않습니다');
      return;
    }
    if (!groupId) {
      toast.error('모임 ID가 없습니다');
      return;
    }
    try {
      await closeClub(Number(groupId));
      toast.success('모임이 삭제되었습니다');
      setShowDeleteDialog(false);
      setDeleteConfirmText('');
      navigate('/', { replace: true });
      setTimeout(() => {
        window.location.reload();
      }, 300);
    } catch (error: any) {
      console.error('모임 삭제 실패:', error);
      const errorMessage = error?.message || '모임 삭제에 실패했습니다.';
      toast.error(errorMessage);
    }
  };

  const handleLeaveClub = async () => {
    if (!groupId) {
      toast.error('모임 ID가 없습니다');
      return;
    }
    try {
      console.log('탈퇴 요청 시작:', { clubId: groupId });
      await leaveClub(Number(groupId));
      console.log('탈퇴 성공');
      toast.success('모임에서 탈퇴했습니다');
      setShowLeaveDialog(false);
      setTimeout(() => navigate('/'), 500);
    } catch (error: any) {
      console.error('모임 탈퇴 실패 상세:', {
        error,
        message: error?.message,
        status: error?.status,
        response: error?.response,
        stack: error?.stack
      });
      const errorMessage = error?.message || error?.response?.data?.message || '모임 탈퇴에 실패했습니다.';
      toast.error(errorMessage);
    }
  };

  // 복합 역할 표시
  const isMultiRole = allRoles.length > 1;

  // 로딩 중일 때는 권한 체크를 하지 않음
  if (loadingRole) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-20" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* 모임 정보 카드 */}
      {club && (
        <Card className="bg-gradient-to-r from-stone-800 to-stone-900 text-white border-none">
          <CardContent className="p-4">
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <h3 className="font-bold text-lg mb-2">{club.clubName}</h3>
                <div className="flex items-center gap-2">
                  <Badge className={club.type === 'FAIR_SETTLEMENT' ? 'bg-green-500/20 text-green-300' : 'bg-blue-500/20 text-blue-300'}>
                    {club.type === 'FAIR_SETTLEMENT' ? '공정정산형' : '운영비형'}
                  </Badge>
                  <span className="text-xs text-stone-400">|</span>
                  <span className="text-xs text-stone-400">{club.currentMembers || 0}명</span>
                  {isClosed && (
                    <>
                      <span className="text-xs text-stone-400">|</span>
                      <Badge className="bg-red-500/20 text-red-300">폐쇄됨</Badge>
                    </>
                  )}
                </div>
              </div>
              {isClosed && isOwner && (
                <Button
                  onClick={handleActivateClub}
                  disabled={isActivating}
                  className="bg-green-600 hover:bg-green-700 text-white"
                  size="sm"
                >
                  {isActivating ? '활성화 중...' : '모임 활성화'}
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* 모임 관리 - 모임장/운영진 */}
      {permissions.canManageGroup && (
        <div className="bg-white rounded-xl border border-stone-100 divide-y divide-stone-50 overflow-hidden">
          <div className="px-4 py-3 bg-stone-50">
            <h3 className="font-medium text-stone-700">모임 관리</h3>
          </div>

          <Link to="edit-group" className="block">
            <div className="p-4 flex items-center justify-between hover:bg-stone-50 cursor-pointer">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-orange-100 text-orange-600 rounded-lg">
                  <Shield className="w-5 h-5" />
                </div>
                <div>
                  <p className="font-medium text-stone-900">모임 정보 수정</p>
                  <p className="text-xs text-stone-500">이름, 커버 이미지, 태그</p>
                </div>
              </div>
              <ChevronRight className="w-5 h-5 text-stone-300" />
            </div>
          </Link>

          <Link to="privacy" className="block">
            <div className="p-4 flex items-center justify-between hover:bg-stone-50 cursor-pointer">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-purple-100 text-purple-600 rounded-lg">
                  {currentVisibility === 'private' ? (
                    <Lock className="w-5 h-5" />
                  ) : (
                    <Globe className="w-5 h-5" />
                  )}
                </div>
                <div>
                  <p className="font-medium text-stone-900">공개 설정</p>
                  <p className="text-xs text-stone-500">
                    {currentVisibility === 'private' && '비공개'}
                    {currentVisibility === 'searchable' && '검색 허용'}
                    {currentVisibility === 'public' && '완전 공개'}
                  </p>
                </div>
              </div>
              <ChevronRight className="w-5 h-5 text-stone-300" />
            </div>
          </Link>
        </div>
      )}

      {/* 통장 관리 - 모든 멤버 */}
      <div className="bg-white rounded-xl border border-stone-100 divide-y divide-stone-50 overflow-hidden">
        <div className="px-4 py-3 bg-stone-50">
          <h3 className="font-medium text-stone-700">통장 관리</h3>
        </div>

        {/* 장부 관리 */}
        <Link to="../dues/ledger" className="block">
          <div className="p-4 flex items-center justify-between hover:bg-stone-50 cursor-pointer">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-indigo-100 text-indigo-600 rounded-lg">
                <BookOpen className="w-5 h-5" />
              </div>
              <div>
                <p className="font-medium text-stone-900">장부 관리</p>
                <p className="text-xs text-stone-500">모임통장 사용 내역 조회</p>
              </div>
            </div>
            <ChevronRight className="w-5 h-5 text-stone-300" />
          </div>
        </Link>

        {/* 계좌 */}
        <button onClick={handleShowAccount} className="block w-full text-left">
          <div className="p-4 flex items-center justify-between hover:bg-stone-50 cursor-pointer">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-blue-100 text-blue-600 rounded-lg">
                <Wallet className="w-5 h-5" />
              </div>
              <div>
                <p className="font-medium text-stone-900">계좌 확인</p>
                <p className="text-xs text-stone-500">모임통장 계좌 정보 및 사용 안내</p>
              </div>
            </div>
            <ChevronRight className="w-5 h-5 text-stone-300" />
          </div>
        </button>
      </div>

      {/* 멤버/권한 관리 */}
      {(permissions.canManageMembers || permissions.canAssignRoles) && (
        <div className="bg-white rounded-xl border border-stone-100 divide-y divide-stone-50 overflow-hidden">
          <div className="px-4 py-3 bg-stone-50">
            <h3 className="font-medium text-stone-700">멤버 관리</h3>
          </div>

          {permissions.canManageMembers && (
            <Link to="members" className="block">
              <div className="p-4 flex items-center justify-between hover:bg-stone-50 cursor-pointer">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-green-100 text-green-600 rounded-lg">
                    <Users className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="font-medium text-stone-900">멤버 관리</p>
                    <p className="text-xs text-stone-500">가입 승인, 추방</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {pendingCount > 0 && (
                    <Badge variant="secondary" className="bg-orange-100 text-orange-700">
                      {pendingCount}명 대기
                    </Badge>
                  )}
                  <ChevronRight className="w-5 h-5 text-stone-300" />
                </div>
              </div>
            </Link>
          )}

          {/* 권한 관리 - 모임장만 */}
          {permissions.canAssignRoles && (
            <Link to="roles" className="block">
              <div className="p-4 flex items-center justify-between hover:bg-stone-50 cursor-pointer">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-yellow-100 text-yellow-600 rounded-lg">
                    <Crown className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="font-medium text-stone-900">권한 관리</p>
                    <p className="text-xs text-stone-500">총무, 운영진 지정 (중복 가능)</p>
                  </div>
                </div>
                <ChevronRight className="w-5 h-5 text-stone-300" />
              </div>
            </Link>
          )}
        </div>
      )}

      {/* 알림 설정 - 모임장/운영진만 */}
      {permissions.canManageGroup && (
        <div className="bg-white rounded-xl border border-stone-100 p-4 space-y-6">
          <h3 className="font-bold text-stone-900">알림 설정</h3>

          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <Label className="text-base text-stone-900">새 멤버 가입 알림</Label>
            </div>
            <Switch defaultChecked className="data-[state=checked]:bg-orange-500" />
          </div>
        </div>
      )}

      {/* 모임 탈퇴 - 모임장이 아닌 모든 멤버 */}
      {userRole !== 'owner' && (
        <button
          onClick={() => setShowLeaveDialog(true)}
          className="w-full p-4 rounded-xl border border-stone-200 bg-white text-stone-700 font-medium flex items-center justify-center gap-2 hover:bg-stone-50 transition-colors"
        >
          <LogOut className="w-5 h-5" />
          모임 탈퇴하기
        </button>
      )}

      {/* 위험 영역 - 모임장만 */}
      {userRole === 'owner' && (
        <button
          onClick={() => setShowDeleteDialog(true)}
          className="w-full p-4 rounded-xl border border-red-100 bg-red-50 text-red-600 font-medium flex items-center justify-center gap-2 hover:bg-red-100 transition-colors"
        >
          <LogOut className="w-5 h-5" />
          모임 삭제하기
        </button>
      )}



      {/* 권한 안내 박스 */}
      {isMultiRole && (
        <Card className="border-purple-200 bg-purple-50">
          <CardContent className="p-4">
            <div className="flex gap-3">
              <Info className="w-5 h-5 text-purple-600 shrink-0" />
              <div className="text-sm text-purple-800">
                <p className="font-medium">복합 권한 보유</p>
                <p className="text-xs text-purple-700 mt-1">
                  운영진과 총무 권한을 동시에 가지고 있어 모든 관련 기능을 사용할 수 있습니다.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}


      {/* Account Dialog */}
      <AlertDialog open={showAccountDialog} onOpenChange={setShowAccountDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="text-xl">계좌 확인</AlertDialogTitle>
          </AlertDialogHeader>
          {(() => {
            if (loadingAccount) {
              return <div className="py-8 text-center text-stone-500">로딩 중...</div>;
            }
            if (bankAccount) {
              return (
                <div className="space-y-4 pt-2">
                  <div className="bg-stone-50 rounded-xl p-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-stone-600">은행</span>
                      <span className="font-medium text-stone-900">{getBankName(bankAccount.bankCode)}</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-stone-600">계좌번호</span>
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-stone-900 font-mono">{bankAccount.accountNumber || '계좌번호 없음'}</span>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={handleCopyAccount}
                          className="h-8 px-2"
                        >
                          {copied ? <Check className="w-4 h-4 text-green-600" /> : <Copy className="w-4 h-4" />}
                        </Button>
                      </div>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-stone-600">예금주</span>
                      <span className="font-medium text-stone-900">{bankAccount.depositorName}</span>
                    </div>
                    {bankAccount.createdAt && (
                      <div className="flex items-center justify-between">
                        <span className="text-stone-600">계좌 생성일</span>
                        <span className="font-medium text-stone-900 text-sm">
                          {new Date(bankAccount.createdAt).toLocaleDateString('ko-KR', {
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric'
                          })}
                        </span>
                      </div>
                    )}
                  </div>

                  <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 space-y-2">
                    <p className="text-sm font-medium text-blue-900">💡 계좌 사용 안내</p>
                    <ul className="text-xs text-blue-800 space-y-1 list-disc list-inside">
                      <li>이체 시 입금자명을 꼭 본인 이름으로 남겨주세요.</li>
                      <li>계좌는 모임 생성 시 자동으로 발급되며 변경할 수 없습니다.</li>
                      <li>회비 입금 시 이 계좌번호로 입금해주세요.</li>
                    </ul>
                  </div>
                </div>
              );
            }
            return (
              <div className="py-8 text-center text-stone-500">
                계좌 정보를 불러올 수 없습니다.
              </div>
            );
          })()}
          <AlertDialogFooter>
            <AlertDialogCancel>닫기</AlertDialogCancel>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Create Account Dialog */}
      <AlertDialog open={showCreateAccountDialog} onOpenChange={setShowCreateAccountDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="text-xl">계좌 생성</AlertDialogTitle>
            <AlertDialogDescription>
              <div className="space-y-4 pt-4">
                <p className="text-sm text-stone-600">
                  모임 계좌가 없습니다. 계좌를 생성하시겠습니까?
                </p>
                <div className="space-y-2">
                  <Label htmlFor="ownerName">예금주명</Label>
                  <Input
                    id="ownerName"
                    value={ownerName}
                    onChange={(e) => setOwnerName(e.target.value)}
                    placeholder="예금주명을 입력하세요"
                    className="h-10"
                  />
                </div>
                <p className="text-xs text-stone-500">
                  💡 기본값으로 오픈은행 계좌가 자동 생성됩니다.
                </p>
              </div>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => setShowCreateAccountDialog(false)}>
              취소
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleCreateAccount}
              disabled={creatingAccount || !ownerName.trim()}
              className="bg-orange-500 hover:bg-orange-600"
            >
              {creatingAccount ? '생성 중...' : '계좌 생성'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Delete Group Dialog */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2 bg-red-100 rounded-full">
                <AlertTriangle className="w-6 h-6 text-red-600" />
              </div>
              <AlertDialogTitle className="text-xl">모임 삭제</AlertDialogTitle>
            </div>
            <AlertDialogDescription className="space-y-4">
              <p>
                이 작업은 되돌릴 수 없습니다. 모든 일정, 회비 내역, 앨범이
                영구적으로 삭제됩니다.
              </p>
              <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                <p className="text-sm text-red-800">
                  삭제를 확인하려면 아래에 <strong>"{groupName}"</strong>을 입력하세요.
                </p>
              </div>
              <Input
                placeholder="모임 이름을 입력하세요"
                value={deleteConfirmText}
                onChange={(e) => setDeleteConfirmText(e.target.value)}
                className="border-stone-300"
              />
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => setDeleteConfirmText('')}>
              취소
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteGroup}
              disabled={deleteConfirmText !== groupName}
              className="bg-red-500 hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              삭제하기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Leave Club Dialog */}
      <AlertDialog open={showLeaveDialog} onOpenChange={setShowLeaveDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>모임 탈퇴</AlertDialogTitle>
            <AlertDialogDescription>
              정말 이 모임에서 탈퇴하시겠습니까?
              <br /><br />
              탈퇴 후에는 모임의 모든 활동에 참여할 수 없으며, 다시 가입하려면 새로 신청해야 합니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleLeaveClub}
              className="bg-red-500 hover:bg-red-600"
            >
              탈퇴하기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
