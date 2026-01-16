import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Shield, Crown, Wallet, Users, UserCircle, ChevronRight, Search, Check, Info } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Avatar, AvatarFallback, AvatarImage } from '../../ui/avatar';
import { Badge } from '../../ui/badge';
import { Card, CardContent } from '../../ui/card';
import { Checkbox } from '../../ui/checkbox';
import { Label } from '../../ui/label';
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

// 역할 타입 (중복 가능)
type RoleType = 'treasurer' | 'manager';

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

export function RoleManagementView() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);
  const [showRoleDialog, setShowRoleDialog] = useState(false);
  const [showTransferDialog, setShowTransferDialog] = useState(false);
  const [selectedRoles, setSelectedRoles] = useState<RoleType[]>([]);

  // Mock data - 권한 중복 가능
  const [members, setMembers] = useState<Member[]>([
    { id: '1', name: '홍길동', avatar: '', isOwner: true, roles: ['treasurer'], joinedDate: '2024.01.15' },
    { id: '2', name: '김철수', avatar: '', isOwner: false, roles: ['treasurer'], joinedDate: '2024.01.20' },
    { id: '3', name: '이영희', avatar: '', isOwner: false, roles: ['manager'], joinedDate: '2024.02.01' },
    { id: '4', name: '박민수', avatar: '', isOwner: false, roles: ['treasurer', 'manager'], joinedDate: '2024.02.15' }, // 중복 권한
    { id: '5', name: '정지훈', avatar: '', isOwner: false, roles: [], joinedDate: '2024.03.01' },
    { id: '6', name: '최유진', avatar: '', isOwner: false, roles: [], joinedDate: '2024.03.10' },
  ]);

  const roleIcons: Record<RoleType | 'owner' | 'member', React.ReactNode> = {
    owner: <Crown className="w-4 h-4" />,
    treasurer: <Wallet className="w-4 h-4" />,
    manager: <Shield className="w-4 h-4" />,
    member: <UserCircle className="w-4 h-4" />,
  };

  const roleDescriptions: Record<RoleType, string> = {
    treasurer: '회비 채우기/보내기, 정산 관리, 지분 확인',
    manager: '멤버 관리, 게시글 삭제, 일정 마무리',
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
    setSelectedMember(member);
    setSelectedRoles([...member.roles]);
    setShowRoleDialog(true);
  };

  const handleRoleToggle = (role: RoleType) => {
    setSelectedRoles(prev => 
      prev.includes(role) 
        ? prev.filter(r => r !== role)
        : [...prev, role]
    );
  };

  const handleSaveRoles = () => {
    if (!selectedMember) return;

    setMembers(members.map(m => 
      m.id === selectedMember.id ? { ...m, roles: selectedRoles } : m
    ));
    setShowRoleDialog(false);
    
    const roleNames = selectedRoles.length > 0 
      ? selectedRoles.map(r => ROLE_LABELS[r]).join(', ')
      : '일반 회원';
    toast.success(`${selectedMember.name}님의 권한이 ${roleNames}(으)로 변경되었습니다`);
  };

  const handleTransferOwnership = () => {
    if (!selectedMember) return;

    setMembers(members.map(m => {
      if (m.id === selectedMember.id) return { ...m, isOwner: true };
      if (m.isOwner) return { ...m, isOwner: false };
      return m;
    }));
    setShowTransferDialog(false);
    toast.success(`${selectedMember.name}님에게 모임장 권한이 위임되었습니다`);
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
        <Card className="border-blue-200 bg-blue-50">
          <CardContent className="p-4">
            <div className="flex items-start gap-3">
              <Info className="w-5 h-5 text-blue-600 mt-0.5" />
              <div>
                <p className="text-sm font-medium text-blue-800">권한 중복 가능</p>
                <p className="text-xs text-blue-700 mt-1">
                  한 멤버가 총무와 운영진 권한을 동시에 가질 수 있습니다.
                  모임장은 모든 권한을 자동으로 보유합니다.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

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
                className={`w-full p-4 flex items-center gap-3 text-left transition-colors ${
                  member.isOwner ? 'bg-orange-50' : 'hover:bg-stone-50'
                }`}
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
                <ChevronRight className="w-5 h-5 text-stone-300 shrink-0" />
              </button>
            ))}
          </div>
        </div>

        {/* Transfer Ownership Button */}
        <Button
          variant="outline"
          onClick={() => {
            setSelectedMember(null);
            // 모임장이 아닌 멤버 선택 다이얼로그 표시
            setShowTransferDialog(true);
          }}
          className="w-full h-12 border-orange-200 text-orange-600 hover:bg-orange-50"
        >
          <Crown className="w-5 h-5 mr-2" />
          모임장 위임하기
        </Button>
      </div>

      {/* Role Change Dialog */}
      <Dialog open={showRoleDialog} onOpenChange={setShowRoleDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>권한 변경</DialogTitle>
            <DialogDescription>
              {selectedMember?.name}님에게 부여할 권한을 선택하세요 (중복 가능)
            </DialogDescription>
          </DialogHeader>

          <div className="py-4 space-y-4">
            {/* 모임장인 경우 */}
            {selectedMember?.isOwner && (
              <Card className="border-orange-200 bg-orange-50">
                <CardContent className="p-3">
                  <div className="flex items-center gap-2 text-orange-700">
                    <Crown className="w-4 h-4" />
                    <span className="text-sm font-medium">모임장은 모든 권한을 자동으로 보유합니다</span>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* 추가 권한 선택 */}
            <div className="space-y-3">
              <p className="text-sm font-medium text-stone-700">추가 권한 부여</p>
              
              {/* 총무 */}
              <div 
                onClick={() => handleRoleToggle('treasurer')}
                className={`flex items-center gap-3 p-4 rounded-xl border-2 cursor-pointer transition-colors ${
                  selectedRoles.includes('treasurer')
                    ? 'border-green-500 bg-green-50'
                    : 'border-stone-100 hover:border-stone-200'
                }`}
              >
                <Checkbox
                  checked={selectedRoles.includes('treasurer')}
                  className="data-[state=checked]:bg-green-500 data-[state=checked]:border-green-500"
                />
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <Wallet className="w-4 h-4 text-green-600" />
                    <span className="font-medium text-stone-900">총무</span>
                  </div>
                  <p className="text-xs text-stone-500 mt-1">{roleDescriptions.treasurer}</p>
                </div>
              </div>

              {/* 운영진 */}
              <div 
                onClick={() => handleRoleToggle('manager')}
                className={`flex items-center gap-3 p-4 rounded-xl border-2 cursor-pointer transition-colors ${
                  selectedRoles.includes('manager')
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-stone-100 hover:border-stone-200'
                }`}
              >
                <Checkbox
                  checked={selectedRoles.includes('manager')}
                  className="data-[state=checked]:bg-blue-500 data-[state=checked]:border-blue-500"
                />
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <Shield className="w-4 h-4 text-blue-600" />
                    <span className="font-medium text-stone-900">운영진</span>
                  </div>
                  <p className="text-xs text-stone-500 mt-1">{roleDescriptions.manager}</p>
                </div>
              </div>
            </div>

            {/* 모임장 위임 */}
            {!selectedMember?.isOwner && (
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
            )}
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
