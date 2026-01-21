import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Search, MoreVertical, UserCheck, UserX, Crown, Shield, Flag, Wallet, Ban, PauseCircle, PlayCircle, Clock } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Avatar, AvatarImage, AvatarFallback } from '../../ui/avatar';
import { Label } from '../../ui/label';
import { Textarea } from '../../ui/textarea';
import { RadioGroup, RadioGroupItem } from '../../ui/radio-group';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../ui/dropdown-menu';
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
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../ui/dialog';
import { ReportDialog } from '../../report/ReportDialog';
import { useUserPermissions } from '../../../data/userRoles';
import { getMembers, MemberListResponse, updateMemberRole, approveMember, rejectMember, kickMember } from '@/api/member';

type SuspendDuration = '1day' | '3days' | '7days' | '30days' | 'permanent';

interface Member {
  id: string;
  name: string;
  avatar?: string;
  role: 'owner' | 'treasurer' | 'manager' | 'member' | 'pending';
  joinedDate: string;
  status: 'active' | 'suspended' | 'banned';
  suspendedUntil?: string;
  suspendReason?: string;
}

// API 응답을 UI Member 형식으로 변환
function mapApiToMember(apiMember: MemberListResponse): Member {
  // roles 배열에서 가장 높은 권한 찾기
  const roleMap: Record<string, 'owner' | 'treasurer' | 'manager' | 'member'> = {
    'OWNER': 'owner',
    'ACCOUNTANT': 'treasurer',
    'STAFF': 'manager',
    'MEMBER': 'member',
  };
  
  let role: Member['role'] = 'member';
  if (apiMember.status === 'PENDING') {
    role = 'pending';
  } else if (apiMember.roles.includes('OWNER')) {
    role = 'owner';
  } else if (apiMember.roles.includes('ACCOUNTANT')) {
    role = 'treasurer';
  } else if (apiMember.roles.includes('STAFF')) {
    role = 'manager';
  }

  return {
    id: apiMember.memberId.toString(),
    name: apiMember.realName || apiMember.clubNickname || '회원',
    role,
    joinedDate: new Date(apiMember.joinedAt).toLocaleDateString('ko-KR'),
    status: apiMember.status === 'ACTIVE' ? 'active' : 
            apiMember.status === 'SUSPENDED' ? 'suspended' : 
            apiMember.status === 'BANNED' ? 'banned' : 'active',
    userId: apiMember.userId, // 신고를 위한 사용자 ID
  };
}

export function MemberManagementView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const clubId = groupId ? parseInt(groupId) : 0;
  const permissions = useUserPermissions(groupId || '1');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);
  const [showKickDialog, setShowKickDialog] = useState(false);
  const [showRoleDialog, setShowRoleDialog] = useState(false);
  const [showReportDialog, setShowReportDialog] = useState(false);
  const [showSuspendDialog, setShowSuspendDialog] = useState(false);
  const [newRole, setNewRole] = useState<'treasurer' | 'manager' | 'member'>('member');
  const [suspendDuration, setSuspendDuration] = useState<SuspendDuration>('7days');
  const [suspendReason, setSuspendReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [members, setMembers] = useState<Member[]>([]);

  // API로 멤버 목록 불러오기
  const fetchMembers = async () => {
    if (!clubId) return;
    
    try {
      setLoading(true);
      // 활성 멤버와 대기 멤버 모두 조회
      const [activeMembers, pendingMembers] = await Promise.all([
        getMembers(clubId, 'ACTIVE').catch((err) => {
          console.error('활성 멤버 조회 실패:', err);
          return [] as MemberListResponse[];
        }),
        getMembers(clubId, 'PENDING').catch((err) => {
          console.error('대기 멤버 조회 실패:', err);
          return [] as MemberListResponse[];
        })
      ]);
      
      const allMembers = [...activeMembers, ...pendingMembers].map(mapApiToMember);
      setMembers(allMembers);
    } catch (error) {
      // 예상치 못한 에러만 표시
      console.error('멤버 목록 조회 실패:', error);
      // 실제 네트워크 에러나 서버 에러인 경우에만 토스트 표시
      if (error instanceof Error && !error.message.includes('404')) {
        toast.error('멤버 목록을 불러오는데 실패했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMembers();
  }, [clubId]);

  const filteredMembers = members.filter(member =>
    member.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const durationLabels: Record<SuspendDuration, string> = {
    '1day': '1일',
    '3days': '3일',
    '7days': '7일',
    '30days': '30일',
    'permanent': '영구',
  };

  const handleApprove = async (memberId: string) => {
    const member = members.find(m => m.id === memberId);
    try {
      await approveMember(clubId, parseInt(memberId));
      toast.success(`${member?.name}님의 가입을 승인했습니다`);
      fetchMembers(); // 목록 새로고침
    } catch (error) {
      console.error('가입 승인 실패:', error);
      toast.error('가입 승인에 실패했습니다.');
    }
  };

  const handleReject = async (memberId: string) => {
    const member = members.find(m => m.id === memberId);
    try {
      await rejectMember(clubId, parseInt(memberId));
      toast.info(`${member?.name}님의 가입을 거절했습니다`);
      fetchMembers(); // 목록 새로고침
    } catch (error) {
      console.error('가입 거절 실패:', error);
      toast.error('가입 거절에 실패했습니다.');
    }
  };

  const handleKickMember = async () => {
    if (selectedMember) {
      try {
        await kickMember(clubId, parseInt(selectedMember.id));
        toast.success(`${selectedMember.name}님을 추방했습니다`);
        setShowKickDialog(false);
        setSelectedMember(null);
        fetchMembers(); // 목록 새로고침
      } catch (error) {
        console.error('멤버 추방 실패:', error);
        toast.error('멤버 추방에 실패했습니다.');
      }
    }
  };

  const handleChangeRole = async () => {
    if (selectedMember) {
      const roleMap: Record<string, string> = {
        treasurer: 'ACCOUNTANT',
        manager: 'STAFF',
        member: 'MEMBER',
      };
      
      try {
        await updateMemberRole(clubId, parseInt(selectedMember.id), roleMap[newRole]);
        const roleLabels = { treasurer: '총무', manager: '운영진', member: '일반 회원' };
        toast.success(`${selectedMember.name}님의 권한이 ${roleLabels[newRole]}로 변경되었습니다`);
        setShowRoleDialog(false);
        setSelectedMember(null);
        fetchMembers(); // 목록 새로고침
      } catch (error) {
        console.error('권한 변경 실패:', error);
        toast.error('권한 변경에 실패했습니다.');
      }
    }
  };

  const handleSuspendMember = () => {
    if (!selectedMember || !suspendReason) {
      toast.error('정지 사유를 입력해주세요');
      return;
    }

    // TODO: 백엔드에 정지 API가 없어서 구현 필요
    toast.info('정지 기능은 준비 중입니다.');
    setShowSuspendDialog(false);
    setSelectedMember(null);
    setSuspendReason('');
    setSuspendDuration('7days');
  };

  const handleActivateMember = (member: Member) => {
    // TODO: 백엔드에 정지 해제 API가 없어서 구현 필요
    toast.info('정지 해제 기능은 준비 중입니다.');
  };

  const calculateSuspendDate = (duration: SuspendDuration): string => {
    const days = { '1day': 1, '3days': 3, '7days': 7, '30days': 30, 'permanent': 0 };
    const date = new Date();
    date.setDate(date.getDate() + days[duration]);
    return date.toISOString().split('T')[0];
  };

  const getRoleBadge = (role: string) => {
    switch (role) {
      case 'owner':
        return <Badge className="bg-orange-100 text-orange-700"><Crown className="w-3 h-3 mr-1" />모임장</Badge>;
      case 'treasurer':
        return <Badge className="bg-green-100 text-green-700"><Wallet className="w-3 h-3 mr-1" />총무</Badge>;
      case 'manager':
        return <Badge className="bg-blue-100 text-blue-700"><Shield className="w-3 h-3 mr-1" />운영진</Badge>;
      case 'pending':
        return <Badge variant="secondary" className="bg-yellow-100 text-yellow-700">대기중</Badge>;
      default:
        return <Badge variant="secondary" className="bg-stone-100 text-stone-600">회원</Badge>;
    }
  };

  const getStatusBadge = (status: string, suspendedUntil?: string) => {
    switch (status) {
      case 'suspended':
        return (
          <Badge className="bg-amber-100 text-amber-700">
            <PauseCircle className="w-3 h-3 mr-1" />
            정지 (~{suspendedUntil})
          </Badge>
        );
      case 'banned':
        return (
          <Badge className="bg-red-100 text-red-700">
            <Ban className="w-3 h-3 mr-1" />
            영구차단
          </Badge>
        );
      default:
        return null;
    }
  };


  const pendingMembers = filteredMembers.filter(m => m.role === 'pending');
  const activeMembers = filteredMembers.filter(m => m.role !== 'pending' && m.status === 'active');
  const restrictedMembers = filteredMembers.filter(m => m.status === 'suspended' || m.status === 'banned');

  // 정렬: owner > treasurer > manager > member
  const sortedActiveMembers = [...activeMembers].sort((a, b) => {
    const order = { owner: 0, treasurer: 1, manager: 2, member: 3 };
    return (order[a.role] || 4) - (order[b.role] || 4);
  });

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 pb-20" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100 backdrop-blur-sm bg-white/95">
        <div className="flex items-center justify-between px-4 py-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="font-bold text-lg text-stone-800">멤버 관리</h1>
          <div className="w-10" />
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
          <Input
            placeholder="멤버 이름 검색"
            className="pl-10 h-12 bg-white border-stone-200 rounded-xl"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Stats */}
        <div className="grid grid-cols-4 gap-2">
          <div className="bg-white rounded-xl p-3 text-center border border-stone-100">
            <p className="text-lg font-bold text-orange-600">{members.filter(m => m.role === 'owner').length}</p>
            <p className="text-xs text-stone-500">모임장</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center border border-stone-100">
            <p className="text-lg font-bold text-green-600">{members.filter(m => m.role === 'treasurer').length}</p>
            <p className="text-xs text-stone-500">총무</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center border border-stone-100">
            <p className="text-lg font-bold text-blue-600">{members.filter(m => m.role === 'manager').length}</p>
            <p className="text-xs text-stone-500">운영진</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center border border-amber-200 bg-amber-50">
            <p className="text-lg font-bold text-amber-600">{restrictedMembers.length}</p>
            <p className="text-xs text-stone-500">제재중</p>
          </div>
        </div>

        {/* Pending Members */}
        {pendingMembers.length > 0 && (
          <div className="space-y-4">
            <h2 className="font-bold text-lg text-stone-900">가입 대기 ({pendingMembers.length})</h2>
            <div className="bg-white rounded-xl border border-stone-200 divide-y divide-stone-100 overflow-hidden">
              {pendingMembers.map((member) => (
                <div key={member.id} className="p-4 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Avatar className="w-12 h-12" draggable={false}>
                      <AvatarImage src={member.avatar} draggable={false} />
                      <AvatarFallback>{member.name[0]}</AvatarFallback>
                    </Avatar>
                    <div>
                      <div className="flex items-center gap-2">
                        <p className="font-medium text-stone-900">{member.name}</p>
                        {getRoleBadge(member.role)}
                      </div>
                      <p className="text-xs text-stone-500">가입 신청일: {member.joinedDate}</p>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      onClick={() => handleApprove(member.id)}
                      className="bg-green-500 hover:bg-green-600 text-white h-8"
                    >
                      <UserCheck className="w-4 h-4 mr-1" />
                      승인
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => handleReject(member.id)}
                      className="h-8 border-stone-300"
                    >
                      <UserX className="w-4 h-4 mr-1" />
                      거절
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Restricted Members */}
        {restrictedMembers.length > 0 && (
          <div className="space-y-4">
            <h2 className="font-bold text-lg text-red-700">제재 중인 멤버 ({restrictedMembers.length})</h2>
            <div className="bg-white rounded-xl border border-red-200 divide-y divide-stone-100 overflow-hidden">
              {restrictedMembers.map((member) => (
                <div key={member.id} className={`p-4 ${member.status === 'banned' ? 'bg-red-50' : 'bg-amber-50'}`}>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <Avatar className="w-12 h-12 opacity-60">
                        <AvatarImage src={member.avatar} />
                        <AvatarFallback>{member.name[0]}</AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="flex items-center gap-2">
                          <p className="font-medium text-stone-700">{member.name}</p>
                          {getStatusBadge(member.status, member.suspendedUntil)}
                        </div>
                        <p className="text-xs text-stone-500">사유: {member.suspendReason}</p>
                        {member.suspendedUntil && (
                          <p className="text-xs text-amber-600 flex items-center gap-1 mt-1">
                            <Clock className="w-3 h-3" />
                            {member.suspendedUntil}까지 정지
                          </p>
                        )}
                      </div>
                    </div>
                    {permissions.canManageMembers && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleActivateMember(member)}
                        className="h-8 border-green-300 text-green-600 hover:bg-green-50"
                      >
                        <PlayCircle className="w-4 h-4 mr-1" />
                        해제
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Active Members */}
        <div className="space-y-4">
          <h2 className="font-bold text-lg text-stone-900">활성 멤버 ({sortedActiveMembers.length})</h2>
          <div className="bg-white rounded-xl border border-stone-200 divide-y divide-stone-100 overflow-hidden">
            {sortedActiveMembers.map((member) => (
              <div key={member.id} className="p-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Avatar className="w-12 h-12">
                    <AvatarImage src={member.avatar} />
                    <AvatarFallback>{member.name[0]}</AvatarFallback>
                  </Avatar>
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="font-medium text-stone-900">{member.name}</p>
                      {getRoleBadge(member.role)}
                    </div>
                    <p className="text-xs text-stone-500">가입일: {member.joinedDate}</p>
                  </div>
                </div>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon" className="h-8 w-8">
                      <MoreVertical className="w-5 h-5 text-stone-400" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-48">
                    {/* 권한 변경 - 모임장만 & owner는 변경 불가 */}
                    {permissions.canAssignRoles && member.role !== 'owner' && (
                      <>
                        <DropdownMenuItem
                          onClick={() => {
                            setSelectedMember(member);
                            setNewRole(member.role === 'member' ? 'manager' : 'member');
                            setShowRoleDialog(true);
                          }}
                        >
                          <Shield className="w-4 h-4 mr-2" />
                          권한 변경
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                      </>
                    )}
                    
                    {/* 정지/차단 - 운영진/모임장만 & owner는 불가 */}
                    {permissions.canManageMembers && member.role !== 'owner' && (
                      <>
                        <DropdownMenuItem
                          className="text-amber-600"
                          onClick={() => {
                            setSelectedMember(member);
                            setShowSuspendDialog(true);
                          }}
                        >
                          <PauseCircle className="w-4 h-4 mr-2" />
                          일시정지
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          className="text-red-600"
                          onClick={() => {
                            setSelectedMember(member);
                            setSuspendDuration('permanent');
                            setShowSuspendDialog(true);
                          }}
                        >
                          <Ban className="w-4 h-4 mr-2" />
                          영구차단
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                      </>
                    )}

                    {/* 추방 - 운영진/모임장만 & owner는 추방 불가 */}
                    {permissions.canManageMembers && member.role !== 'owner' && (
                      <>
                        <DropdownMenuItem
                          className="text-red-600"
                          onClick={() => {
                            setSelectedMember(member);
                            setShowKickDialog(true);
                          }}
                        >
                          <UserX className="w-4 h-4 mr-2" />
                          추방하기
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                      </>
                    )}
                    
                    {/* 신고 - 모두 가능 & owner는 신고 불가 */}
                    {member.role !== 'owner' && (
                      <DropdownMenuItem
                        className="text-orange-600"
                        onClick={() => {
                          setSelectedMember(member);
                          setShowReportDialog(true);
                        }}
                      >
                        <Flag className="w-4 h-4 mr-2" />
                        신고하기
                      </DropdownMenuItem>
                    )}
                    
                    {member.role === 'owner' && (
                      <DropdownMenuItem disabled className="text-stone-400">
                        모임장은 변경할 수 없습니다
                      </DropdownMenuItem>
                    )}
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Kick Member Dialog */}
      <AlertDialog open={showKickDialog} onOpenChange={setShowKickDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>멤버 추방</AlertDialogTitle>
            <AlertDialogDescription>
              {selectedMember?.name}님을 정말 추방하시겠습니까? 이 작업은 되돌릴 수 없습니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleKickMember}
              className="bg-red-500 hover:bg-red-600"
            >
              추방하기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Change Role Dialog */}
      <AlertDialog open={showRoleDialog} onOpenChange={setShowRoleDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>권한 변경</AlertDialogTitle>
            <AlertDialogDescription className="space-y-3">
              <p>{selectedMember?.name}님의 권한을 변경합니다.</p>
              <div className="flex flex-wrap gap-2">
                {(['treasurer', 'manager', 'member'] as const).map(role => (
                  <button
                    key={role}
                    onClick={() => setNewRole(role)}
                    className={`px-3 py-2 rounded-lg border transition-colors ${
                      newRole === role 
                        ? 'border-orange-500 bg-orange-50 text-orange-700' 
                        : 'border-stone-200 text-stone-600 hover:bg-stone-50'
                    }`}
                  >
                    {role === 'treasurer' && '총무'}
                    {role === 'manager' && '운영진'}
                    {role === 'member' && '일반 회원'}
                  </button>
                ))}
              </div>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleChangeRole}
              className="bg-orange-500 hover:bg-orange-600"
            >
              변경하기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Suspend/Ban Dialog */}
      <Dialog open={showSuspendDialog} onOpenChange={setShowSuspendDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {suspendDuration === 'permanent' ? (
                <>
                  <Ban className="w-5 h-5 text-red-500" />
                  멤버 영구차단
                </>
              ) : (
                <>
                  <PauseCircle className="w-5 h-5 text-amber-500" />
                  멤버 일시정지
                </>
              )}
            </DialogTitle>
            <DialogDescription>
              {selectedMember && `"${selectedMember.name}"님을 ${suspendDuration === 'permanent' ? '영구차단' : '정지'}합니다.`}
            </DialogDescription>
          </DialogHeader>
          <div className="py-4 space-y-4">
            <div className="space-y-3">
              <Label>정지 기간</Label>
              <RadioGroup value={suspendDuration} onValueChange={(v) => setSuspendDuration(v as SuspendDuration)}>
                <div className="grid grid-cols-3 gap-2">
                  {(['1day', '3days', '7days'] as SuspendDuration[]).map(d => (
                    <div key={d} className={`flex items-center space-x-2 border rounded-lg p-3 cursor-pointer ${suspendDuration === d ? 'border-amber-500 bg-amber-50' : 'border-stone-200'}`}>
                      <RadioGroupItem value={d} id={d} />
                      <Label htmlFor={d} className="cursor-pointer">{durationLabels[d]}</Label>
                    </div>
                  ))}
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div className={`flex items-center space-x-2 border rounded-lg p-3 cursor-pointer ${suspendDuration === '30days' ? 'border-amber-500 bg-amber-50' : 'border-stone-200'}`}>
                    <RadioGroupItem value="30days" id="30days" />
                    <Label htmlFor="30days" className="cursor-pointer">30일</Label>
                  </div>
                  <div className={`flex items-center space-x-2 border rounded-lg p-3 cursor-pointer ${suspendDuration === 'permanent' ? 'border-red-500 bg-red-50' : 'border-stone-200'}`}>
                    <RadioGroupItem value="permanent" id="permanent" />
                    <Label htmlFor="permanent" className="cursor-pointer text-red-600">영구차단</Label>
                  </div>
                </div>
              </RadioGroup>
            </div>
            <div className="space-y-2">
              <Label>정지/차단 사유</Label>
              <Textarea
                placeholder="정지 사유를 입력하세요 (예: 욕설, 규칙 위반 등)"
                value={suspendReason}
                onChange={(e) => setSuspendReason(e.target.value)}
                className="min-h-[80px]"
              />
            </div>
            {suspendDuration === 'permanent' && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                <p className="text-xs text-red-700">
                  ⚠️ 영구차단된 멤버는 이 모임에 다시 가입할 수 없습니다.
                </p>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => {
              setShowSuspendDialog(false);
              setSuspendDuration('7days');
            }}>
              취소
            </Button>
            <Button 
              onClick={handleSuspendMember}
              className={suspendDuration === 'permanent' ? 'bg-red-500 hover:bg-red-600' : 'bg-amber-500 hover:bg-amber-600'}
            >
              {suspendDuration === 'permanent' ? '영구차단' : '정지하기'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Report User Dialog */}
      <ReportDialog
        open={showReportDialog}
        onOpenChange={setShowReportDialog}
        type="user"
        targetId={selectedMember?.userId || (selectedMember ? parseInt(selectedMember.id) : 0)}
        targetName={selectedMember?.name}
      />
    </div>
  );
}
