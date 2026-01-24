import { useState, useEffect, type ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Shield, Crown, Wallet, UserCircle, ChevronRight, Search, Info } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Avatar, AvatarFallback, AvatarImage } from '../../ui/avatar';
import { Badge } from '../../ui/badge';
import { Card, CardContent } from '../../ui/card';
import { RadioGroup, RadioGroupItem } from '../../ui/radio-group';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../ui/dialog';
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
import { getMembers, MemberListResponse, updateMemberRole } from '@/api/member';
import { transferOwnership } from '@/api/club-full';
import { getMyInfo } from '@/api/user';

// 역할 타입 (단일 선택)
type RoleType = 'treasurer' | 'manager' | 'member';

interface Member {
  id: string;
  name: string;
  avatar?: string;
  isOwner: boolean;
  roles: RoleType[];
  joinedDate: string;
}

const ROLE_LABELS: Record<RoleType | 'owner' | 'member', string> = {
  owner: '모임장',
  treasurer: '총무',
  manager: '운영진',
  member: '회원',
};

const ROLE_COLORS: Record<RoleType | 'owner' | 'member', string> = {
  owner: 'bg-orange-100 text-orange-700',
  treasurer: 'bg-green-100 text-green-700',
  manager: 'bg-blue-100 text-blue-700',
  member: 'bg-stone-100 text-stone-600',
};

// API 응답을 UI Member 형식으로 변환
function mapApiToMember(apiMember: MemberListResponse): Member {
  const roles: RoleType[] = [];
  if (apiMember.roles.includes('ACCOUNTANT')) roles.push('treasurer');
  if (apiMember.roles.includes('STAFF')) roles.push('manager');

  const isOwner = apiMember.roles.includes('OWNER');
  
  // 멤버 관리 탭에서는 모든 멤버를 "실명(닉네임)" 형식으로 표시
  let displayName: string;
  if (apiMember.realName && apiMember.clubNickname) {
    displayName = `${apiMember.realName}(${apiMember.clubNickname})`;
  } else if (apiMember.realName) {
    displayName = apiMember.realName;
  } else if (apiMember.clubNickname) {
    displayName = apiMember.clubNickname;
  } else {
    displayName = '회원';
  }

  return {
    id: apiMember.memberId.toString(),
    name: displayName,
    isOwner,
    roles,
    joinedDate: new Date(apiMember.joinedAt).toLocaleDateString('ko-KR'),
  };
}

export function RoleManagementView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const clubId = groupId ? parseInt(groupId) : 0;
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);
  const [showRoleDialog, setShowRoleDialog] = useState(false);
  const [showTransferDialog, setShowTransferDialog] = useState(false);
  const [selectedRole, setSelectedRole] = useState<RoleType | 'member'>('member');
  const [loading, setLoading] = useState(true);
  const [members, setMembers] = useState<Member[]>([]);
  const [isOwner, setIsOwner] = useState(false);

  // API로 멤버 목록 불러오기
  const fetchMembers = async (): Promise<boolean> => {
    if (!clubId) return false;
    
    try {
      setLoading(true);
      const activeMembers = await getMembers(clubId, 'ACTIVE');
      const mappedMembers = activeMembers.map(mapApiToMember);
      setMembers(mappedMembers);
      
      // 현재 사용자 정보 확인
      try {
        const myInfo = await getMyInfo();
        // 현재 사용자가 모임장인지 확인 (userId로 정확히 매칭)
        const currentApiMember = activeMembers.find(am => am.userId === myInfo.userId);
        if (currentApiMember) {
          const currentMember = mappedMembers.find(m => m.id === currentApiMember.memberId.toString());
          const isCurrentUserOwner = currentMember?.isOwner || false;
          setIsOwner(isCurrentUserOwner);
          return isCurrentUserOwner;
        } else {
          setIsOwner(false);
          return false;
        }
      } catch (error) {
        console.error('사용자 정보 조회 실패:', error);
        setIsOwner(false);
        return false;
      }
    } catch (error) {
      console.error('멤버 목록 조회 실패:', error);
      toast.error('멤버 목록을 불러오는데 실패했습니다.');
      return false;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMembers();
  }, [clubId]);

  const roleIcons: Record<RoleType | 'owner' | 'member', ReactNode> = {
    owner: <Crown className="w-4 h-4" />,
    treasurer: <Wallet className="w-4 h-4" />,
    manager: <Shield className="w-4 h-4" />,
    member: <UserCircle className="w-4 h-4" />,
  };

  const roleDescriptions: Record<RoleType, string> = {
    treasurer: '회비 채우기/보내기, 정산 관리, 지분 확인',
    manager: '멤버 관리, 게시글 삭제, 일정 마무리',
    member: '일반 멤버',
  };

  const filteredMembers = members.filter(m => 
    m.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // 정렬: 모임장 > 권한 있는 멤버 > 일반 멤버
  const sortedMembers = [...filteredMembers].sort((a, b) => {
    if (a.isOwner) return -1;
    if (b.isOwner) return 1;
    if (a.roles.length !== b.roles.length) return b.roles.length - a.roles.length;
    return 0;
  });

  const handleMemberClick = (member: Member) => {
    // 모임장만 권한 관리 가능
    if (!isOwner) return;
    
    // 모임장 본인은 클릭 불가
    if (member.isOwner) return;
    
    setSelectedMember(member);
    // 현재 멤버의 역할을 단일 값으로 설정 (가장 높은 권한)
    if (member.roles.includes('treasurer')) {
      setSelectedRole('treasurer');
    } else if (member.roles.includes('manager')) {
      setSelectedRole('manager');
    } else {
      setSelectedRole('member');
    }
    setShowRoleDialog(true);
  };

  const handleSaveRoles = async () => {
    if (!selectedMember) return;

    // 단일 역할만 지원
    let newRole = 'MEMBER';
    if (selectedRole === 'treasurer') {
      newRole = 'ACCOUNTANT';
    } else if (selectedRole === 'manager') {
      newRole = 'STAFF';
    }

    try {
      await updateMemberRole(clubId, parseInt(selectedMember.id), newRole);
      const roleName = selectedRole === 'member' ? '일반 회원' : ROLE_LABELS[selectedRole];
      toast.success(`${selectedMember.name}님의 권한이 ${roleName}(으)로 변경되었습니다`);
      setShowRoleDialog(false);
      fetchMembers(); // 목록 새로고침
    } catch (error) {
      console.error('권한 변경 실패:', error);
      toast.error('권한 변경에 실패했습니다.');
    }
  };

  const handleTransferOwnership = async () => {
    if (!selectedMember || !groupId) return;

    try {
      await transferOwnership(Number(groupId), {
        newOwnerMemberId: parseInt(selectedMember.id)
      });
      toast.success(`${selectedMember.name}님에게 모임장을 위임했습니다`);
      setShowTransferDialog(false);
      setSelectedMember(null);
      // 멤버 목록 다시 불러오기 (현재 사용자 정보도 업데이트됨)
      const isStillOwner = await fetchMembers();
      // 모임장 위임 후 더 이상 모임장이 아니므로 관리 페이지로 리다이렉트
      if (!isStillOwner) {
        navigate(`/group/${groupId}`);
      }
    } catch (error) {
      console.error('모임장 위임 실패:', error);
      toast.error('모임장 위임에 실패했습니다');
    }
  };

  const getDisplayRoles = (member: Member) => {
    const roles = [];
    if (member.isOwner) roles.push('owner');
    roles.push(...member.roles);
    if (roles.length === 0 || (roles.length === 1 && roles[0] === 'owner' === false)) {
      if (!member.isOwner && member.roles.length === 0) roles.push('member');
    }
    return roles as (RoleType | 'owner' | 'member')[];
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
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
          <h1 className="ml-2 text-lg font-semibold text-stone-800">권한 관리</h1>
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Info */}
        {isOwner && (
          <Card className="border-blue-200 bg-blue-50">
            <CardContent className="p-4">
              <div className="flex items-start gap-3">
                <Info className="w-5 h-5 text-blue-600 mt-0.5" />
                <div>
                  <p className="text-sm font-medium text-blue-800">모임장 위임</p>
                  <p className="text-xs text-blue-700 mt-1">
                    모임장을 다른 멤버에게 위임할 수 있습니다. 위임 후에는 일반 회원이 되며, 모든 관리 권한을 잃게 됩니다.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Role Legend */}
        <div className="bg-white rounded-2xl p-4 border border-stone-100 space-y-3">
          <h3 className="font-bold text-stone-900">역할별 권한</h3>
          <div className="space-y-3">
            <div className="flex items-start gap-3 p-3 rounded-lg bg-orange-50">
              <Badge className={ROLE_COLORS.owner}>
                {roleIcons.owner}
                <span className="ml-1">{ROLE_LABELS.owner}</span>
              </Badge>
              <p className="text-xs text-stone-600 flex-1">모든 권한 (자동 부여)</p>
            </div>
            <div className="flex items-start gap-3 p-3 rounded-lg bg-stone-50">
              <Badge className={ROLE_COLORS.treasurer}>
                {roleIcons.treasurer}
                <span className="ml-1">{ROLE_LABELS.treasurer}</span>
              </Badge>
              <p className="text-xs text-stone-600 flex-1">{roleDescriptions.treasurer}</p>
            </div>
            <div className="flex items-start gap-3 p-3 rounded-lg bg-stone-50">
              <Badge className={ROLE_COLORS.manager}>
                {roleIcons.manager}
                <span className="ml-1">{ROLE_LABELS.manager}</span>
              </Badge>
              <p className="text-xs text-stone-600 flex-1">{roleDescriptions.manager}</p>
            </div>
          </div>
          <p className="text-xs text-stone-500 pt-2 border-t border-stone-100">
            💡 권한은 계층 구조로 관리되며, 한 멤버는 하나의 권한만 가질 수 있습니다.
          </p>
        </div>

        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
          <Input
            placeholder="멤버 검색"
            className="pl-10 h-11 bg-white border-stone-200 rounded-xl"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Member List */}
        <div className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
          <div className="px-4 py-3 bg-stone-50 border-b border-stone-100">
            <span className="text-sm font-medium text-stone-500">
              멤버 {sortedMembers.length}명
            </span>
          </div>
          <div className="divide-y divide-stone-100">
            {sortedMembers.map(member => (
              <button
                key={member.id}
                onClick={() => handleMemberClick(member)}
                disabled={!isOwner || member.isOwner}
                className={`w-full p-4 flex items-center gap-3 text-left transition-colors ${
                  member.isOwner ? 'bg-orange-50' : isOwner ? 'hover:bg-stone-50 cursor-pointer' : 'cursor-default'
                } ${!isOwner || member.isOwner ? 'opacity-60' : ''}`}
              >
                <Avatar className="w-12 h-12">
                  <AvatarImage src={member.avatar} />
                  <AvatarFallback>{member.name[0]}</AvatarFallback>
                </Avatar>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-medium text-stone-900">{member.name}</p>
                    {member.isOwner && (
                      <Crown className="w-4 h-4 text-orange-500" />
                    )}
                  </div>
                  <div className="flex flex-wrap gap-1 mt-1">
                    {getDisplayRoles(member).map(role => (
                      <Badge key={role} className={`text-xs ${ROLE_COLORS[role]}`}>
                        {roleIcons[role]}
                        <span className="ml-1">{ROLE_LABELS[role]}</span>
                      </Badge>
                    ))}
                  </div>
                </div>
                {isOwner && !member.isOwner && (
                  <ChevronRight className="w-5 h-5 text-stone-300 shrink-0" />
                )}
                {member.isOwner && (
                  <span className="text-xs text-stone-400">모임장</span>
                )}
              </button>
            ))}
          </div>
        </div>

      </div>

      {/* Role Change Dialog */}
      <Dialog open={showRoleDialog} onOpenChange={setShowRoleDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>권한 변경</DialogTitle>
            <DialogDescription>
              {selectedMember?.name}님에게 부여할 권한을 선택하세요
            </DialogDescription>
          </DialogHeader>

          <div className="py-4 space-y-4">
            {/* 권한 선택 (단일 선택) */}
            <RadioGroup value={selectedRole} onValueChange={(v) => setSelectedRole(v as RoleType | 'member')}>
              <div className="space-y-3">
                {/* 일반 회원 */}
                <label
                  className={`flex items-start gap-3 p-4 rounded-xl border-2 cursor-pointer transition-colors ${
                    selectedRole === 'member'
                      ? 'border-stone-500 bg-stone-50'
                      : 'border-stone-100 hover:border-stone-200'
                  }`}
                >
                  <RadioGroupItem 
                    value="member" 
                    className="mt-1 data-[state=checked]:border-stone-500 data-[state=checked]:bg-stone-500" 
                  />
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <UserCircle className="w-4 h-4 text-stone-600" />
                      <span className="font-medium text-stone-900">일반 회원</span>
                    </div>
                    <p className="text-xs text-stone-500 mt-1">기본 권한</p>
                  </div>
                </label>

                {/* 총무 */}
                <label
                  className={`flex items-start gap-3 p-4 rounded-xl border-2 cursor-pointer transition-colors ${
                    selectedRole === 'treasurer'
                      ? 'border-green-500 bg-green-50'
                      : 'border-stone-100 hover:border-stone-200'
                  }`}
                >
                  <RadioGroupItem 
                    value="treasurer" 
                    className="mt-1 data-[state=checked]:border-green-500 data-[state=checked]:bg-green-500" 
                  />
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <Wallet className="w-4 h-4 text-green-600" />
                      <span className="font-medium text-stone-900">총무</span>
                    </div>
                    <p className="text-xs text-stone-500 mt-1">{roleDescriptions.treasurer}</p>
                  </div>
                </label>

                {/* 운영진 */}
                <label
                  className={`flex items-start gap-3 p-4 rounded-xl border-2 cursor-pointer transition-colors ${
                    selectedRole === 'manager'
                      ? 'border-blue-500 bg-blue-50'
                      : 'border-stone-100 hover:border-stone-200'
                  }`}
                >
                  <RadioGroupItem 
                    value="manager" 
                    className="mt-1 data-[state=checked]:border-blue-500 data-[state=checked]:bg-blue-500" 
                  />
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <Shield className="w-4 h-4 text-blue-600" />
                      <span className="font-medium text-stone-900">운영진</span>
                    </div>
                    <p className="text-xs text-stone-500 mt-1">{roleDescriptions.manager}</p>
                  </div>
                </label>
              </div>
            </RadioGroup>

            {/* 모임장 위임 */}
            <div className="pt-2 border-t border-stone-100">
              <button
                onClick={() => {
                  setShowRoleDialog(false);
                  setShowTransferDialog(true);
                }}
                className="w-full flex items-center gap-3 p-4 rounded-xl border-2 border-dashed border-orange-200 hover:bg-orange-50 transition-colors"
              >
                <Crown className="w-5 h-5 text-orange-600" />
                <div className="flex-1 text-left">
                  <p className="font-medium text-orange-600">모임장 위임</p>
                  <p className="text-xs text-orange-500">이 멤버에게 모임장을 위임합니다</p>
                </div>
              </button>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setShowRoleDialog(false)}>
              취소
            </Button>
            <Button
              onClick={handleSaveRoles}
              className="bg-orange-500 hover:bg-orange-600"
            >
              저장하기
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Transfer Ownership Confirmation */}
      <AlertDialog open={showTransferDialog} onOpenChange={setShowTransferDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>모임장 위임</AlertDialogTitle>
            <AlertDialogDescription>
              {selectedMember ? (
                <>
                  정말 <span className="font-bold text-stone-900">{selectedMember.name}</span>님에게 
                  모임장을 위임하시겠습니까?
                </>
              ) : (
                '모임장을 위임할 멤버를 선택하세요.'
              )}
              <br /><br />
              위임 후에는 일반 회원이 되며, 모든 관리 권한을 잃게 됩니다.
              이 작업은 취소할 수 없습니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            {selectedMember && (
              <AlertDialogAction
                onClick={handleTransferOwnership}
                className="bg-orange-500 hover:bg-orange-600"
              >
                위임하기
              </AlertDialogAction>
            )}
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
