import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import * as React from 'react';
import { 
  ArrowLeft, 
  Shield, 
  Flag, 
  Users, 
  UserX, 
  Ban, 
  Search,
  Eye,
  CheckCircle,
  XCircle,
  Clock,
  Trash2,
  Home,
  AlertTriangle,
  User,
  PauseCircle,
  PlayCircle,
  MoreVertical
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Card, CardContent } from '../ui/card';
import { Input } from '../ui/input';
import { Badge } from '../ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '../ui/avatar';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { RadioGroup, RadioGroupItem } from '../ui/radio-group';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '../ui/alert-dialog';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../ui/dropdown-menu';

interface Report {
  id: string;
  type: 'user' | 'group' | 'post';
  targetId: string;
  targetName: string;
  reporterName: string;
  reason: string;
  details?: string;
  status: 'pending' | 'reviewing' | 'resolved' | 'dismissed';
  createdAt: string;
}

interface ManagedUser {
  id: string;
  name: string;
  email: string;
  avatar?: string;
  status: 'active' | 'suspended' | 'banned';
  suspendedUntil?: string;
  joinedAt: string;
  groupCount: number;
  reportCount: number;
}

interface ManagedGroup {
  id: string;
  name: string;
  image: string;
  status: 'active' | 'suspended' | 'banned';
  suspendedUntil?: string;
  memberCount: number;
  ownerName: string;
  createdAt: string;
  reportCount: number;
}

type SuspendDuration = '1day' | '3days' | '7days' | '30days' | 'permanent';

export function SystemAdminView() {
  const navigate = useNavigate();
  
  // 모든 상태를 조건부 return 전에 선언 (React 훅 규칙)
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [selectedReport, setSelectedReport] = useState<Report | null>(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [showSuspendDialog, setShowSuspendDialog] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [suspendTarget, setSuspendTarget] = useState<{ type: 'user' | 'group'; id: string; name: string } | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<{ type: 'user' | 'group'; id: string; name: string } | null>(null);
  const [suspendDuration, setSuspendDuration] = useState<SuspendDuration>('7days');
  const [suspendReason, setSuspendReason] = useState('');

  // Mock 신고 데이터
  const [reports, setReports] = useState<Report[]>([
    {
      id: '1',
      type: 'user',
      targetId: 'user1',
      targetName: '악성유저123',
      reporterName: '홍길동',
      reason: '욕설/비방',
      details: '댓글에서 지속적으로 욕설을 사용합니다.',
      status: 'pending',
      createdAt: '2024-04-12 14:30',
    },
    {
      id: '2',
      type: 'group',
      targetId: 'group1',
      targetName: '수상한 투자 모임',
      reporterName: '김철수',
      reason: '사기/허위 정보',
      details: '가상화폐 투자 사기로 의심됩니다.',
      status: 'reviewing',
      createdAt: '2024-04-11 10:15',
    },
    {
      id: '3',
      type: 'post',
      targetId: 'post1',
      targetName: '광고 게시글',
      reporterName: '이영희',
      reason: '스팸/광고',
      status: 'resolved',
      createdAt: '2024-04-10 09:00',
    },
  ]);

  // Mock 사용자 목록
  const [users, setUsers] = useState<ManagedUser[]>([
    { id: '1', name: '홍길동', email: 'hong@test.com', status: 'active', joinedAt: '2024-01-15', groupCount: 3, reportCount: 0 },
    { id: '2', name: '김철수', email: 'kim@test.com', status: 'active', joinedAt: '2024-02-10', groupCount: 2, reportCount: 1 },
    { id: '3', name: '악성유저123', email: 'bad@test.com', status: 'suspended', suspendedUntil: '2024-04-20', joinedAt: '2024-03-01', groupCount: 1, reportCount: 5 },
    { id: '4', name: '스패머99', email: 'spam@test.com', status: 'banned', joinedAt: '2024-03-15', groupCount: 0, reportCount: 10 },
    { id: '5', name: '이영희', email: 'lee@test.com', status: 'active', joinedAt: '2024-03-20', groupCount: 4, reportCount: 0 },
  ]);

  // Mock 모임 목록
  const [groups, setGroups] = useState<ManagedGroup[]>([
    { id: '1', name: '주말 등산 클럽', image: 'https://images.unsplash.com/photo-1551632811-561732d1e306?w=200', status: 'active', memberCount: 15, ownerName: '홍길동', createdAt: '2024-01-15', reportCount: 0 },
    { id: '2', name: '강남 독서 모임', image: 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=200', status: 'active', memberCount: 8, ownerName: '김철수', createdAt: '2024-02-10', reportCount: 0 },
    { id: '3', name: '수상한 투자 모임', image: 'https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=200', status: 'suspended', suspendedUntil: '2024-04-25', memberCount: 20, ownerName: '악성유저123', createdAt: '2024-03-01', reportCount: 8 },
    { id: '4', name: '불법 도박 모임', image: 'https://images.unsplash.com/photo-1596838132731-3c2b5f1c5b92?w=200', status: 'banned', memberCount: 50, ownerName: '스패머99', createdAt: '2024-03-10', reportCount: 15 },
  ]);

  // 시스템 관리자 권한 체크 (모든 useState 다음에 useEffect)
  useEffect(() => {
    const adminStatus = localStorage.getItem('isSystemAdmin');
    if (adminStatus !== 'true') {
      toast.error('시스템 관리자 권한이 필요합니다');
      navigate('/login');
      return;
    }
    setIsAdmin(true);
    setIsLoading(false);
  }, [navigate]);

  // 로딩 중이거나 권한이 없으면 조기 반환
  if (isLoading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-center">
          <Shield className="w-12 h-12 text-red-500 mx-auto mb-4 animate-pulse" />
          <p className="text-stone-600">권한 확인 중...</p>
        </div>
      </div>
    );
  }

  if (!isAdmin) {
    return null;
  }

  const statusLabels: Record<string, string> = {
    pending: '대기 중',
    reviewing: '검토 중',
    resolved: '처리 완료',
    dismissed: '기각',
    active: '활성',
    suspended: '일시정지',
    banned: '영구정지',
  };

  const statusColors: Record<string, string> = {
    pending: 'bg-amber-100 text-amber-700',
    reviewing: 'bg-blue-100 text-blue-700',
    resolved: 'bg-green-100 text-green-700',
    dismissed: 'bg-stone-100 text-stone-600',
    active: 'bg-green-100 text-green-700',
    suspended: 'bg-amber-100 text-amber-700',
    banned: 'bg-red-100 text-red-700',
  };

  const typeLabels: Record<string, string> = {
    user: '사용자',
    group: '모임',
    post: '게시글',
  };

  const durationLabels: Record<SuspendDuration, string> = {
    '1day': '1일',
    '3days': '3일',
    '7days': '7일',
    '30days': '30일',
    'permanent': '영구',
  };

  const filteredReports = reports.filter(r => {
    const matchesSearch = r.targetName.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         r.reporterName.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = filterStatus === 'all' || r.status === filterStatus;
    return matchesSearch && matchesStatus;
  });

  const filteredUsers = users.filter(u => 
    u.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    u.email.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const filteredGroups = groups.filter(g => 
    g.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    g.ownerName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleUpdateStatus = (reportId: string, newStatus: Report['status']) => {
    setReports(prev => prev.map(r => 
      r.id === reportId ? { ...r, status: newStatus } : r
    ));
    toast.success('상태가 업데이트되었습니다');
    setSelectedReport(null);
  };

  const handleSuspend = () => {
    if (!suspendTarget || !suspendReason) {
      toast.error('정지 사유를 입력해주세요');
      return;
    }

    if (suspendTarget.type === 'user') {
      setUsers(prev => prev.map(u => 
        u.id === suspendTarget.id 
          ? { ...u, status: suspendDuration === 'permanent' ? 'banned' : 'suspended', suspendedUntil: suspendDuration === 'permanent' ? undefined : '계산된 날짜' } 
          : u
      ));
    } else {
      setGroups(prev => prev.map(g => 
        g.id === suspendTarget.id 
          ? { ...g, status: suspendDuration === 'permanent' ? 'banned' : 'suspended', suspendedUntil: suspendDuration === 'permanent' ? undefined : '계산된 날짜' } 
          : g
      ));
    }

    const action = suspendDuration === 'permanent' ? '영구정지' : `${durationLabels[suspendDuration]} 정지`;
    toast.success(`${suspendTarget.name}이(가) ${action} 처리되었습니다`);
    setShowSuspendDialog(false);
    setSuspendTarget(null);
    setSuspendReason('');
    setSuspendDuration('7days');
  };

  const handleDelete = () => {
    if (!deleteTarget) return;

    if (deleteTarget.type === 'user') {
      setUsers(prev => prev.filter(u => u.id !== deleteTarget.id));
    } else {
      setGroups(prev => prev.filter(g => g.id !== deleteTarget.id));
    }

    toast.success(`${deleteTarget.name}이(가) 삭제되었습니다`);
    setShowDeleteDialog(false);
    setDeleteTarget(null);
  };

  const handleActivate = (type: 'user' | 'group', id: string, name: string) => {
    if (type === 'user') {
      setUsers(prev => prev.map(u => u.id === id ? { ...u, status: 'active', suspendedUntil: undefined } : u));
    } else {
      setGroups(prev => prev.map(g => g.id === id ? { ...g, status: 'active', suspendedUntil: undefined } : g));
    }
    toast.success(`${name}의 정지가 해제되었습니다`);
  };

  const stats = {
    totalReports: reports.length,
    pending: reports.filter(r => r.status === 'pending').length,
    totalUsers: users.length,
    suspendedUsers: users.filter(u => u.status === 'suspended').length,
    bannedUsers: users.filter(u => u.status === 'banned').length,
    totalGroups: groups.length,
    suspendedGroups: groups.filter(g => g.status === 'suspended').length,
    bannedGroups: groups.filter(g => g.status === 'banned').length,
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-gradient-to-r from-red-600 to-red-700 text-white">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={() => navigate('/')} className="text-white hover:bg-white/20">
              <Home className="w-6 h-6" />
            </Button>
            <div className="flex items-center gap-2">
              <Shield className="w-6 h-6" />
              <div>
                <h1 className="text-lg font-bold">시스템 관리자</h1>
                <p className="text-xs text-red-100">admin@moim.com</p>
              </div>
            </div>
          </div>
          <Badge className="bg-white/20 text-white border-none">ADMIN</Badge>
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Stats */}
        <div className="grid grid-cols-2 gap-3">
          <Card className="border-amber-200 bg-amber-50">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-amber-100 rounded-full flex items-center justify-center">
                  <Flag className="w-5 h-5 text-amber-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-amber-700">{stats.pending}</p>
                  <p className="text-xs text-amber-600">대기 중 신고</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="border-red-200 bg-red-50">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
                  <Ban className="w-5 h-5 text-red-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-red-700">{stats.bannedUsers + stats.bannedGroups}</p>
                  <p className="text-xs text-red-600">영구정지</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-4 gap-2">
          <div className="bg-white rounded-xl p-3 text-center border border-stone-100">
            <p className="text-lg font-bold text-stone-900">{stats.totalUsers}</p>
            <p className="text-xs text-stone-500">전체 사용자</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center border border-stone-100">
            <p className="text-lg font-bold text-amber-600">{stats.suspendedUsers}</p>
            <p className="text-xs text-stone-500">정지 사용자</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center border border-stone-100">
            <p className="text-lg font-bold text-stone-900">{stats.totalGroups}</p>
            <p className="text-xs text-stone-500">전체 모임</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center border border-stone-100">
            <p className="text-lg font-bold text-amber-600">{stats.suspendedGroups}</p>
            <p className="text-xs text-stone-500">정지 모임</p>
          </div>
        </div>

        {/* Tabs */}
        <Tabs defaultValue="reports" className="space-y-4">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="reports" className="text-xs">
              신고 관리
              {stats.pending > 0 && (
                <Badge className="ml-1 bg-red-500 text-white text-[10px] px-1">{stats.pending}</Badge>
              )}
            </TabsTrigger>
            <TabsTrigger value="users" className="text-xs">사용자</TabsTrigger>
            <TabsTrigger value="groups" className="text-xs">모임</TabsTrigger>
          </TabsList>

          {/* Reports Tab */}
          <TabsContent value="reports" className="space-y-4">
            {/* Search & Filter */}
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
                <Input
                  placeholder="검색..."
                  className="pl-10 h-11 bg-white"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
              <Select value={filterStatus} onValueChange={setFilterStatus}>
                <SelectTrigger className="w-28 h-11">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체</SelectItem>
                  <SelectItem value="pending">대기 중</SelectItem>
                  <SelectItem value="reviewing">검토 중</SelectItem>
                  <SelectItem value="resolved">처리 완료</SelectItem>
                  <SelectItem value="dismissed">기각</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Reports List */}
            <div className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
              {filteredReports.length === 0 ? (
                <div className="p-8 text-center text-stone-500">
                  <Flag className="w-12 h-12 mx-auto mb-3 text-stone-300" />
                  <p>신고 내역이 없습니다</p>
                </div>
              ) : (
                <div className="divide-y divide-stone-100">
                  {filteredReports.map(report => (
                    <div 
                      key={report.id} 
                      className="p-4 hover:bg-stone-50 cursor-pointer"
                      onClick={() => setSelectedReport(report)}
                    >
                      <div className="flex items-start gap-3">
                        <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                          report.type === 'user' ? 'bg-purple-100' : 
                          report.type === 'group' ? 'bg-blue-100' : 'bg-orange-100'
                        }`}>
                          {report.type === 'user' ? <User className="w-5 h-5 text-purple-600" /> :
                           report.type === 'group' ? <Users className="w-5 h-5 text-blue-600" /> :
                           <Flag className="w-5 h-5 text-orange-600" />}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <Badge variant="secondary" className="text-xs">
                              {typeLabels[report.type]}
                            </Badge>
                            <Badge className={`text-xs ${statusColors[report.status]}`}>
                              {statusLabels[report.status]}
                            </Badge>
                          </div>
                          <p className="font-medium text-stone-900 truncate">{report.targetName}</p>
                          <p className="text-sm text-stone-500">{report.reason}</p>
                          <p className="text-xs text-stone-400 mt-1">
                            신고자: {report.reporterName} · {report.createdAt}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </TabsContent>

          {/* Users Tab */}
          <TabsContent value="users" className="space-y-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
              <Input
                placeholder="사용자 검색..."
                className="pl-10 h-11 bg-white"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            <div className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
              <div className="divide-y divide-stone-100">
                {filteredUsers.map(user => (
                  <div key={user.id} className="p-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <Avatar className="w-10 h-10">
                          <AvatarImage src={user.avatar} />
                          <AvatarFallback>{user.name[0]}</AvatarFallback>
                        </Avatar>
                        <div>
                          <div className="flex items-center gap-2">
                            <p className="font-medium text-stone-900">{user.name}</p>
                            <Badge className={`text-xs ${statusColors[user.status]}`}>
                              {statusLabels[user.status]}
                            </Badge>
                          </div>
                          <p className="text-xs text-stone-500">{user.email}</p>
                          <div className="flex items-center gap-3 mt-1 text-xs text-stone-400">
                            <span>모임 {user.groupCount}개</span>
                            {user.reportCount > 0 && (
                              <span className="text-red-500">신고 {user.reportCount}회</span>
                            )}
                          </div>
                        </div>
                      </div>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon" className="h-8 w-8">
                            <MoreVertical className="w-4 h-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          {user.status === 'active' && (
                            <DropdownMenuItem 
                              className="text-amber-600"
                              onClick={() => {
                                setSuspendTarget({ type: 'user', id: user.id, name: user.name });
                                setShowSuspendDialog(true);
                              }}
                            >
                              <PauseCircle className="w-4 h-4 mr-2" />
                              정지하기
                            </DropdownMenuItem>
                          )}
                          {(user.status === 'suspended' || user.status === 'banned') && (
                            <DropdownMenuItem 
                              className="text-green-600"
                              onClick={() => handleActivate('user', user.id, user.name)}
                            >
                              <PlayCircle className="w-4 h-4 mr-2" />
                              정지 해제
                            </DropdownMenuItem>
                          )}
                          <DropdownMenuSeparator />
                          <DropdownMenuItem 
                            className="text-red-600"
                            onClick={() => {
                              setDeleteTarget({ type: 'user', id: user.id, name: user.name });
                              setShowDeleteDialog(true);
                            }}
                          >
                            <Trash2 className="w-4 h-4 mr-2" />
                            계정 삭제
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </TabsContent>

          {/* Groups Tab */}
          <TabsContent value="groups" className="space-y-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
              <Input
                placeholder="모임 검색..."
                className="pl-10 h-11 bg-white"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            <div className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
              <div className="divide-y divide-stone-100">
                {filteredGroups.map(group => (
                  <div key={group.id} className="p-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-xl overflow-hidden bg-stone-200">
                          <img src={group.image} alt="" className="w-full h-full object-cover" />
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <p className="font-medium text-stone-900">{group.name}</p>
                            <Badge className={`text-xs ${statusColors[group.status]}`}>
                              {statusLabels[group.status]}
                            </Badge>
                          </div>
                          <p className="text-xs text-stone-500">모임장: {group.ownerName}</p>
                          <div className="flex items-center gap-3 mt-1 text-xs text-stone-400">
                            <span>멤버 {group.memberCount}명</span>
                            {group.reportCount > 0 && (
                              <span className="text-red-500">신고 {group.reportCount}회</span>
                            )}
                          </div>
                        </div>
                      </div>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon" className="h-8 w-8">
                            <MoreVertical className="w-4 h-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          {group.status === 'active' && (
                            <DropdownMenuItem 
                              className="text-amber-600"
                              onClick={() => {
                                setSuspendTarget({ type: 'group', id: group.id, name: group.name });
                                setShowSuspendDialog(true);
                              }}
                            >
                              <PauseCircle className="w-4 h-4 mr-2" />
                              정지하기
                            </DropdownMenuItem>
                          )}
                          {(group.status === 'suspended' || group.status === 'banned') && (
                            <DropdownMenuItem 
                              className="text-green-600"
                              onClick={() => handleActivate('group', group.id, group.name)}
                            >
                              <PlayCircle className="w-4 h-4 mr-2" />
                              정지 해제
                            </DropdownMenuItem>
                          )}
                          <DropdownMenuSeparator />
                          <DropdownMenuItem 
                            className="text-red-600"
                            onClick={() => {
                              setDeleteTarget({ type: 'group', id: group.id, name: group.name });
                              setShowDeleteDialog(true);
                            }}
                          >
                            <Trash2 className="w-4 h-4 mr-2" />
                            모임 삭제
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </div>

      {/* Report Detail Dialog */}
      <AlertDialog open={!!selectedReport} onOpenChange={() => setSelectedReport(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>신고 상세</AlertDialogTitle>
            <AlertDialogDescription asChild>
              {selectedReport && (
                <div className="space-y-4">
                  <div className="bg-stone-50 rounded-lg p-4 space-y-2">
                    <div className="flex justify-between">
                      <span className="text-stone-500">유형</span>
                      <span className="font-medium">{typeLabels[selectedReport.type]}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-stone-500">대상</span>
                      <span className="font-medium">{selectedReport.targetName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-stone-500">신고자</span>
                      <span className="font-medium">{selectedReport.reporterName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-stone-500">사유</span>
                      <span className="font-medium">{selectedReport.reason}</span>
                    </div>
                    {selectedReport.details && (
                      <div>
                        <span className="text-stone-500 text-sm">상세 내용</span>
                        <p className="text-stone-700 mt-1">{selectedReport.details}</p>
                      </div>
                    )}
                  </div>

                  <div className="flex gap-2">
                    <Button 
                      className="flex-1 bg-green-500 hover:bg-green-600"
                      onClick={() => handleUpdateStatus(selectedReport.id, 'resolved')}
                    >
                      <CheckCircle className="w-4 h-4 mr-2" />
                      처리 완료
                    </Button>
                    <Button 
                      variant="outline"
                      className="flex-1"
                      onClick={() => handleUpdateStatus(selectedReport.id, 'dismissed')}
                    >
                      <XCircle className="w-4 h-4 mr-2" />
                      기각
                    </Button>
                  </div>

                  {selectedReport.type !== 'post' && (
                    <Button 
                      variant="destructive"
                      className="w-full"
                      onClick={() => {
                        setSuspendTarget({
                          type: selectedReport.type as 'user' | 'group',
                          id: selectedReport.targetId,
                          name: selectedReport.targetName,
                        });
                        setSelectedReport(null);
                        setShowSuspendDialog(true);
                      }}
                    >
                      <Ban className="w-4 h-4 mr-2" />
                      {selectedReport.type === 'user' ? '사용자 제재' : '모임 제재'}
                    </Button>
                  )}
                </div>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>닫기</AlertDialogCancel>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Suspend Dialog */}
      <Dialog open={showSuspendDialog} onOpenChange={setShowSuspendDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <PauseCircle className="w-5 h-5 text-amber-500" />
              {suspendTarget?.type === 'user' ? '사용자' : '모임'} 정지
            </DialogTitle>
            <DialogDescription>
              {suspendTarget && `"${suspendTarget.name}"을(를) 정지합니다.`}
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
                    <Label htmlFor="permanent" className="cursor-pointer text-red-600">영구정지</Label>
                  </div>
                </div>
              </RadioGroup>
            </div>
            <div className="space-y-2">
              <Label>정지 사유</Label>
              <Textarea
                placeholder="정지 사유를 입력하세요"
                value={suspendReason}
                onChange={(e) => setSuspendReason(e.target.value)}
                className="min-h-[80px]"
              />
            </div>
            {suspendDuration === 'permanent' && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                <p className="text-xs text-red-700">
                  ⚠️ 영구정지는 해당 {suspendTarget?.type === 'user' ? '사용자' : '모임'}의 모든 활동을 완전히 차단합니다.
                </p>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowSuspendDialog(false)}>
              취소
            </Button>
            <Button 
              onClick={handleSuspend}
              className={suspendDuration === 'permanent' ? 'bg-red-500 hover:bg-red-600' : 'bg-amber-500 hover:bg-amber-600'}
            >
              {suspendDuration === 'permanent' ? '영구정지' : '정지하기'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Dialog */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="flex items-center gap-2 text-red-600">
              <Trash2 className="w-5 h-5" />
              {deleteTarget?.type === 'user' ? '계정' : '모임'} 삭제
            </AlertDialogTitle>
            <AlertDialogDescription>
              {deleteTarget && (
                <div className="space-y-3">
                  <p>
                    <span className="font-medium text-stone-900">"{deleteTarget.name}"</span>을(를) 
                    완전히 삭제하시겠습니까?
                  </p>
                  <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                    <p className="text-xs text-red-700">
                      ⚠️ 이 작업은 되돌릴 수 없습니다. 모든 관련 데이터가 영구적으로 삭제됩니다.
                    </p>
                  </div>
                </div>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-red-500 hover:bg-red-600">
              삭제하기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
