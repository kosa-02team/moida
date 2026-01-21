import { useState, useEffect } from 'react';
import { useParams, useOutletContext } from 'react-router-dom';
import { ChevronRight, Shield, Users, LogOut, AlertTriangle, Crown, Globe, Lock, BookOpen, Info } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Switch } from '../../ui/switch';
import { Label } from '../../ui/label';
import { Input } from '../../ui/input';
import { Badge } from '../../ui/badge';
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
import {
  useUserRole,
  useUserPermissions,
  getRoleLabel,
  getRoleColor,
} from '../../../data/userRoles';
import { ClubDetailResponse, activateClub, closeClub } from '../../../../api/club-full';
import { getMembers } from '../../../../api/member';
import { getMyInfo } from '../../../../api/user';

interface GroupContextType {
  club: ClubDetailResponse | null;
  loading: boolean;
}

export function AdminView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const { club, loading: clubLoading } = useOutletContext<GroupContextType>();
  
  // 모임별 역할 가져오기
  const { userRole, allRoles } = useUserRole(groupId || '1');
  const permissions = useUserPermissions(groupId || '1');
  
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [deleteConfirmText, setDeleteConfirmText] = useState('');
  const [pendingCount, setPendingCount] = useState(0);
  const [loadingMembers, setLoadingMembers] = useState(false);
  const [isActivating, setIsActivating] = useState(false);
  const [displayRole, setDisplayRole] = useState<string>('');
  const [displayRoleColor, setDisplayRoleColor] = useState<string>('');

  useEffect(() => {
    async function fetchPendingCount() {
      if (!groupId) return;
      try {
        setLoadingMembers(true);
        const pendingMembers = await getMembers(Number(groupId), 'PENDING');
        setPendingCount(pendingMembers.length);
      } catch (error) {
        console.error('대기 멤버 수 조회 실패:', error);
      } finally {
        setLoadingMembers(false);
      }
    }
    fetchPendingCount();
  }, [groupId]);

  // 실제 역할 정보 가져오기 (운영진 이상만 표시)
  useEffect(() => {
    async function fetchUserRole() {
      if (!groupId) return;
      try {
        const [myInfo, members] = await Promise.all([
          getMyInfo(),
          getMembers(Number(groupId), 'ACTIVE')
        ]);
        const currentMember = members.find(m => m.userId === myInfo.userId);
        if (currentMember) {
          const roles = currentMember.roles || [];
          // 역할 우선순위: OWNER > ACCOUNTANT > STAFF > MEMBER
          if (roles.includes('OWNER')) {
            setDisplayRole('모임장');
            setDisplayRoleColor('bg-orange-500 text-white');
          } else if (roles.includes('ACCOUNTANT')) {
            setDisplayRole('총무');
            setDisplayRoleColor('bg-green-500 text-white');
          } else if (roles.includes('STAFF')) {
            setDisplayRole('운영진');
            setDisplayRoleColor('bg-blue-500 text-white');
          } else {
            setDisplayRole('회원');
            setDisplayRoleColor('bg-stone-500 text-white');
          }
        }
      } catch (error) {
        console.error('사용자 역할 조회 실패:', error);
      }
    }
    fetchUserRole();
  }, [groupId]);

  const groupName = club?.clubName || '모임';
  const currentVisibility = club?.visibility === 'PUBLIC' ? 'public' : 
                           club?.visibility === 'PRIVATE' ? 'private' : 'searchable';
  
  // 모임이 닫혔는지 확인 (status가 INACTIVE이거나 closedAt이 null이 아닌 경우)
  const isClosed = club?.status === 'INACTIVE' || club?.closedAt !== null;
  const isOwner = userRole === 'owner';

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
      toast.success('모임이 폐쇄되었습니다');
      setShowDeleteDialog(false);
      setTimeout(() => navigate('/'), 500);
    } catch (error) {
      console.error('모임 폐쇄 실패:', error);
      toast.error('모임 폐쇄에 실패했습니다.');
    }
  };

  // 복합 역할 표시
  const isMultiRole = allRoles.length > 1;
  const multiRoleLabel = getRoleLabel(groupId || '1');
  const multiRoleColor = getRoleColor(groupId || '1');

  return (
    <div className="space-y-6 pb-20" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* 역할 표시 */}
      {displayRole && displayRole !== '회원' && (
        <div className="flex justify-end">
          <Badge className={`${displayRoleColor} text-xs`}>
            {displayRole}
          </Badge>
        </div>
      )}

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

      {/* 통장 관리 - 총무/모임장만 */}
      {permissions.canWithdraw && (
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
        </div>
      )}

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

      {/* 권한 없음 안내 - 일반 회원 */}
      {userRole === 'member' && (
        <div className="bg-stone-100 rounded-xl p-6 text-center">
          <Lock className="w-10 h-10 text-stone-400 mx-auto mb-3" />
          <p className="font-medium text-stone-700 mb-1">관리 권한이 없습니다</p>
          <p className="text-sm text-stone-500">
            모임장, 총무, 운영진만 관리 기능을 사용할 수 있습니다.
          </p>
        </div>
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
    </div>
  );
}
